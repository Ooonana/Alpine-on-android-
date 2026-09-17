package com.alpine.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.system.Os;
import android.util.Pair;
import com.alpine.R;
import com.alpine.shared.file.FileUtils;
import com.alpine.shared.alpine.file.AlpineFileUtils;
import com.alpine.shared.interact.MessageDialogUtils;
import com.alpine.shared.logger.Logger;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.shell.command.environment.AlpineShellEnvironment;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static com.alpine.shared.alpine.AlpineConstants.*;

final class AlpineInstaller {
    private static final String LOG_TAG = "AlpineInstaller";
    private static final String BOOTSTRAP_VERSION = "v66";
    private static final String BOOTSTRAP_VERSION_FILE_PATH = ALPINE_PREFIX_DIR_PATH + "/etc/alpine-bootstrap-version";

    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        if (FileUtils.directoryFileExists(ALPINE_PREFIX_DIR_PATH, true) &&
                !AlpineFileUtils.isAlpinePrefixDirectoryEmpty() &&
                bootstrapVersionIsCurrent()) {
            whenDone.run(); return;
        }
        final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteFile("prefix", ALPINE_PREFIX_DIR_PATH, true);
                    FileUtils.deleteFile("staging", ALPINE_STAGING_PREFIX_DIR_PATH, true);
                    final byte[] buffer = new byte[8192];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(100);
                    final byte[] zipBytes = loadZipBytes();
                    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInput));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    String[] parts = line.split("←");
                                    if (parts.length == 2) {
                                        symlinks.add(Pair.create(parts[0], ALPINE_STAGING_PREFIX_DIR_PATH + "/" + parts[1]));
                                        new File(ALPINE_STAGING_PREFIX_DIR_PATH + "/" + parts[1]).getParentFile().mkdirs();
                                    }
                                }
                            } else {
                                if (zipEntry.getName().startsWith("home/")) continue;
                                File target = new File(ALPINE_STAGING_PREFIX_DIR_PATH, zipEntry.getName());
                                if (zipEntry.isDirectory()) { target.mkdirs(); } else {
                                    target.getParentFile().mkdirs();
                                    try (FileOutputStream out = new FileOutputStream(target)) {
                                        int read;
                                        while ((read = zipInput.read(buffer)) != -1) out.write(buffer, 0, read);
                                    }
                                    // RECURSIVE CHMOD: Check if path contains bin, lib, or libexec anywhere
                                    String path = zipEntry.getName().toLowerCase();
                                    if (path.contains("/bin/") || path.startsWith("bin/") ||
                                        path.contains("/sbin/") || path.startsWith("sbin/") ||
                                        path.contains("/lib/") || path.startsWith("lib/") || 
                                        path.contains("/libexec/") || path.startsWith("libexec/")) {
                                        Os.chmod(target.getAbsolutePath(), 0700);
                                    }
                                }
                            }
                        }
                    }
                    for (Pair<String, String> symlink : symlinks) { 
                        try { new File(symlink.second).delete(); Os.symlink(symlink.first, symlink.second); } catch (Exception ignored) {} 
                    }
                    if (!ALPINE_STAGING_PREFIX_DIR.renameTo(ALPINE_PREFIX_DIR)) throw new RuntimeException("Rename failed");
                    AlpineShellEnvironment.writeEnvironmentToFile(activity);
                    writeBootstrapVersionMarker();
                    activity.runOnUiThread(whenDone);
                } catch (Exception e) {
                    activity.runOnUiThread(() -> {
                        String message = e.getMessage();
                        if (message == null || message.trim().isEmpty())
                            message = activity.getString(R.string.bootstrap_error_body);
                        MessageDialogUtils.exitAppWithErrorMessage(
                            activity, activity.getString(R.string.bootstrap_error_title), message);
                    });
                } finally {
                    activity.runOnUiThread(() -> { try { progress.dismiss(); } catch (Exception ignored) {} });
                }
            }
        }.start();
    }

    private static boolean bootstrapVersionIsCurrent() {
        File marker = new File(BOOTSTRAP_VERSION_FILE_PATH);
        if (!marker.isFile()) return false;
        int length = (int) Math.min(marker.length(), 64);
        byte[] data = new byte[length];
        try (FileInputStream in = new FileInputStream(marker)) {
            int read = in.read(data);
            if (read <= 0) return false;
            return BOOTSTRAP_VERSION.equals(new String(data, 0, read, StandardCharsets.UTF_8).trim());
        } catch (IOException e) {
            return false;
        }
    }

    private static void writeBootstrapVersionMarker() throws IOException {
        File marker = new File(BOOTSTRAP_VERSION_FILE_PATH);
        File parent = marker.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(marker)) {
            out.write((BOOTSTRAP_VERSION + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    static void setupStorageSymlinks(final Context context) {
        new Thread() {
            public void run() {
                try {
                    File storageDir = AlpineConstants.ALPINE_STORAGE_HOME_DIR;
                    FileUtils.clearDirectory("~/storage", storageDir.getAbsolutePath());
                    Os.symlink(Environment.getExternalStorageDirectory().getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());
                } catch (Exception e) {
                    Logger.logError(LOG_TAG, "Storage error: " + e.getMessage());
                }
            }
        }.start();
    }

    public static byte[] loadZipBytes() { System.loadLibrary("alpine-bootstrap"); return getZip(); }
    public static native byte[] getZip();
}
