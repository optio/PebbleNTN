"""Unit tests for the route-capture harness's pure parts (#55). Run: python3 -m unittest discover tools/route-capture/tests"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from routecap import geo, host, issues, notifications, report, ui  # noqa: E402

DUMPSYS = """
  NotificationRecord(0x02078ad1: pkg=com.google.android.apps.maps user=UserHandle{0} id=1 tag=null importance=3 key=0|com.google.android.apps.maps|1|null|10172: Notification(channel=1_foreground_1 shortcut=null contentView=null vibrate=null sound=null defaults=0 flags=ONGOING_EVENT category=navigation groupKey=g actions=1 vis=PUBLIC))
      extras={
        android.title=SpannableString (70 m)
        android.subText=String (10 min · 3.2 km · 12:36 AM ETA)
        android.text=SpannableString (Coudenberg / Koudenberg)
        android.template=String (android.app.Notification$BigTextStyle)
        android.progress=Integer (0)
      }
  NotificationRecord(0x0aaa: pkg=com.android.systemui user=UserHandle{0} id=2 Notification(channel=other category=sys))
        android.title=String (USB debugging connected)
"""

UI_XML = ('<hierarchy><node index="0" text="Make it your map" bounds="[0,0][1080,200]" />'
          '<node index="1" text="Skip" content-desc="" bounds="[800,100][960,172]" />'
          '<node index="2" text="" content-desc="Got it" bounds="[700,1700][1000,1800]" /></hierarchy>')


class GeoTest(unittest.TestCase):
    def test_walk_steps_at_the_speed_and_keeps_both_ends(self):
        line = [(4.35, 50.84), (4.36, 50.84)]  # about 704 m east
        pts = geo.walk(line, 100)
        self.assertEqual(pts[0], line[0])
        self.assertEqual(pts[-1], line[-1])
        steps = [geo.distance_m(a, b) for a, b in zip(pts, pts[1:-1])]
        self.assertTrue(all(abs(s - 100) < 1 for s in steps), steps)

    def test_walk_carries_distance_across_vertices(self):
        line = [(4.35, 50.84), (4.3507, 50.84), (4.3514, 50.84)]  # two ~49 m segments
        pts = geo.walk(line, 30)
        self.assertAlmostEqual(geo.distance_m(pts[1], pts[2]), 30, delta=1)

    def test_walk_rejects_a_non_positive_step(self):
        with self.assertRaises(ValueError):
            geo.walk([(0, 0), (1, 1)], 0)


class NotificationsTest(unittest.TestCase):
    def test_parses_text_extras_template_and_channel_for_one_package(self):
        recs = notifications.parse_dumpsys(DUMPSYS, "com.google.android.apps.maps")
        self.assertEqual(1, len(recs))
        r = recs[0]
        self.assertEqual(("70 m", "Coudenberg / Koudenberg", "10 min · 3.2 km · 12:36 AM ETA", None), notifications.key(r))
        self.assertEqual("BigTextStyle", r["template"])
        self.assertEqual("1_foreground_1", r["channelId"])
        self.assertEqual("navigation", r["category"])

    def test_without_a_package_every_record_is_returned(self):
        self.assertEqual(2, len(notifications.parse_dumpsys(DUMPSYS)))


class UiTest(unittest.TestCase):
    def test_finds_by_text_or_content_desc_in_label_order(self):
        self.assertEqual(("Skip", (880, 136)), ui.find(UI_XML, ["Skip", "Got it"]))
        self.assertEqual(("Got it", (850, 1750)), ui.find(UI_XML, ["Got it"]))
        self.assertIsNone(ui.find(UI_XML, ["Dismiss"]))


class HostTest(unittest.TestCase):
    def test_avd_config_gets_play_store_resources_and_drops_the_temp_data_path(self):
        out = host.configure_avd("PlayStore.enabled=no\nhw.ramSize=1536M\ndisk.dataPartition.path=<temp>\nhw.lcd.density=420\n", 4096, 4, 8)
        self.assertIn("PlayStore.enabled=yes", out)
        self.assertIn("hw.ramSize=4096M", out)
        self.assertIn("disk.dataPartition.size=8G", out)
        self.assertIn("hw.lcd.density=420", out)
        self.assertNotIn("dataPartition.path", out)


class LocaleTest(unittest.TestCase):
    def test_reads_the_system_language_from_am_get_config(self):
        self.assertEqual("pl-PL", host.locale_from_config("config: mcc310-mnc260-pl-rPL-ldltr-sw411dp-w411dp-normal\nabi: x86_64"))
        self.assertEqual("en-US", host.locale_from_config("config: mcc310-mnc260-en-rUS-ldltr-sw411dp"))
        self.assertEqual("", host.locale_from_config("nothing here"))

    def test_scenario_language_matches_on_the_language_part(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location("routecap_cli", Path(__file__).resolve().parents[1] / "routecap.py")
        mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
        self.assertTrue(mod.same_language("pl-PL", "pl-PL"))
        self.assertTrue(mod.same_language("pt-BR", "pt-PT"))
        self.assertFalse(mod.same_language("en-US", "pl-PL"))


class FailedScenarioTest(unittest.TestCase):
    def test_failed_scenarios_are_left_out_of_the_report_and_the_issue(self):
        import importlib.util, tempfile
        spec = importlib.util.spec_from_file_location("routecap_cli", Path(__file__).resolve().parents[1] / "routecap.py")
        mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
        ok = {"id": "ok", "app": "google-maps", "locale": "en-US", "mode": "car", "route": "r", "appVersion": "1",
              "captures": [{"packageName": "com.google.android.apps.maps", "title": "300 m · Turn left onto X", "subText": "Arrive 20:26"}]}
        bad = {"id": "bad", "app": "google-maps", "locale": "pt-BR", "mode": "car", "route": "r", "failed": "no notifications", "captures": []}
        run = {"startedAt": "t", "android": "17", "image": "i", "scenarios": [ok, bad]}
        with tempfile.TemporaryDirectory() as d:
            mod.write_report(run, Path(d), publish=False)
            text = (Path(d) / "report.md").read_text()
            summary = (Path(d) / "summary-google-maps.json").read_text()
        self.assertIn("Failed scenarios (not reported): `bad`", text)
        self.assertNotIn('"bad"', summary)


class ReportTest(unittest.TestCase):
    def run_with(self, captures, sid="s1", locale="en-US", started="2026-10-04 12:00 UTC"):
        return {"startedAt": started, "android": "17", "image": "test",
                "scenarios": [{"id": sid, "app": "google-maps", "locale": locale, "mode": "car", "route": "r",
                               "appVersion": "26.14", "captures": captures}]}

    CLASSIC = {"packageName": "com.google.android.apps.maps", "title": "70 m", "text": "Coudenberg",
               "subText": "10 min · 3.2 km · 12:36 AM ETA", "template": "BigTextStyle"}

    def test_counts_recognised_and_groups_unrecognised_shapes(self):
        classic2 = dict(self.CLASSIC, title="60 m", text="Rue Ducale")
        turn = {"packageName": "com.google.android.apps.maps", "title": "300 m · Turn left onto Bd Ney", "subText": "Arrive 20:26"}
        state = report.evaluate(self.run_with([self.CLASSIC, classic2, turn]))
        self.assertEqual((3, 1), (state["scenarios"]["s1"]["total"], state["scenarios"]["s1"]["matched"]))
        shapes = report.unrecognised(state)
        self.assertEqual(1, len(shapes), "same layout, different numbers and road: one shape")
        self.assertEqual(2, shapes[0]["count"])
        self.assertIn("«N m»", shapes[0]["shape"])

    def test_road_names_are_abstracted_so_steps_group_by_instruction(self):
        self.assertEqual("Vire à esquerda na …", report.instruction("Vire à esquerda na Rue de l'Hôpital/Gasthuisstraat"))
        self.assertEqual("Turn left onto …", report.instruction("Turn left onto Bd Ney"))
        self.assertEqual("Head north", report.instruction("Head north"))
        self.assertEqual(report.shape({"title": "180 m · Vire à esquerda na Rue A"}),
                         report.shape({"title": "90 m · Vire à esquerda na Pl. Saint-Jean"}))

    def test_cards_that_are_not_directions_are_not_counted_as_missing(self):
        starting = {"packageName": "com.google.android.apps.maps", "title": "Starting navigation…"}
        rerouting = {"packageName": "com.google.android.apps.maps", "title": "Rerouting...", "subText": "Arrive "}
        state = report.evaluate(self.run_with([starting, rerouting]))
        self.assertEqual([], report.unrecognised(state))
        self.assertEqual(2, len(state["notDirections"]["s1"]))

    def test_fallback_matches_are_listed_for_review(self):
        approach = {"packageName": "com.google.android.apps.maps", "title": "90 m · tipi! Cinquantenaire", "subText": "Arrive 3:37 AM"}
        state = report.evaluate(self.run_with([approach]))
        self.assertEqual(1, state["scenarios"]["s1"]["matched"])
        self.assertEqual("google-maps-destination-approach-en", state["fallback"]["s1"][0]["rule"])
        self.assertIn("Recognised only by a fallback rule", report.markdown("Google Maps", state))

    def test_maneuver_words_come_from_the_app(self):
        self.assertIn("turn", report.MANEUVER_WORDS)
        self.assertIn("rechtsaf", report.MANEUVER_WORDS)

    def test_merging_keeps_other_scenarios_and_replaces_the_rerun_one(self):
        polish = report.evaluate(self.run_with([dict(self.CLASSIC)], sid="pl", locale="pl-PL", started="run 1"))
        english = report.evaluate(self.run_with([dict(self.CLASSIC)], sid="en", started="run 1"))
        merged = report.merge(report.merge(None, polish), english)
        self.assertEqual({"pl", "en"}, set(merged["scenarios"]))
        fixed = report.evaluate(self.run_with([], sid="en", started="run 2"))
        merged = report.merge(merged, fixed)
        self.assertEqual("run 2", merged["scenarios"]["en"]["run"])
        self.assertEqual(["pl"], report.unrecognised(merged)[0]["scenarios"], "Polish findings survive an English-only run")

    def test_markdown_lists_scenarios_and_shapes(self):
        state = report.evaluate(self.run_with([dict(self.CLASSIC)]))
        md = report.markdown("Google Maps", state)
        self.assertIn("| `s1` | en-US | car | r | 26.14 | 2026-10-04 12:00 UTC (Android 17) | 1 | 0 |", md)
        self.assertIn("1×", md)


class IssuesTest(unittest.TestCase):
    def test_decides_create_update_or_clear(self):
        self.assertEqual("create", issues.decide(None, 3))
        self.assertEqual("skip", issues.decide(None, 0))
        self.assertEqual("update", issues.decide({"number": 1}, 2))
        self.assertEqual("update-clear", issues.decide({"number": 1}, 0))

    def test_one_marker_and_title_per_app(self):
        self.assertNotEqual(issues.marker("google-maps"), issues.marker("osmand"))
        self.assertEqual("Route capture: unrecognised Google Maps notifications", issues.title("Google Maps"))

    def test_state_survives_the_issue_body(self):
        state = {"scenarios": {"x": {"id": "x", "note": "Vire à esquerda -- ok"}}, "shapes": {"x": []}, "notDirections": {}}
        body = "Report …\n\n" + issues.marker("google-maps") + "\n" + issues.encode_state(state)
        self.assertEqual(state, issues.decode_state(body))
        self.assertNotIn("--", issues.encode_state(state)[len(issues.STATE_PREFIX):-4])
        self.assertIsNone(issues.decode_state("no state here"))

    def test_dry_run_publishes_nothing(self):
        state = report.evaluate(ReportTest().run_with([dict(ReportTest.CLASSIC)]))
        self.assertEqual("create (dry run)", issues.publish("google-maps", "Google Maps", state, "t", dry_run=True))


if __name__ == "__main__":
    unittest.main()
