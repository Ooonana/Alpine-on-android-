package com.alpine.shared.alpine.shell.am;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.shared.errors.Error;
import com.alpine.shared.logger.Logger;
import com.alpine.shared.net.socket.local.LocalClientSocket;
import com.alpine.shared.net.socket.local.LocalServerSocket;
import com.alpine.shared.net.socket.local.LocalSocketManager;
import com.alpine.shared.net.socket.local.LocalSocketManagerClientBase;
import com.alpine.shared.net.socket.local.LocalSocketRunConfig;
import com.alpine.shared.shell.am.AmSocketServerRunConfig;
import com.alpine.shared.shell.am.AmSocketServer;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.crash.AlpineCrashUtils;
import com.alpine.shared.alpine.plugins.AlpinePluginUtils;
import com.alpine.shared.alpine.settings.properties.AlpineAppSharedProperties;
import com.alpine.shared.alpine.settings.properties.AlpinePropertyConstants;
import com.alpine.shared.alpine.shell.command.environment.AlpineAppShellEnvironment;

/**
 * A wrapper for {@link AmSocketServer} for alpine-app usage.
 *
 * The static {@link #alpineAmSocketServer} variable stores the {@link LocalSocketManager} for the
 * {@link AmSocketServer}.
 *
 * The {@link AlpineAmSocketServerClient} extends the {@link AmSocketServer.AmSocketServerClient}
 * class to also show plugin error notifications for errors and disallowed client connections in
 * addition to logging the messages to logcat, which are only logged by {@link LocalSocketManagerClientBase}
 * if log level is debug or higher for privacy issues.
 *
 * It uses a filesystem socket server with the socket file at
 * {@link AlpineConstants.ALPINE_APP#ALPINE_AM_SOCKET_FILE_PATH}. It would normally only allow
 * processes belonging to the alpine user and root user to connect to it. If commands are sent by the
 * root user, then the am commands executed will be run as the alpine user and its permissions,
 * capabilities and selinux context instead of root.
 *
 * The `$PREFIX/bin/alpine-am` client connects to the server via `$PREFIX/bin/alpine-am-socket` to
 * run the am commands. It provides similar functionality to "$PREFIX/bin/am"
 * (and "/system/bin/am"), but should be faster since it does not require starting a dalvik vm for
 * every command as done by "am" via alpine/AlpineAm.
 *
 * The server is started by alpine-app Application class but is not started if
 * {@link AlpinePropertyConstants#KEY_RUN_ALPINE_AM_SOCKET_SERVER} is `false` which can be done by
 * adding the prop with value "false" to the "~/.alpine/alpine.properties" file. Changes
 * require alpine-app to be force stopped and restarted.
 *
 * The current state of the server can be checked with the
 * {@link AlpineAppShellEnvironment#ENV_ALPINE_APP__AM_SOCKET_SERVER_ENABLED} env variable, which is exported
 * for all shell sessions and tasks.
 *
 * https://github.com/termux/termux-am-socket
 * https://github.com/termux/TermuxAm
 */
public class AlpineAmSocketServer {

    public static final String LOG_TAG = "AlpineAmSocketServer";

    public static final String TITLE = "AlpineAm";

    /** The static instance for the {@link AlpineAmSocketServer} {@link LocalSocketManager}. */
    private static LocalSocketManager alpineAmSocketServer;

    /** Whether {@link AlpineAmSocketServer} is enabled and running or not. */
    @Keep
    protected static Boolean ALPINE_APP_AM_SOCKET_SERVER_ENABLED;

    /**
     * Setup the {@link AmSocketServer} {@link LocalServerSocket} and start listening for
     * new {@link LocalClientSocket} if enabled.
     *
     * @param context The {@link Context} for {@link LocalSocketManager}.
     */
    public static void setupAlpineAmSocketServer(@NonNull Context context) {
        // Start alpine-am-socket server if enabled by user
        boolean enabled = false;
        if (AlpineAppSharedProperties.getProperties().shouldRunAlpineAmSocketServer()) {
            Logger.logDebug(LOG_TAG, "Starting " + TITLE + " socket server since its enabled");
            start(context);
            if (alpineAmSocketServer != null && alpineAmSocketServer.isRunning()) {
                enabled = true;
                Logger.logDebug(LOG_TAG, TITLE + " socket server successfully started");
            }
        } else {
            Logger.logDebug(LOG_TAG, "Not starting " + TITLE + " socket server since its not enabled");
        }

        // Once alpine-app has started, the server state must not be changed since the variable is
        // exported in shell sessions and tasks and if state is changed, then env of older shells will
        // retain invalid value. User should force stop the app to update state after changing prop.
        ALPINE_APP_AM_SOCKET_SERVER_ENABLED = enabled;
        AlpineAppShellEnvironment.updateAlpineAppAMSocketServerEnabled(context);
    }

    /**
     * Create the {@link AmSocketServer} {@link LocalServerSocket} and start listening for new {@link LocalClientSocket}.
     */
    public static synchronized void start(@NonNull Context context) {
        stop();

        AmSocketServerRunConfig amSocketServerRunConfig = new AmSocketServerRunConfig(TITLE,
            AlpineConstants.ALPINE_APP.ALPINE_AM_SOCKET_FILE_PATH, new AlpineAmSocketServerClient());

        alpineAmSocketServer = AmSocketServer.start(context, amSocketServerRunConfig);
    }

    /**
     * Stop the {@link AmSocketServer} {@link LocalServerSocket} and stop listening for new {@link LocalClientSocket}.
     */
    public static synchronized void stop() {
        if (alpineAmSocketServer != null) {
            Error error = alpineAmSocketServer.stop();
            if (error != null) {
                alpineAmSocketServer.onError(error);
            }
            alpineAmSocketServer = null;
        }
    }
    
    /**
     * Update the state of the {@link AmSocketServer} {@link LocalServerSocket} depending on current
     * value of {@link AlpinePropertyConstants#KEY_RUN_ALPINE_AM_SOCKET_SERVER}.
     */
    public static synchronized void updateState(@NonNull Context context) {
        AlpineAppSharedProperties properties = AlpineAppSharedProperties.getProperties();
        if (properties.shouldRunAlpineAmSocketServer()) {
            if (alpineAmSocketServer == null) {
                Logger.logDebug(LOG_TAG, "updateState: Starting " + TITLE + " socket server");
                start(context);
            }
        } else {
            if (alpineAmSocketServer != null) {
                Logger.logDebug(LOG_TAG, "updateState: Disabling " + TITLE + " socket server");
                stop();
            }
        }
    }
    
    /**
     * Get {@link #alpineAmSocketServer}.
     */
    public static synchronized LocalSocketManager getAlpineAmSocketServer() {
        return alpineAmSocketServer;
    }

    /**
     * Show an error notification on the {@link AlpineConstants#ALPINE_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID}
     * {@link AlpineConstants#ALPINE_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME} with a call
     * to {@link AlpinePluginUtils#sendPluginCommandErrorNotification(Context, String, CharSequence, String, String)}.
     *
     * @param context The {@link Context} to send the notification with.
     * @param error The {@link Error} generated.
     * @param localSocketRunConfig The {@link LocalSocketRunConfig} for {@link LocalSocketManager}.
     * @param clientSocket The optional {@link LocalClientSocket} for which the error was generated.
     */
    public static synchronized void showErrorNotification(@NonNull Context context, @NonNull Error error,
                                                          @NonNull LocalSocketRunConfig localSocketRunConfig,
                                                          @Nullable LocalClientSocket clientSocket) {
        AlpinePluginUtils.sendPluginCommandErrorNotification(context, LOG_TAG,
            localSocketRunConfig.getTitle() + " Socket Server Error", error.getMinimalErrorString(),
            LocalSocketManager.getErrorMarkdownString(error, localSocketRunConfig, clientSocket));
    }



    public static Boolean getAlpineAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        boolean isAlpineApp = AlpineConstants.ALPINE_PACKAGE_NAME.equals(currentPackageContext.getPackageName());
        if (isAlpineApp) {
            return ALPINE_APP_AM_SOCKET_SERVER_ENABLED;
        } else {
            // Currently, unsupported since plugin app processes don't know that value is set in alpine
            // app process AlpineAmSocketServer class. A binder API or a way to check if server is actually
            // running needs to be used. Long checks would also not be possible on main application thread
            return null;
        }

    }





    /** Enhanced implementation for {@link AmSocketServer.AmSocketServerClient} for {@link AlpineAmSocketServer}. */
    public static class AlpineAmSocketServerClient extends AmSocketServer.AmSocketServerClient {

        public static final String LOG_TAG = "AlpineAmSocketServerClient";

        @Nullable
        @Override
        public Thread.UncaughtExceptionHandler getLocalSocketManagerClientThreadUEH(
            @NonNull LocalSocketManager localSocketManager) {
            // Use alpine crash handler for socket listener thread just like used for main app process thread.
            return AlpineCrashUtils.getCrashHandler(localSocketManager.getContext());
        }

        @Override
        public void onError(@NonNull LocalSocketManager localSocketManager,
                            @Nullable LocalClientSocket clientSocket, @NonNull Error error) {
            // Don't show notification if server is not running since errors may be triggered
            // when server is stopped and server and client sockets are closed.
            if (localSocketManager.isRunning()) {
                AlpineAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                    localSocketManager.getLocalSocketRunConfig(), clientSocket);
            }

            // But log the exception
            super.onError(localSocketManager, clientSocket, error);
        }

        @Override
        public void onDisallowedClientConnected(@NonNull LocalSocketManager localSocketManager,
                                                @NonNull LocalClientSocket clientSocket, @NonNull Error error) {
            // Always show notification and log error regardless of if server is running or not
            AlpineAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                localSocketManager.getLocalSocketRunConfig(), clientSocket);
            super.onDisallowedClientConnected(localSocketManager, clientSocket, error);
        }



        @Override
        protected String getLogTag() {
            return LOG_TAG;
        }

    }

}
