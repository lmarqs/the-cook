"""Run with: python3 -B -m unittest discover -s cookbook/test/beads -p 'test_*.py'."""

import unittest

from runtime_acceptance import matches_pinned_version


class PinnedVersionTest(unittest.TestCase):
    def test_accepts_complete_pinned_native_version_outputs(self):
        cases = [
            ("bd", "bd version 1.3.1 (Homebrew)\n", "1.3.1"),
            ("bd", "bd version 1.3.1\n", "1.3.1"),
            ("bv", "bv v0.25.2\n", "0.25.2"),
            ("ralph-tui", "ralph-tui 0.12.0\n", "0.12.0"),
        ]
        for tool, output, expected in cases:
            with self.subTest(tool=tool, output=output):
                self.assertTrue(matches_pinned_version(tool, output, expected))

    def test_rejects_versions_that_only_contain_the_pin(self):
        cases = [
            ("bd", "bd version 1.3.10 (Homebrew)", "1.3.1"),
            ("bv", "bv v0.25.20", "0.25.2"),
            ("ralph-tui", "ralph-tui 0.12.00", "0.12.0"),
            ("bd", "bd version 1.3.1-rc.1 (Homebrew)", "1.3.1"),
            ("bv", "bv v0.25.2-beta.1", "0.25.2"),
            ("ralph-tui", "ralph-tui 0.12.0+unverified", "0.12.0"),
            ("bd", "bd version 9.0.0 (built with 1.3.1)", "1.3.1"),
            ("bv", "other-tool v0.25.2", "0.25.2"),
            ("ralph-tui", "dependency 0.12.0\nralph-tui 9.0.0", "0.12.0"),
        ]
        for tool, output, expected in cases:
            with self.subTest(tool=tool, output=output):
                self.assertFalse(matches_pinned_version(tool, output, expected))
