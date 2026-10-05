package org.pebblentn.iconprobe;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/**
 * Saves each distinct large icon a navigation app posts as <files>/<package>/<md5>.png and logs
 * every post to <files>/log.tsv: time, package, title, text, md5 of the pixels, 64-bit dHash, size.
 * Pull with: adb pull /sdcard/Android/data/org.pebblentn.iconprobe/files
 */
public class ProbeListener extends NotificationListenerService {
    private static final java.util.List<String> PACKAGES = java.util.List.of(
            "app.comaps", "app.organicmaps.web", "app.organicmaps", "com.google.android.apps.maps");
    private final Set<String> saved = new HashSet<>();
    /** Reference renders per package: turn drawable name -> bitmap at intrinsic size. */
    private final java.util.Map<String, java.util.Map<String, Bitmap>> refs = new java.util.HashMap<>();
    /**
     * Renders each installed app's turn drawables by resource name, as the app does for its
     * notification (intrinsic size, ARGB_8888), into <files>/refs.tsv: package, name, resource id,
     * md5, dHash, size. Shows whether the names survive the release build and whether a reference
     * render equals the posted icon.
     */
    @Override
    public void onListenerConnected() {
        StringBuilder out = new StringBuilder("#refs " + System.currentTimeMillis() + " density=" + getResources().getDisplayMetrics().densityDpi + "\n");
        for (String pkg : PACKAGES) {
            try {
                android.content.Context other = createPackageContext(pkg, 0);
                android.content.res.Resources res = other.getResources();
                // Enumerate the app's drawables (type 0x08 in the usual 0x7f package; the type id is
                // read from a known drawable when one resolves) and keep the turn icons by name.
                int probe = firstDrawable(res, pkg);
                if (probe == 0) { out.append(pkg).append("\tno drawables found\n"); continue; }
                int base = probe & 0xffff0000;
                int misses = 0;  // ids can have gaps: stop after a long run of missing ones
                for (int id = base; id < base + 0x10000 && misses < 1024; id++) {
                    String name;
                    try { name = res.getResourceEntryName(id); misses = 0; }
                    catch (android.content.res.Resources.NotFoundException e) { misses++; continue; }
                    if (!(name.startsWith("ic_turn_") || name.startsWith("ic_exit_highway") || name.startsWith("maneuver_"))) continue;
                    String md5 = "-", dh = "-", size = "-";
                    try {
                        Bitmap b = bitmap(other.getDrawable(id));
                        refs.computeIfAbsent(pkg, k -> new java.util.LinkedHashMap<>()).put(name, b);
                        md5 = md5(b); dh = dHash(b); size = b.getWidth() + "x" + b.getHeight();
                    } catch (Exception e) {
                        md5 = "render-error:" + e.getClass().getSimpleName();
                    }
                    out.append(pkg).append('\t').append(name).append('\t').append(Integer.toHexString(id)).append('\t')
                            .append(md5).append('\t').append(dh).append('\t').append(size).append('\n');
                }
            } catch (Exception e) {
                out.append(pkg).append("\terror\t").append(e).append('\n');
            }
        }
        try (FileWriter w = new FileWriter(new File(getExternalFilesDir(null), "refs.tsv"), true)) {
            w.write(out.toString());
        } catch (Exception e) {
            android.util.Log.w("IconProbe", e);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!PACKAGES.contains(sbn.getPackageName())) return;
        try {
            Notification n = sbn.getNotification();
            Bundle x = n.extras;
            Icon large = n.getLargeIcon();
            Bitmap bmp = large == null ? null : bitmap(large.loadDrawable(this));
            String md5 = "-", dhash = "-", size = "-";
            if (bmp != null) {
                md5 = md5(bmp);
                dhash = dHash(bmp);
                size = bmp.getWidth() + "x" + bmp.getHeight();
                File dir = new File(getExternalFilesDir(null), sbn.getPackageName());
                if (saved.add(md5)) {
                    dir.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(new File(dir, md5 + ".png"))) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                }
            }
            String match = bmp == null ? "-" : closest(sbn.getPackageName(), bmp);
            String line = System.currentTimeMillis() + "\t" + sbn.getPackageName() + "\t" + text(x, Notification.EXTRA_TITLE)
                    + "\t" + text(x, Notification.EXTRA_TEXT) + "\t" + md5 + "\t" + dhash + "\t" + size + "\t" + match + "\n";
            try (FileWriter w = new FileWriter(new File(getExternalFilesDir(null), "log.tsv"), true)) {
                w.write(line);
            }
        } catch (Exception e) {
            android.util.Log.w("IconProbe", e);
        }
    }

    /**
     * The reference turn closest to a posted icon: each reference scaled to the icon's size (the system
     * scales large icons down to 48 dp), then the mean absolute alpha difference, 0..255. Logged as
     * "name:diff/second-best-name:diff", so the margin between the best and the runner-up shows.
     */
    private String closest(String pkg, Bitmap icon) {
        java.util.Map<String, Bitmap> r = refs.get(pkg);
        if (r == null) return "no-refs";
        int w = icon.getWidth(), h = icon.getHeight();
        int[] a = new int[w * h];
        icon.getPixels(a, 0, w, 0, 0, w, h);
        String best = "?", second = "?";
        double bestD = Double.MAX_VALUE, secondD = Double.MAX_VALUE;
        for (java.util.Map.Entry<String, Bitmap> e : r.entrySet()) {
            Bitmap s = Bitmap.createScaledBitmap(e.getValue(), w, h, true);
            int[] b = new int[w * h];
            s.getPixels(b, 0, w, 0, 0, w, h);
            long sum = 0;
            for (int i = 0; i < a.length; i++) sum += Math.abs((a[i] >>> 24) - (b[i] >>> 24));
            double d = (double) sum / a.length;
            if (d < bestD) { second = best; secondD = bestD; best = e.getKey(); bestD = d; }
            else if (d < secondD) { second = e.getKey(); secondD = d; }
        }
        return String.format(java.util.Locale.ROOT, "%s:%.2f/%s:%.2f", best, bestD, second, secondD);
    }

    /** Some drawable of the app, to find its drawable type id. */
    private static int firstDrawable(android.content.res.Resources res, String pkg) {
        for (String name : new String[]{"ic_turn_straight", "maneuver_straight", "ic_launcher_foreground", "ic_launcher"}) {
            int id = res.getIdentifier(name, "drawable", pkg);
            if (id != 0) return id;
        }
        return 0;
    }

    private static String md5(Bitmap bmp) throws Exception {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        MessageDigest md = MessageDigest.getInstance("MD5");
        for (int p : px) md.update(new byte[]{(byte) (p >> 24), (byte) (p >> 16), (byte) (p >> 8), (byte) p});
        return hex(md.digest());
    }

    /** Like CoMaps' Graphics.drawableToBitmap: the bitmap itself, or a render at intrinsic size. */
    private static Bitmap bitmap(Drawable d) {
        if (d == null) return null;
        if (d instanceof BitmapDrawable) return ((BitmapDrawable) d).getBitmap();
        Bitmap b = Bitmap.createBitmap(Math.max(1, d.getIntrinsicWidth()), Math.max(1, d.getIntrinsicHeight()), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, c.getWidth(), c.getHeight());
        d.draw(c);
        return b;
    }

    /**
     * Difference hash of the icon's shape: a 9x8 thumbnail of the alpha channel (the arrows are one
     * colour on transparent), one bit per horizontal neighbour comparison. Density-independent, unlike
     * the pixel MD5.
     */
    private static String dHash(Bitmap bmp) {
        Bitmap s = Bitmap.createScaledBitmap(bmp, 9, 8, true);
        long bits = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                bits = (bits << 1) | ((s.getPixel(x, y) >>> 24) > (s.getPixel(x + 1, y) >>> 24) ? 1 : 0);
            }
        }
        return String.format("%016x", bits);
    }

    private static String text(Bundle x, String key) {
        CharSequence c = x.getCharSequence(key);
        return c == null ? "" : c.toString().replace('\t', ' ').replace('\n', ' ');
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte v : b) s.append(String.format("%02x", v));
        return s.toString();
    }
}
