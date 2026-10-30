import math

import pytest

from tool import stats


def test_wilson_matches_the_published_interval():
    # Newcombe (1998), table I: 81 of 263 gives 0.2553 to 0.3662.
    low, high = stats.wilson(81, 263)

    assert round(low, 4) == 0.2553
    assert round(high, 4) == 0.3662


def test_mcnemar_is_the_exact_binomial_on_the_discordant_pairs():
    reference = [True] * 10 + [False] * 2 + [True] * 5
    other = [False] * 10 + [True] * 2 + [True] * 5

    result = stats.mcnemar(reference, other)

    assert (result["b"], result["c"]) == (10, 2)
    assert round(result["p"], 4) == 0.0386


def test_mcnemar_without_discordant_pairs_is_one():
    assert stats.mcnemar([True, False], [True, False])["p"] == 1.0


def test_holm_multiplies_by_rank_and_keeps_order():
    adjusted = stats.holm({"a": 0.01, "b": 0.04, "c": 0.03})

    assert adjusted == {"a": 0.03, "c": 0.06, "b": 0.06}


def test_tost_finds_equivalence_only_inside_the_margin():
    close = [0.01, -0.02, 0.0, 0.01, -0.01, 0.02, 0.0, -0.01] * 4
    far = [0.3, 0.25, 0.35, 0.3, 0.28, 0.32, 0.3, 0.31] * 4

    assert stats.tost(close, 0.10) < 0.05
    assert stats.tost(far, 0.10) > 0.05


def test_tost_tokens_works_on_the_log_ratio():
    reference = [1000.0] * 20
    same = [1000.0 * (1 + d) for d in [0.01, -0.01] * 10]
    double = [2000.0] * 20

    assert stats.tost_tokens(reference, same, 0.15) < 0.05
    assert stats.tost_tokens(reference, double, 0.15) == 1.0


def test_verdict_reads_superiority_then_equivalence():
    assert stats.verdict(0.01, None, -0.3) == "worse"
    assert stats.verdict(0.01, None, 0.3) == "better"
    assert stats.verdict(0.4, 0.01, 0.0) == "equivalent"
    assert stats.verdict(0.4, 0.4, 0.05) == "inconclusive"


def test_kappa_matches_a_hand_computed_value():
    first = [True, True, False, False, True, False, True, True, False, False]
    second = [True, False, False, False, True, False, True, True, True, False]

    assert stats.kappa(first, second) == pytest.approx(0.6, abs=1e-9)


def test_mixed_logistic_finds_a_worse_arm():
    rows = []
    for case in range(12):
        for run in range(3):
            rows.append({"passed": True if (case + run) % 6 else False, "arm": "ref", "case": f"c{case}"})
            rows.append({"passed": (case + run) % 3 == 0, "arm": "control", "case": f"c{case}"})

    fit = stats.mixed_logistic(rows, "ref")

    coefficient = next(v for k, v in fit.items() if "control" in k)
    assert coefficient["mean"] < 0
    assert math.isfinite(coefficient["sd"])
