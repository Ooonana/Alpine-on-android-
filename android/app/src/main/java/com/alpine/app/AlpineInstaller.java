package com.alpine.app;

import android.annotation.SuppressLint;
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
import com.alpine.shared.errors.Error;
import com.alpine.shared.interact.MessageDialogUtils;
import com.alpine.shared.logger.Logger;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.shell.command.environment.AlpineShellEnvironment;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static com.alpine.shared.alpine.AlpineConstants.*;

final class AlpineInstaller {
    private static final String LOG_TAG = "AlpineInstaller";
    private static final String BOOTSTRAP_VERSION = "v68.1";
    private static final String BOOTSTRAP_VERSION_FILE_PATH = ALPINE_PREFIX_DIR_PATH + "/etc/alpine-bootstrap-version";
    private static final String BOOTSTRAP_BACKUP_DIR_PATH = ALPINE_PREFIX_DIR_PATH + "-backup";
    private static final File BOOTSTRAP_BACKUP_DIR = new File(BOOTSTRAP_BACKUP_DIR_PATH);
    // Keep generous headroom for filesystem allocation and transactional staging
    // while the previous prefix is still present during an upgrade.
    private static final long MIN_BOOTSTRAP_FREE_BYTES = 192L * 1024L * 1024L;
    private static final String ROOTFS_RELATIVE_PATH = "var/lib/proot-distro/installed-rootfs/alpine";
    private static final String[] REQUIRED_BOOTSTRAP_FILES = {
        "bin/proot",
        "bin/proot-distro",
        "lib/libandroid-shmem.so",
        "lib/libtalloc.so.2",
        "libexec/proot/loader",
        "etc/bash.bashrc",
        "etc/alpine-bootstrap-version",
        ROOTFS_RELATIVE_PATH + "/bin/sh",
        ROOTFS_RELATIVE_PATH + "/etc/alpine-release",
        ROOTFS_RELATIVE_PATH + "/etc/alpine-bootstrap-version",
        ROOTFS_RELATIVE_PATH + "/sbin/apk",
        ROOTFS_RELATIVE_PATH + "/usr/local/bin/start-x11",
        ROOTFS_RELATIVE_PATH + "/usr/local/bin/install-desktop",
        ROOTFS_RELATIVE_PATH + "/usr/local/bin/start-desktop"
    };
    private static final String[] REQUIRED_EXECUTABLE_FILES = {
        "bin/proot",
        "bin/proot-distro",
        "libexec/proot/loader",
        ROOTFS_RELATIVE_PATH + "/bin/sh",
        ROOTFS_RELATIVE_PATH + "/sbin/apk",
        ROOTFS_RELATIVE_PATH + "/usr/local/bin/start-x11",
        ROOTFS_RELATIVE_PATH + "/usr/local/bin/install-desktop",
        ROOTFS_RELATIVE_PATH + "/usr/local/bin/start-desktop"
    };

    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        if (FileUtils.directoryFileExists(ALPINE_PREFIX_DIR_PATH, true) &&
                !AlpineFileUtils.isAlpinePrefixDirectoryEmpty() &&
                installedBootstrapLooksUsable()) {
            cleanupStaleBackupAsync();
            whenDone.run(); return;
        }
        final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        new Thread() {
            @Override
            public void run() {
                boolean oldPrefixBackedUp = false;
                boolean newPrefixActivated = false;
                try {
                    recoverInterruptedBootstrapInstall();
                    deletePathOrThrow("staging", ALPINE_STAGING_PREFIX_DIR_PATH, true);
                    ensureBootstrapFreeSpace(activity);
                    final byte[] buffer = new byte[8192];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(100);
                    final List<Pair<String, Integer>> modes = new ArrayList<>(5000);
                    final Set<String> seenEntries = new HashSet<>();
                    final Set<String> seenSymlinkPaths = new HashSet<>();
                    final Set<String> seenModePaths = new HashSet<>();
                    final Set<String> extractedPaths = new HashSet<>();
                    boolean foundSymlinkManifest = false;
                    boolean foundModeManifest = false;
                    try (ZipInputStream zipInput = new ZipInputStream(loadZipStream())) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            final String entryName = zipEntry.getName();
                            if (!seenEntries.add(entryName)) throw new IOException("Duplicate bootstrap entry: " + entryName);
                            if (entryName.equals("SYMLINKS.txt")) {
                                foundSymlinkManifest = true;
                                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInput, StandardCharsets.UTF_8));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    String[] parts = line.split("←", 2);
                                    if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty())
                                        throw new IOException("Malformed bootstrap symlink entry: " + line);
                                    File linkFile = safeStagingTarget(parts[1]);
                                    if (!seenSymlinkPaths.add(linkFile.getAbsolutePath()))
                                        throw new IOException("Duplicate bootstrap symlink path: " + parts[1]);
                                    symlinks.add(Pair.create(parts[0], linkFile.getAbsolutePath()));
                                    File parent = linkFile.getParentFile();
                                    if (parent != null && !parent.isDirectory() && !parent.mkdirs())
                                        throw new IOException("Could not create symlink parent: " + parent);
                                }
                            } else if (entryName.equals("MODES.txt")) {
                                foundModeManifest = true;
                                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInput, StandardCharsets.UTF_8));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.isEmpty()) continue;
                                    String[] parts = line.split("\t", 2);
                                    if (parts.length != 2 || !parts[0].matches("[0-7]{4}") || parts[1].isEmpty())
                                        throw new IOException("Malformed bootstrap mode entry: " + line);
                                    int mode;
                                    try {
                                        mode = Integer.parseInt(parts[0], 8);
                                    } catch (NumberFormatException e) {
                                        throw new IOException("Invalid bootstrap mode: " + parts[0], e);
                                    }
                                    File modeFile = safeStagingTarget(parts[1]);
                                    String modePath = modeFile.getAbsolutePath();
                                    if (!seenModePaths.add(modePath))
                                        throw new IOException("Duplicate bootstrap mode path: " + parts[1]);
                                    modes.add(Pair.create(modePath, mode));
                                }
                            } else {
                                if (entryName.startsWith("home/")) continue;
                                File target = safeStagingTarget(entryName);
                                if (!extractedPaths.add(target.getAbsolutePath()))
                                    throw new IOException("Duplicate canonical bootstrap path: " + entryName);
                                if (zipEntry.isDirectory()) {
                                    if (!target.isDirectory() && !target.mkdirs())
                                        throw new IOException("Could not create bootstrap directory: " + entryName);
                                } else {
                                    File parent = target.getParentFile();
                                    if (parent != null && !parent.isDirectory() && !parent.mkdirs())
                                        throw new IOException("Could not create bootstrap parent: " + parent);
                                    try (FileOutputStream out = new FileOutputStream(target)) {
                                        int read;
                                        while ((read = zipInput.read(buffer)) != -1) out.write(buffer, 0, read);
                                    }
                                }
                            }
                        }
                    }
                    if (!foundSymlinkManifest || symlinks.isEmpty())
                        throw new IOException("Bootstrap symlink manifest is missing or empty");
                    if (!foundModeManifest || modes.isEmpty())
                        throw new IOException("Bootstrap mode manifest is missing or empty");
                    if (!seenModePaths.equals(extractedPaths)) {
                        Set<String> missingModes = new HashSet<>(extractedPaths);
                        missingModes.removeAll(seenModePaths);
                        Set<String> extraModes = new HashSet<>(seenModePaths);
                        extraModes.removeAll(extractedPaths);
                        throw new IOException("Bootstrap mode manifest mismatch: missing=" + missingModes.size() + ", extra=" + extraModes.size());
                    }
                    for (Pair<String, String> symlink : symlinks) {
                        File linkFile = new File(symlink.second);
                        if (linkFile.exists() && !linkFile.delete())
                            throw new IOException("Could not replace bootstrap symlink placeholder: " + symlink.second);
                        Os.symlink(symlink.first, symlink.second);
                    }
                    for (Pair<String, Integer> mode : modes) {
                        Os.chmod(mode.first, mode.second);
                    }

                    validateBootstrapDirectory(ALPINE_STAGING_PREFIX_DIR, true);

                    // The packaged marker validates staging, but installation is only considered
                    // committed after the live prefix has been activated and environment setup ran.
                    File stagingMarker = new File(ALPINE_STAGING_PREFIX_DIR_PATH + "/etc/alpine-bootstrap-version");
                    if (!stagingMarker.delete()) throw new IOException("Could not prepare bootstrap commit marker");

                    deletePathOrThrow("stale bootstrap backup", BOOTSTRAP_BACKUP_DIR_PATH, true);
                    if (ALPINE_PREFIX_DIR.exists()) {
                        if (!ALPINE_PREFIX_DIR.renameTo(BOOTSTRAP_BACKUP_DIR))
                            throw new IOException("Could not preserve existing Alpine prefix before upgrade");
                        oldPrefixBackedUp = true;
                    }
                    if (!ALPINE_STAGING_PREFIX_DIR.renameTo(ALPINE_PREFIX_DIR))
                        throw new IOException("Could not activate prepared Alpine prefix");
                    newPrefixActivated = true;

                    AlpineShellEnvironment.writeEnvironmentToFile(activity);
                    File environmentFile = new File(AlpineConstants.ALPINE_ENV_FILE_PATH);
                    if (!environmentFile.isFile() || environmentFile.length() == 0)
                        throw new IOException("Could not write Alpine shell environment");
                    writeBootstrapVersionMarker();
                    validateBootstrapDirectory(ALPINE_PREFIX_DIR, true);

                    if (oldPrefixBackedUp) deletePathBestEffort("old bootstrap backup", BOOTSTRAP_BACKUP_DIR_PATH);
                    activity.runOnUiThread(whenDone);
                } catch (Exception e) {
                    rollbackBootstrapInstall(oldPrefixBackedUp, newPrefixActivated);
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
        return bootstrapVersionIsCurrent(new File(BOOTSTRAP_VERSION_FILE_PATH));
    }

    private static boolean bootstrapVersionIsCurrent(File marker) {
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

    private static boolean installedBootstrapLooksUsable() {
        try {
            validateBootstrapDirectory(ALPINE_PREFIX_DIR, true);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void validateBootstrapDirectory(File prefix, boolean requireCommitMarker) throws IOException {
        if (!prefix.isDirectory()) throw new IOException("Alpine prefix directory is missing");
        for (String relativePath : REQUIRED_BOOTSTRAP_FILES) {
            File required = new File(prefix, relativePath);
            if (!required.isFile()) throw new IOException("Required bootstrap file is missing: " + relativePath);
        }
        for (String relativePath : REQUIRED_EXECUTABLE_FILES) {
            File required = new File(prefix, relativePath);
            if (!required.canExecute()) throw new IOException("Required bootstrap file is not executable: " + relativePath);
        }
        if (requireCommitMarker && !bootstrapVersionIsCurrent(new File(prefix, "etc/alpine-bootstrap-version")))
            throw new IOException("Bootstrap version marker is not " + BOOTSTRAP_VERSION);
        if (!bootstrapVersionIsCurrent(new File(prefix, ROOTFS_RELATIVE_PATH + "/etc/alpine-bootstrap-version")))
            throw new IOException("Rootfs version marker is not " + BOOTSTRAP_VERSION);
    }

    private static File safeStagingTarget(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isEmpty() || relativePath.startsWith("/") || relativePath.indexOf('\0') >= 0)
            throw new IOException("Unsafe bootstrap path: " + relativePath);
        File stagingRoot = ALPINE_STAGING_PREFIX_DIR.getCanonicalFile();
        File target = new File(stagingRoot, relativePath).getCanonicalFile();
        String rootPath = stagingRoot.getPath() + File.separator;
        if (!target.getPath().startsWith(rootPath)) throw new IOException("Bootstrap path escapes staging: " + relativePath);
        return target;
    }

    @SuppressLint("UsableSpace")
    private static void ensureBootstrapFreeSpace(Context context) throws IOException {
        // Intentionally use actually writable free space instead of allocatable
        // bytes: bootstrap setup should fail conservatively rather than evicting
        // other applications' caches just to satisfy the preflight.
        long usable = context.getFilesDir().getUsableSpace();
        if (usable >= 0 && usable < MIN_BOOTSTRAP_FREE_BYTES) {
            long availableMiB = usable / (1024L * 1024L);
            long requiredMiB = MIN_BOOTSTRAP_FREE_BYTES / (1024L * 1024L);
            throw new IOException("Not enough free storage for Alpine setup. Need about " + requiredMiB + " MiB free; available: " + availableMiB + " MiB.");
        }
    }

    private static void recoverInterruptedBootstrapInstall() throws IOException {
        if (!ALPINE_PREFIX_DIR.exists() && BOOTSTRAP_BACKUP_DIR.exists()) {
            if (!BOOTSTRAP_BACKUP_DIR.renameTo(ALPINE_PREFIX_DIR))
                throw new IOException("Could not restore Alpine after an interrupted update");
            return;
        }
        if (ALPINE_PREFIX_DIR.exists() && BOOTSTRAP_BACKUP_DIR.exists()) {
            if (installedBootstrapLooksUsable()) {
                deletePathBestEffort("stale bootstrap backup", BOOTSTRAP_BACKUP_DIR_PATH);
            } else {
                deletePathOrThrow("incomplete Alpine prefix", ALPINE_PREFIX_DIR_PATH, false);
                if (!BOOTSTRAP_BACKUP_DIR.renameTo(ALPINE_PREFIX_DIR))
                    throw new IOException("Could not restore previous Alpine prefix");
            }
        }
    }

    private static void rollbackBootstrapInstall(boolean oldPrefixBackedUp, boolean newPrefixActivated) {
        deletePathBestEffort("failed bootstrap staging", ALPINE_STAGING_PREFIX_DIR_PATH);
        if (newPrefixActivated) deletePathBestEffort("failed Alpine prefix", ALPINE_PREFIX_DIR_PATH);
        if (oldPrefixBackedUp && BOOTSTRAP_BACKUP_DIR.exists() && !ALPINE_PREFIX_DIR.exists()) {
            if (!BOOTSTRAP_BACKUP_DIR.renameTo(ALPINE_PREFIX_DIR))
                Logger.logError(LOG_TAG, "Could not restore previous Alpine prefix after failed update");
        }
    }

    private static void cleanupStaleBackupAsync() {
        if (!BOOTSTRAP_BACKUP_DIR.exists()) return;
        new Thread(() -> deletePathBestEffort("stale bootstrap backup", BOOTSTRAP_BACKUP_DIR_PATH)).start();
    }

    private static void deletePathOrThrow(String label, String path, boolean ignoreNonExistent) throws IOException {
        Error error = FileUtils.deleteFile(label, path, ignoreNonExistent);
        if (error != null) throw new IOException(error.toString());
    }

    private static void deletePathBestEffort(String label, String path) {
        Error error = FileUtils.deleteFile(label, path, true);
        if (error != null) Logger.logError(LOG_TAG, error.toString());
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
                    Error clearError = FileUtils.clearDirectory("~/storage", storageDir.getAbsolutePath());
                    if (clearError != null) {
                        Logger.logError(LOG_TAG, "Storage setup failed: " + clearError);
                        return;
                    }

                    File externalStorage = Environment.getExternalStorageDirectory();
                    if (!externalStorage.isDirectory() || !externalStorage.canRead()) {
                        Logger.logError(LOG_TAG, "Storage setup failed: primary external storage is not readable");
                        return;
                    }

                    String externalPath = externalStorage.getAbsolutePath();
                    String sharedPath = new File(storageDir, "shared").getAbsolutePath();
                    Os.symlink(externalPath, sharedPath);
                    if (!externalPath.equals(Os.readlink(sharedPath))) {
                        throw new IOException("Shared storage symlink target does not match primary external storage");
                    }
                } catch (Exception e) {
                    Logger.logError(LOG_TAG, "Storage error: " + e.getMessage());
                }
            }
        }.start();
    }

    private static InputStream loadZipStream() throws IOException {
        System.loadLibrary("alpine-bootstrap");
        ByteBuffer buffer = getZipBuffer();
        if (buffer == null || !buffer.hasRemaining()) throw new IOException("Embedded bootstrap is empty");
        return new ByteBufferInputStream(buffer.asReadOnlyBuffer());
    }

    public static native ByteBuffer getZipBuffer();

    private static final class ByteBufferInputStream extends InputStream {
        private final ByteBuffer buffer;

        ByteBufferInputStream(ByteBuffer buffer) { this.buffer = buffer; }

        @Override
        public int read() {
            return buffer.hasRemaining() ? buffer.get() & 0xff : -1;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (length == 0) return 0;
            if (!buffer.hasRemaining()) return -1;
            int count = Math.min(length, buffer.remaining());
            buffer.get(bytes, offset, count);
            return count;
        }
    }
}
