#!/usr/bin/env python3
"""Unit tests for scripts/validate_catalog.py: rule families (#113) and the manifest <queries> check."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import validate_catalog  # noqa: E402


def app(app_id: str, packages: list[str], has_rules: bool = True, **extra) -> dict:
    entry = {
        "appId": app_id,
        "displayName": app_id,
        "packageNames": packages,
        "hasOfficialRules": has_rules,
        "captureAvailable": True,
        "defaultEnabled": True,
    }
    entry.update(extra)
    return entry


ROOT = app("google-maps", ["com.google.android.apps.maps"])
VARIANT = app("microg-maps", ["app.morphe.android.apps.maps"], rulesFrom="google-maps")


class RuleFamilyErrorsTest(unittest.TestCase):
    def test_variant_of_an_app_with_rules_is_valid(self):
        self.assertEqual([], validate_catalog.rule_family_errors([ROOT, VARIANT], {"google-maps"}))

    def test_unknown_target_is_rejected(self):
        errors = validate_catalog.rule_family_errors([app("x", ["p.x"], rulesFrom="nowhere")], set())
        self.assertIn("unknown app nowhere", errors[0])

    def test_self_reference_is_rejected(self):
        errors = validate_catalog.rule_family_errors([app("x", ["p.x"], rulesFrom="x")], set())
        self.assertIn("names itself", errors[0])

    def test_chain_is_rejected(self):
        chained = app("third", ["p.third"], rulesFrom="microg-maps")
        errors = validate_catalog.rule_family_errors([ROOT, VARIANT, chained], {"google-maps"})
        self.assertEqual(1, len(errors))
        self.assertIn("no chains", errors[0])

    def test_target_without_rules_is_rejected(self):
        capture_only = app("waze", ["com.waze"], has_rules=False)
        errors = validate_catalog.rule_family_errors([capture_only, app("x", ["p.x"], rulesFrom="waze")], set())
        self.assertIn("no rules of its own", errors[0])

    def test_rules_folder_for_a_variant_is_rejected(self):
        errors = validate_catalog.rule_family_errors([ROOT, VARIANT], {"google-maps", "microg-maps"})
        self.assertIn("rules/bundled/microg-maps/ must not exist", errors[0])

    def test_duplicate_package_is_rejected(self):
        twin = app("twin", ["com.google.android.apps.maps"], rulesFrom="google-maps")
        errors = validate_catalog.rule_family_errors([ROOT, twin], {"google-maps"})
        self.assertIn("claimed by both", errors[0])


class ManifestQueriesTest(unittest.TestCase):
    def setUp(self):
        self.apps = json.loads(validate_catalog.CATALOG_PATH.read_text(encoding="utf-8"))["apps"]
        self.manifest = validate_catalog.MANIFEST_PATH.read_text(encoding="utf-8")

    def test_bundled_manifest_covers_all_28_navigation_packages(self):
        # 28 since app.vela (Vela Maps, #120) joined; 27 since org.ungoogled.android.apps.maps.microg
        # joined MicroG Maps (#113).
        self.assertEqual(28, len(validate_catalog.queried_catalog_packages(self.apps)))
        self.assertEqual([], validate_catalog.missing_manifest_packages(self.apps, self.manifest))

    def test_missing_microg_package_is_reported(self):
        without = self.manifest.replace('<package android:name="app.morphe.android.apps.maps" />', "")
        self.assertEqual(
            ["app.morphe.android.apps.maps"],
            validate_catalog.missing_manifest_packages(self.apps, without),
        )

    def test_bundled_catalog_rule_families_are_consistent(self):
        bundled = {p.name for p in validate_catalog.BUNDLED_DIR.iterdir() if p.is_dir()}
        self.assertEqual([], validate_catalog.rule_family_errors(self.apps, bundled))


if __name__ == "__main__":
    unittest.main()
