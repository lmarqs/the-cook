"""The statistics #90 names: Wilson intervals, McNemar, Wilcoxon, Holm, TOST, and a mixed-effects logistic model."""

import math

import numpy as np
from scipy import stats


def wilson(successes: int, n: int, z: float = 1.959964) -> tuple[float, float]:
    if n == 0:
        return (0.0, 1.0)
    p = successes / n
    centre = (p + z * z / (2 * n)) / (1 + z * z / n)
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n)
    return (max(0.0, centre - half), min(1.0, centre + half))


def mcnemar(reference: list[bool], other: list[bool]) -> dict:
    """Exact McNemar on paired outcomes: b, reference passes and other fails; c, the reverse."""
    b = sum(r and not o for r, o in zip(reference, other))
    c = sum(o and not r for r, o in zip(reference, other))
    p = 1.0 if b + c == 0 else stats.binomtest(b, b + c, 0.5).pvalue
    return {"b": b, "c": c, "p": p}


def wilcoxon(reference: list[float], other: list[float]) -> float | None:
    differences = [o - r for r, o in zip(reference, other) if o != r]
    if len(differences) < 2:
        return None
    return float(stats.wilcoxon(differences).pvalue)


def holm(pvalues: dict[str, float]) -> dict[str, float]:
    ordered = sorted((p, name) for name, p in pvalues.items() if p is not None)
    adjusted, running = {}, 0.0
    for rank, (p, name) in enumerate(ordered):
        running = max(running, min(1.0, (len(ordered) - rank) * p))
        adjusted[name] = running
    return adjusted


def tost(differences: list[float], margin: float) -> float | None:
    """Two one-sided paired t-tests that the mean difference lies within ±margin; returns the larger p."""
    values = np.asarray(differences, dtype=float)
    if len(values) < 2:
        return None
    if np.allclose(values, values[0]):
        return 0.0 if abs(values[0]) < margin else 1.0
    lower = stats.ttest_1samp(values, -margin, alternative="greater").pvalue
    upper = stats.ttest_1samp(values, margin, alternative="less").pvalue
    return float(max(lower, upper))


def tost_success(reference: list[bool], other: list[bool], margin: float) -> float | None:
    return tost([float(o) - float(r) for r, o in zip(reference, other)], margin)


def tost_tokens(reference: list[float], other: list[float], margin: float) -> float | None:
    """Equivalence of tokens on the paired log ratio, within ±log(1 + margin)."""
    pairs = [(r, o) for r, o in zip(reference, other) if r > 0 and o > 0]
    return tost([math.log(o / r) for r, o in pairs], math.log(1 + margin))


def mixed_logistic(rows: list[dict], reference: str) -> dict:
    """pass ~ arm with a random intercept per case, by statsmodels' variational Bayes; posterior means and sds."""
    import pandas as pd
    from statsmodels.genmod.bayes_mixed_glm import BinomialBayesMixedGLM

    frame = pd.DataFrame(rows)
    if frame["passed"].nunique() < 2 or frame["arm"].nunique() < 2 or frame["case"].nunique() < 2:
        return {"skipped": "needs both outcomes, two arms and two cases"}
    frame["passed"] = frame["passed"].astype(int)
    formula = f"passed ~ C(arm, Treatment('{reference}'))"
    model = BinomialBayesMixedGLM.from_formula(formula, {"case": "0 + C(case)"}, frame)
    fit = model.fit_vb()
    names = model.exog_names
    return {name: {"mean": float(fit.fe_mean[i]), "sd": float(fit.fe_sd[i])} for i, name in enumerate(names)}


def verdict(p_superiority: float | None, p_equivalence: float | None, difference: float, alpha: float = 0.05) -> str:
    """The other arm against the reference: better, worse, equivalent or inconclusive."""
    if p_superiority is not None and p_superiority < alpha and difference != 0:
        return "better" if difference > 0 else "worse"
    if p_equivalence is not None and p_equivalence < alpha:
        return "equivalent"
    return "inconclusive"


def kappa(first: list[bool], second: list[bool]) -> float | None:
    n = len(first)
    if n == 0:
        return None
    observed = sum(a == b for a, b in zip(first, second)) / n
    p1, p2 = sum(first) / n, sum(second) / n
    expected = p1 * p2 + (1 - p1) * (1 - p2)
    return 1.0 if expected == 1 else (observed - expected) / (1 - expected)
