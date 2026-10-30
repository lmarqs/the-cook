"""Writes a study's report.md from its graded runs: validity flags first, then arms, contrasts and cases."""

import json
import statistics
from collections import defaultdict
from pathlib import Path

from tool import stats
from tool.cases import Study
from tool.harness import refused
from tool.traces import first_index

HEADROOM = (0.10, 0.90)
KAPPA = 0.61


def rows(batch_dirs: list[Path]) -> list[dict]:
    found = []
    for batch_dir in batch_dirs:
        for directory in sorted((batch_dir / "runs").iterdir()):
            if not (directory / "grading.json").exists():
                continue
            record = json.loads((directory / "run.json").read_text())
            grading = json.loads((directory / "grading.json").read_text())
            found.append({**record, "grading": grading, "passed": grading["outcome"]["pass"],
                          "share": grading["outcome"]["share"], "batch": batch_dir.name})
    return found


def effort(row: dict, case_data: dict) -> tuple[int | None, bool]:
    """Steps before the owning page is first read, and whether it never was: such a run counts all its steps."""
    page = case_data.get("owning_page")
    if not page:
        return None, False
    index = first_index(row["steps"], "read", page)
    return (len(row["steps"]), True) if index is None else (index, False)


def median_mean(values: list[float]) -> str:
    values = [v for v in values if v is not None]
    if not values:
        return "–"
    return f"{statistics.median(values):,.0f} / {statistics.mean(values):,.0f}"


def paired(rows_: list[dict], reference: str, arm: str, harness: str) -> list[tuple[dict, dict]]:
    index = {(r["batch"], r["case"], r["run"]): r for r in rows_ if r["arm"] == reference and r["harness"] == harness}
    return [(index[(r["batch"], r["case"], r["run"])], r) for r in rows_
            if r["arm"] == arm and r["harness"] == harness and (r["batch"], r["case"], r["run"]) in index]


def write(study: Study, batch_dirs: list[Path], labels: Path | None = None) -> Path:
    data = rows(batch_dirs)
    for r in data:
        r["effort"], r["censored"] = effort(r, study.case(r["case"]).data)
    reference, margins = study.reference, study.margins
    harnesses = sorted({r["harness"] for r in data})
    lines = [f"# Study {study.id}: report", "",
             f"Batches: {', '.join(d.name for d in batch_dirs)}. Runs graded: {len(data)}. "
             f"Reference arm: `{reference}`. Outcome: all of {study.outcome} pass. Primary: "
             + ("the outcome." if study.primary == "pass" else "effort, the steps before the owning page is read."),
             ""]

    flags = []
    contrasts, pvalues = [], {}
    for harness in harnesses:
        for arm in study.arms[1:]:
            pairs = paired(data, reference, arm, harness)
            ref_pass = [a["passed"] for a, _ in pairs]
            other_pass = [b["passed"] for _, b in pairs]
            test = stats.mcnemar(ref_pass, other_pass)
            name = f"{harness}: {arm} vs {reference}"
            difference = (sum(other_pass) - sum(ref_pass)) / len(pairs) if pairs else 0.0
            tokens_ref = [a["tokens"]["input"] + a["tokens"]["output"] for a, _ in pairs]
            tokens_other = [b["tokens"]["input"] + b["tokens"]["output"] for _, b in pairs]
            efforts = [(a["effort"], b["effort"]) for a, b in pairs if a["effort"] is not None and b["effort"] is not None]
            effort_p = stats.wilcoxon([a for a, _ in efforts], [b for _, b in efforts])
            effort_difference = statistics.median(b - a for a, b in efforts) if efforts else None
            pvalues[name] = test["p"] if study.primary == "pass" else effort_p
            contrasts.append({
                "name": name, "arm": arm, "harness": harness, "n": len(pairs), "difference": difference,
                "effort_difference": effort_difference, "wilcoxon_effort": effort_p,
                "mcnemar": test, "wilcoxon_tokens": stats.wilcoxon(tokens_ref, tokens_other),
                "tost_success": stats.tost_success(ref_pass, other_pass, margins["success"]),
                "tost_tokens": stats.tost_tokens(tokens_ref, tokens_other, margins["tokens"]),
            })
    adjusted = stats.holm(pvalues)
    for contrast in contrasts:
        contrast["p_holm"] = adjusted.get(contrast["name"])
        if study.primary == "pass":
            contrast["verdict"] = stats.verdict(contrast["p_holm"], contrast["tost_success"], contrast["difference"])
            worse = contrast["difference"] < 0
        else:
            # Fewer steps is better, so the sign is flipped; effort has no equivalence margin.
            fewer = -(contrast["effort_difference"] or 0)
            contrast["verdict"] = stats.verdict(contrast["p_holm"], None, fewer)
            worse = fewer < 0
        if contrast["arm"] == study.arms[-1] and not (contrast["p_holm"] is not None and contrast["p_holm"] < 0.05
                                                       and worse):
            flags.append(f"The control `{contrast['arm']}` does not separate from `{reference}` on "
                         f"{contrast['harness']} (Holm p = {contrast['p_holm']}): no comparison in this study counts.")

    by_case = defaultdict(list)
    for r in data:
        if r["arm"] == reference:
            by_case[(r["case"], r["harness"])].append(r)
    for (case, harness), runs in sorted(by_case.items()):
        if study.primary == "pass":
            rate = sum(r["passed"] for r in runs) / len(runs)
            if not HEADROOM[0] <= rate <= HEADROOM[1]:
                flags.append(f"Case `{case}` on {harness}: the reference passes {rate:.0%} of {len(runs)} runs, "
                             f"outside {HEADROOM[0]:.0%}–{HEADROOM[1]:.0%}.")
        elif all(r["effort"] == 0 for r in runs):
            flags.append(f"Case `{case}` on {harness}: the reference reads the owning page first in every run, "
                         "so no arm can reach it sooner.")

    by_grader = defaultdict(list)
    for r in data:
        for e in r["grading"]["expectations"]:
            by_grader[e["name"]].append(e["passed"])
    for name, outcomes in sorted(by_grader.items()):
        if len(set(outcomes)) == 1:
            flags.append(f"Grader `{name}` {'passes' if outcomes[0] else 'fails'} every run of every arm: "
                         "it discriminates nothing.")

    leaky = [r["id"] for r in data if r.get("init") and (r["init"].get("mcp_servers")
             or any(p.get("path") != "builtin" for p in r["init"].get("plugins") or []))]
    if leaky:
        flags.append(f"Runs that loaded MCP servers or non-builtin plugins, so saw more than the fixture: {leaky}.")
    turned_away = [r["id"] for r in data if refused(r)]
    if turned_away:
        flags.append(f"Runs the provider refused on every attempt, counted as failed: {turned_away}.")
    missing = sum(e["verdict_missing"] for r in data for e in r["grading"]["expectations"])
    if missing:
        flags.append(f"{missing} judge calls returned no verdict and count as failed.")
    if labels and labels.exists():
        from tool.label import agreement
        k, n = agreement(labels)
        if k is None or k < KAPPA:
            flags.append(f"Judge agreement with the owner's labels: kappa {k} on {n} labels, below {KAPPA}.")
    else:
        flags.append("The judge is not validated: no owner labels yet (`evals label`, then `evals kappa`).")

    lines += ["## Validity", ""] + ([f"- {f}" for f in flags] or ["- No flags."]) + [""]

    lines += ["## Arms", "", "| Harness | Arm | Runs | Pass | 95% Wilson | Share | pass^k | Tokens in, median / mean "
              "| Tokens out, median / mean | Cost USD, mean | Seconds, median | Steps before owning page, median "
              "| Never read the owning page |",
              "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"]
    for harness in harnesses:
        for arm in study.arms:
            subset = [r for r in data if r["harness"] == harness and r["arm"] == arm]
            if not subset:
                continue
            passes = sum(r["passed"] for r in subset)
            low, high = stats.wilson(passes, len(subset))
            per_case = defaultdict(list)
            for r in subset:
                per_case[r["case"]].append(r["passed"])
            pass_k = sum(all(v) for v in per_case.values()) / len(per_case)
            costs = [r["cost_usd"] for r in subset if r.get("cost_usd") is not None]
            before = [r["effort"] for r in subset if r["effort"] is not None]
            never = sum(r["censored"] for r in subset)
            cost = f"{statistics.mean(costs):.3f}" if costs else "–"
            lines.append(
                f"| {harness} | `{arm}` | {len(subset)} | {passes / len(subset):.0%} | {low:.0%}–{high:.0%} "
                f"| {statistics.mean(r['share'] for r in subset):.2f} | {pass_k:.0%} "
                f"| {median_mean([r['tokens']['input'] for r in subset])} "
                f"| {median_mean([r['tokens']['output'] for r in subset])} | {cost} "
                f"| {statistics.median(r['duration_s'] for r in subset):.0f} "
                f"| {statistics.median(before) if before else '–'} | {never if before else '–'} |"
            )
    lines.append("")

    lines += ["## Contrasts", "",
              f"Each arm against `{reference}`, paired by batch, case and run. Superiority: exact McNemar on the "
              f"outcome and Wilcoxon on the steps before the owning page; Holm corrects the primary one, "
              f"{'McNemar' if study.primary == 'pass' else 'Wilcoxon on steps'}, across the study. Equivalence: TOST "
              f"within ±{margins['success']:.0%} of success and ±{margins['tokens']:.0%} of tokens.", "",
              "| Contrast | Pairs | Difference in pass | b / c | McNemar p | Difference in steps, median "
              "| Wilcoxon steps p | Holm p | TOST success p | Wilcoxon tokens p | TOST tokens p | Verdict |",
              "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"]
    for c in contrasts:
        steps = "–" if c["effort_difference"] is None else f"{c['effort_difference']:+g}"
        lines.append(f"| {c['name']} | {c['n']} | {c['difference']:+.0%} | {c['mcnemar']['b']} / {c['mcnemar']['c']} "
                     f"| {fmt(c['mcnemar']['p'])} | {steps} | {fmt(c['wilcoxon_effort'])} | {fmt(c['p_holm'])} "
                     f"| {fmt(c['tost_success'])} | {fmt(c['wilcoxon_tokens'])} | {fmt(c['tost_tokens'])} "
                     f"| {c['verdict']} |")
    lines.append("")

    lines += ["## Mixed-effects logistic model", "",
              "pass ~ arm, random intercept per case; posterior mean and sd of each coefficient, by harness.", ""]
    for harness in harnesses:
        model_rows = [{"passed": r["passed"], "arm": r["arm"], "case": r["case"]} for r in data
                      if r["harness"] == harness]
        try:
            fit = stats.mixed_logistic(model_rows, reference)
        except Exception as error:  # the model is reported as not fitted rather than stopping the report
            fit = {"skipped": f"did not fit: {error}"}
        lines.append(f"- {harness}: " + ("; ".join(f"`{k}` {v['mean']:+.2f} ± {v['sd']:.2f}" for k, v in fit.items())
                                         if "skipped" not in fit else fit["skipped"]))
    lines.append("")

    lines += ["## Graders", "", "| Grader | Arm | Harness | Pass |", "| --- | --- | --- | --- |"]
    for name in sorted(by_grader):
        for harness in harnesses:
            for arm in study.arms:
                outcomes = [e["passed"] for r in data if r["arm"] == arm and r["harness"] == harness
                            for e in r["grading"]["expectations"] if e["name"] == name]
                if outcomes:
                    lines.append(f"| `{name}` | `{arm}` | {harness} | {sum(outcomes)}/{len(outcomes)} |")
    lines.append("")

    lines += ["## Runs", "", "| Run | Outcome | Graders passed | Error |", "| --- | --- | --- | --- |"]
    for r in sorted(data, key=lambda r: r["id"]):
        lines.append(f"| `{r['id']}` | {'pass' if r['passed'] else 'fail'} | {r['grading']['summary']['passed']}/"
                     f"{r['grading']['summary']['total']} | {r.get('error') or ''} |")
    path = batch_dirs[-1] / "report.md"
    path.write_text("\n".join(lines) + "\n")
    return path


def fmt(value: float | None) -> str:
    return "–" if value is None else f"{value:.3f}"
