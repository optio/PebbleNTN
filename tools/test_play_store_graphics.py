"""Unit tests for tools/play_store_graphics.py (REQ-ANDROID-022). Needs Pillow, like the script.

Run: <python with Pillow> -m unittest discover -s tools -p 'test_play_store_graphics.py'
"""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import play_store_graphics as g  # noqa: E402


class ScreenshotRulesTest(unittest.TestCase):

    def test_play_size_screenshot_passes(self):
        self.assertEqual([], g.screenshot_problems("1.png", (1080, 2160), "RGB"))

    def test_longer_than_two_to_one_fails(self):
        problems = g.screenshot_problems("1.png", (1080, 2280), "RGB")
        self.assertEqual(1, len(problems))
        self.assertIn("2:1", problems[0])

    def test_too_small_watch_screenshot_fails(self):
        self.assertTrue(g.screenshot_problems("w.png", (144, 168), "RGB"))

    def test_too_large_side_fails(self):
        self.assertTrue(g.screenshot_problems("big.png", (2000, 3900), "RGB"))

    def test_alpha_channel_fails(self):
        problems = g.screenshot_problems("1.png", (1080, 2160), "RGBA")
        self.assertEqual(1, len(problems))
        self.assertIn("alpha", problems[0])

    def test_exact_sizes(self):
        self.assertEqual([], g.exact_size_problems("featureGraphic.png", (1024, 500), g.FEATURE_SIZE))
        self.assertTrue(g.exact_size_problems("featureGraphic.png", (1024, 512), g.FEATURE_SIZE))
        self.assertTrue(g.exact_size_problems("icon.png", (192, 192), g.ICON_SIZE))


class CropRowsTest(unittest.TestCase):

    def test_drops_the_status_bar_and_cuts_through_blank_rows(self):
        # Status bar rows 15-50, content from 148 to 2230, everything else blank.
        content = set(range(15, 51)) | set(range(148, 2231))
        blank = set(range(2280)) - content
        top = g.crop_rows(2280, 2160, blank)
        self.assertEqual(108, top)  # 40 rows kept above the first content
        self.assertGreater(top, 50)

    def test_never_cuts_content_at_the_bottom(self):
        # Content runs to the last row: only a top crop of all 120 rows would do, and it would cut
        # into the content (first content at 128), so there is no clean cut.
        content = set(range(15, 51)) | set(range(128, 2280))
        blank = set(range(2280)) - content
        self.assertIsNone(g.crop_rows(2280, 2160, blank))

    def test_prefers_a_split_whose_bottom_cut_is_blank(self):
        # A text line at the bottom (2250-2265) must not be cut: with content from 128, the top crop
        # is at most 88, so 32 rows come off the bottom (cut at 2248, blank).
        content = set(range(15, 51)) | set(range(128, 2240)) | set(range(2250, 2266))
        blank = set(range(2280)) - content
        top = g.crop_rows(2280, 2160, blank)
        self.assertIsNotNone(top)
        cut = 2280 - (120 - top)
        self.assertTrue(cut <= 2250 or cut > 2266)


class FitPhoneScreenshotTest(unittest.TestCase):

    def test_play_size_is_copied(self):
        source = g.Image.new("RGBA", (1080, 2160), (10, 20, 30, 255))
        out, note = g.fit_phone_screenshot(source)
        self.assertEqual((1080, 2160), out.size)
        self.assertEqual("RGB", out.mode)
        self.assertEqual("copied", note)

    def test_a_device_capture_is_cropped_through_background(self):
        source = g.Image.new("RGB", (1080, 2280), (250, 248, 255))
        draw = g.ImageDraw.Draw(source)
        draw.rectangle((0, 15, 1079, 50), fill=(0, 0, 0))      # status bar
        draw.rectangle((60, 148, 900, 2230), fill=(0, 0, 0))   # content
        out, note = g.fit_phone_screenshot(source)
        self.assertEqual((1080, 2160), out.size)
        self.assertIn("cropped 108 rows from the top", note)

    def test_any_other_size_is_scaled_and_padded_whole(self):
        source = g.Image.new("RGB", (1440, 3120), (250, 248, 255))
        out, note = g.fit_phone_screenshot(source)
        self.assertEqual((1080, 2160), out.size)
        self.assertIn("scaled and padded", note)
        self.assertEqual([], g.screenshot_problems("x.png", out.size, out.mode))


if __name__ == "__main__":
    unittest.main()
