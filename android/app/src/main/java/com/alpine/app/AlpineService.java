package com.alpine.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alpine.R;
import com.alpine.app.event.SystemEventReceiver;
import com.alpine.app.terminal.AlpineTerminalSessionActivityClient;
import com.alpine.app.terminal.AlpineTerminalSessionServiceClient;
import com.alpine.shared.alpine.plugins.AlpinePluginUtils;
import com.alpine.shared.data.IntentUtils;
import com.alpine.shared.net.uri.UriUtils;
import com.alpine.shared.errors.Errno;
import com.alpine.shared.shell.ShellUtils;
import com.alpine.shared.shell.command.runner.app.AppShell;
import com.alpine.shared.alpine.settings.properties.AlpineAppSharedProperties;
import com.alpine.shared.alpine.shell.command.environment.AlpineShellEnvironment;
import com.alpine.shared.alpine.shell.AlpineShellUtils;
import com.alpine.shared.alpine.AlpineConstants;
import com.alpine.shared.alpine.AlpineConstants.ALPINE_APP.ALPINE_ACTIVITY;
import com.alpine.shared.alpine.AlpineConstants.ALPINE_APP.ALPINE_SERVICE;
import com.alpine.shared.alpine.settings.preferences.AlpineAppSharedPreferences;
import com.alpine.shared.alpine.shell.AlpineShellManager;
import com.alpine.shared.alpine.shell.command.runner.terminal.AlpineSession;
import com.alpine.shared.alpine.terminal.AlpineTerminalSessionClientBase;
import com.alpine.shared.logger.Logger;
import com.alpine.shared.notification.NotificationUtils;
import com.alpine.shared.android.PermissionUtils;
import com.alpine.shared.data.DataUtils;
import com.alpine.shared.shell.command.ExecutionCommand;
import com.alpine.shared.shell.command.ExecutionCommand.Runner;
import com.alpine.shared.shell.command.ExecutionCommand.ShellCreateMode;
import com.alpine.terminal.TerminalEmulator;
import com.alpine.terminal.TerminalSession;
import com.alpine.terminal.TerminalSessionClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A service holding a list of {@link AlpineSession} in {@link AlpineShellManager#mAlpineSessions} and background {@link AppShell}
 * in {@link AlpineShellManager#mAlpineTasks}, showing a foreground notification while running so that it is not terminated.
 * The user interacts with the session through {@link AlpineActivity}, but this service may outlive
 * the activity when the user or the system disposes of the activity. In that case the user may
 * restart {@link AlpineActivity} later to yet again access the sessions.
 * <p/>
 * In order to keep both terminal sessions and spawned processes (who may outlive the terminal sessions) alive as long
 * as wanted by the user this service is a foreground service, {@link Service#startForeground(int, Notification)}.
 * <p/>
 * Optionally may hold a wake and a wifi lock, in which case that is shown in the notification - see
 * {@link #buildNotification()}.
 */
public final class AlpineService extends Service implements AppShell.AppShellClient, AlpineSession.AlpineSessionClient {

    /** This service is only bound from inside the same process and never uses IPC. */
    class LocalBinder extends Binder {
        public final AlpineService service = AlpineService.this;
    }

    private final IBinder mBinder = new LocalBinder();

    private final Handler mHandler = new Handler();


    /** The full implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that holds activity references for activity related functions.
     * Note that the service may often outlive the activity, so need to clear this reference.
     */
    private AlpineTerminalSessionActivityClient mAlpineTerminalSessionActivityClient;

    /** The basic implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that does not hold activity references and only a service reference.
     */
    private final AlpineTerminalSessionServiceClient mAlpineTerminalSessionServiceClient = new AlpineTerminalSessionServiceClient(this);

    /**
     * Alpine app shared properties manager, loaded from alpine.properties
     */
    private AlpineAppSharedProperties mProperties;

    /**
     * Alpine app shell manager
     */
    private AlpineShellManager mShellManager;

    /** The wake lock and wifi lock are always acquired and released together. */
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    /** If the user has executed the {@link ALPINE_SERVICE#ACTION_STOP_SERVICE} intent. */
    boolean mWantsToStop = false;

    private static final String LOG_TAG = "AlpineService";

    @Override
    public void onCreate() {
        Logger.logVerbose(LOG_TAG, "onCreate");

        // Get Alpine app SharedProperties without loading from disk since AlpineApplication handles
        // load and AlpineActivity handles reloads
        mProperties = AlpineAppSharedProperties.getProperties();

        mShellManager = AlpineShellManager.getShellManager();

        runStartForeground();

        SystemEventReceiver.registerPackageUpdateEvents(this);
    }

    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        String action = null;
        if (intent != null) {
            Logger.logVerboseExtended(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case ALPINE_SERVICE.ACTION_STOP_SERVICE:
                    Logger.logDebug(LOG_TAG, "ACTION_STOP_SERVICE intent received");
                    actionStopService();
                    break;
                case ALPINE_SERVICE.ACTION_WAKE_LOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_LOCK intent received");
                    actionAcquireWakeLock();
                    break;
                case ALPINE_SERVICE.ACTION_WAKE_UNLOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_UNLOCK intent received");
                    actionReleaseWakeLock(true);
                    break;
                case ALPINE_SERVICE.ACTION_SERVICE_EXECUTE:
                    Logger.logDebug(LOG_TAG, "ACTION_SERVICE_EXECUTE intent received");
                    actionServiceExecute(intent);
                    break;
                default:
                    Logger.logError(LOG_TAG, "Invalid action: \"" + action + "\"");
                    break;
            }
        }

        // If this service really do get killed, there is no point restarting it automatically - let the user do on next
        // start of {@link Term):
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.logVerbose(LOG_TAG, "onDestroy");

        AlpineShellUtils.clearAlpineTMPDIR(true);

        actionReleaseWakeLock(false);
        if (!mWantsToStop)
            killAllAlpineExecutionCommands();

        AlpineShellManager.onAppExit(this);

        SystemEventReceiver.unregisterPackageUpdateEvents(this);

        runStopForeground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onUnbind");

        // Since we cannot rely on {@link AlpineActivity.onDestroy()} to always complete,
        // we unset clients here as well if it failed, so that we do not leave service and session
        // clients with references to the activity.
        if (mAlpineTerminalSessionActivityClient != null)
            unsetAlpineTerminalSessionClient();
        return false;
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(AlpineConstants.ALPINE_APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }

    /** Request to stop service. */
    private void requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service");
        runStopForeground();
        stopSelf();
    }

    /** Process action to stop service. */
    private void actionStopService() {
        mWantsToStop = true;
        killAllAlpineExecutionCommands();
        requestStopService();
    }

    /** Kill all AlpineSessions and AlpineTasks by sending SIGKILL to their processes.
     *
     * For AlpineSessions, all sessions will be killed, whether user manually exited Alpine or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will only be done if user manually exited alpine or if the session was started by a plugin
     * which **expects** the result back via a pending intent.
     *
     * For AlpineTasks, only tasks that were started by a plugin which **expects** the result
     * back via a pending intent will be killed, whether user manually exited Alpine or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will always be done for the tasks that are killed. The remaining processes will keep on
     * running until the alpine app process is killed by android, like by OOM, so we let them run
     * as long as they can.
     *
     * Some plugin execution commands may not have been processed and added to mAlpineSessions and
     * mAlpineTasks lists before the service is killed, so we maintain a separate
     * mPendingPluginExecutionCommands list for those, so that we can notify the pending intent
     * creators that execution was cancelled.
     *
     * Note that if user didn't manually exit Alpine and if onDestroy() was directly called because
     * of unintended shutdown, like android deciding to kill the service, then there will be no
     * guarantee that onDestroy() will be allowed to finish and alpine app process may be killed before
     * it has finished. This means that in those cases some results may not be sent back to their
     * creators for plugin commands but we still try to process whatever results can be processed
     * despite the unreliable behaviour of onDestroy().
     *
     * Note that if don't kill the processes started by plugins which **expect** the result back
     * and notify their creators that they have been killed, then they may get stuck waiting for
     * the results forever like in case of commands started by Alpine:Tasker or RUN_COMMAND intent,
     * since once AlpineService has been killed, no result will be sent back. They may still get
     * stuck if alpine app process gets killed, so for this case reasonable timeout values should
     * be used, like in Tasker for the Alpine:Tasker actions.
     *
     * We make copies of each list since items are removed inside the loop.
     */
    private synchronized void killAllAlpineExecutionCommands() {
        boolean processResult;

        Logger.logDebug(LOG_TAG, "Killing AlpineSessions=" + mShellManager.mAlpineSessions.size() +
            ", AlpineTasks=" + mShellManager.mAlpineTasks.size() +
            ", PendingPluginExecutionCommands=" + mShellManager.mPendingPluginExecutionCommands.size());

        List<AlpineSession> alpineSessions = new ArrayList<>(mShellManager.mAlpineSessions);
        List<AppShell> alpineTasks = new ArrayList<>(mShellManager.mAlpineTasks);
        List<ExecutionCommand> pendingPluginExecutionCommands = new ArrayList<>(mShellManager.mPendingPluginExecutionCommands);

        for (int i = 0; i < alpineSessions.size(); i++) {
            ExecutionCommand executionCommand = alpineSessions.get(i).getExecutionCommand();
            processResult = mWantsToStop || executionCommand.isPluginExecutionCommandWithPendingResult();
            alpineSessions.get(i).killIfExecuting(this, processResult);
            if (!processResult)
                mShellManager.mAlpineSessions.remove(alpineSessions.get(i));
        }


        for (int i = 0; i < alpineTasks.size(); i++) {
            ExecutionCommand executionCommand = alpineTasks.get(i).getExecutionCommand();
            if (executionCommand.isPluginExecutionCommandWithPendingResult())
                alpineTasks.get(i).killIfExecuting(this, true);
            else
                mShellManager.mAlpineTasks.remove(alpineTasks.get(i));
        }

        for (int i = 0; i < pendingPluginExecutionCommands.size(); i++) {
            ExecutionCommand executionCommand = pendingPluginExecutionCommands.get(i);
            if (!executionCommand.shouldNotProcessResults() && executionCommand.isPluginExecutionCommandWithPendingResult()) {
                if (executionCommand.setStateFailed(Errno.ERRNO_CANCELLED.getCode(), this.getString(com.alpine.shared.R.string.error_execution_cancelled))) {
                    AlpinePluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);
                }
            }
        }
    }



    /** Process action to acquire Power and Wi-Fi WakeLocks. */
    @SuppressLint({"WakelockTimeout", "BatteryLife"})
    private void actionAcquireWakeLock() {
        if (mWakeLock != null) {
            Logger.logDebug(LOG_TAG, "Ignoring acquiring WakeLocks since they are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Acquiring WakeLocks");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AlpineConstants.ALPINE_APP_NAME.toLowerCase() + ":service-wakelock");
        mWakeLock.acquire();

        // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, AlpineConstants.ALPINE_APP_NAME.toLowerCase());
        mWifiLock.acquire();

        if (!PermissionUtils.checkIfBatteryOptimizationsDisabled(this)) {
            PermissionUtils.requestDisableBatteryOptimizations(this);
        }

        updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks acquired successfully");

    }

    /** Process action to release Power and Wi-Fi WakeLocks. */
    private void actionReleaseWakeLock(boolean updateNotification) {
        if (mWakeLock == null && mWifiLock == null) {
            Logger.logDebug(LOG_TAG, "Ignoring releasing WakeLocks since none are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Releasing WakeLocks");

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }

        if (updateNotification)
            updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks released successfully");
    }

    /** Process {@link ALPINE_SERVICE#ACTION_SERVICE_EXECUTE} intent to execute a shell command in
     * a foreground AlpineSession or in a background AlpineTask. */
    private void actionServiceExecute(Intent intent) {
        if (intent == null) {
            Logger.logError(LOG_TAG, "Ignoring null intent to actionServiceExecute");
            return;
        }

        ExecutionCommand executionCommand = new ExecutionCommand(AlpineShellManager.getNextShellId());

        executionCommand.executableUri = intent.getData();
        executionCommand.isPluginExecutionCommand = true;

        // If EXTRA_RUNNER is passed, use that, otherwise check EXTRA_BACKGROUND and default to Runner.TERMINAL_SESSION
        executionCommand.runner = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_RUNNER,
            (intent.getBooleanExtra(ALPINE_SERVICE.EXTRA_BACKGROUND, false) ? Runner.APP_SHELL.getName() : Runner.TERMINAL_SESSION.getName()));
        if (Runner.runnerOf(executionCommand.runner) == null) {
            String errmsg = this.getString(R.string.error_alpine_service_invalid_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            AlpinePluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            return;
        }

        if (executionCommand.executableUri != null) {
            Logger.logVerbose(LOG_TAG, "uri: \"" + executionCommand.executableUri + "\", path: \"" + executionCommand.executableUri.getPath() + "\", fragment: \"" + executionCommand.executableUri.getFragment() + "\"");

            // Get full path including fragment (anything after last "#")
            executionCommand.executable = UriUtils.getUriFilePathWithFragment(executionCommand.executableUri);
            executionCommand.arguments = IntentUtils.getStringArrayExtraIfSet(intent, ALPINE_SERVICE.EXTRA_ARGUMENTS, null);
            if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
                executionCommand.stdin = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_STDIN, null);
            executionCommand.backgroundCustomLogLevel = IntentUtils.getIntegerExtraIfSet(intent, ALPINE_SERVICE.EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL, null);
        }

        executionCommand.workingDirectory = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_WORKDIR, null);
        executionCommand.isFailsafe = intent.getBooleanExtra(ALPINE_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
        executionCommand.sessionAction = intent.getStringExtra(ALPINE_SERVICE.EXTRA_SESSION_ACTION);
        executionCommand.shellName = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_SHELL_NAME, null);
        executionCommand.shellCreateMode = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_SHELL_CREATE_MODE, null);
        executionCommand.commandLabel = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_COMMAND_LABEL, "Execution Intent Command");
        executionCommand.commandDescription = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_COMMAND_DESCRIPTION, null);
        executionCommand.commandHelp = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_COMMAND_HELP, null);
        executionCommand.pluginAPIHelp = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_PLUGIN_API_HELP, null);
        executionCommand.resultConfig.resultPendingIntent = intent.getParcelableExtra(ALPINE_SERVICE.EXTRA_PENDING_INTENT);
        executionCommand.resultConfig.resultDirectoryPath = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_RESULT_DIRECTORY, null);
        if (executionCommand.resultConfig.resultDirectoryPath != null) {
            executionCommand.resultConfig.resultSingleFile = intent.getBooleanExtra(ALPINE_SERVICE.EXTRA_RESULT_SINGLE_FILE, false);
            executionCommand.resultConfig.resultFileBasename = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_RESULT_FILE_BASENAME, null);
            executionCommand.resultConfig.resultFileOutputFormat = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_RESULT_FILE_OUTPUT_FORMAT, null);
            executionCommand.resultConfig.resultFileErrorFormat = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_RESULT_FILE_ERROR_FORMAT, null);
            executionCommand.resultConfig.resultFilesSuffix = IntentUtils.getStringExtraIfSet(intent, ALPINE_SERVICE.EXTRA_RESULT_FILES_SUFFIX, null);
        }

        if (executionCommand.shellCreateMode == null)
            executionCommand.shellCreateMode = ShellCreateMode.ALWAYS.getMode();

        // Add the execution command to pending plugin execution commands list
        mShellManager.mPendingPluginExecutionCommands.add(executionCommand);

        if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
            executeAlpineTaskCommand(executionCommand);
        else if (Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner))
            executeAlpineSessionCommand(executionCommand);
        else {
            String errmsg = getString(R.string.error_alpine_service_unsupported_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            AlpinePluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
        }
    }





    /** Execute a shell command in background AlpineTask. */
    private void executeAlpineTaskCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing background \"" + executionCommand.getCommandIdAndLabelLogString() + "\" AlpineTask command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        AppShell newAlpineTask = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newAlpineTask = getAlpineTaskForShellName(executionCommand.shellName);
            if (newAlpineTask != null)
                Logger.logVerbose(LOG_TAG, "Existing AlpineTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing AlpineTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newAlpineTask == null)
            newAlpineTask = createAlpineTask(executionCommand);
    }

    /** Create a AlpineTask. */
    @Nullable
    public AppShell createAlpineTask(String executablePath, String[] arguments, String stdin, String workingDirectory) {
        return createAlpineTask(new ExecutionCommand(AlpineShellManager.getNextShellId(), executablePath,
            arguments, stdin, workingDirectory, Runner.APP_SHELL.getName(), false));
    }

    /** Create a AlpineTask. */
    @Nullable
    public synchronized AppShell createAlpineTask(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" AlpineTask");

        if (!Runner.APP_SHELL.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createAlpineTask()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        AppShell newAlpineTask = AppShell.execute(this, executionCommand, this,
            new AlpineShellEnvironment(), null,false);
        if (newAlpineTask == null) {
            Logger.logError(LOG_TAG, "Failed to execute new AlpineTask command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                AlpinePluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mAlpineTasks.add(newAlpineTask);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        updateNotification();

        return newAlpineTask;
    }

    /** Callback received when a AlpineTask finishes. */
    @Override
    public void onAppShellExited(final AppShell alpineTask) {
        mHandler.post(() -> {
            if (alpineTask != null) {
                ExecutionCommand executionCommand = alpineTask.getExecutionCommand();

                Logger.logVerbose(LOG_TAG, "The onAlpineTaskExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" AlpineTask command");

                // If the execution command was started for a plugin, then process the results
                if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                    AlpinePluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

                mShellManager.mAlpineTasks.remove(alpineTask);
            }

            updateNotification();
        });
    }





    /** Execute a shell command in a foreground {@link AlpineSession}. */
    private void executeAlpineSessionCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing foreground \"" + executionCommand.getCommandIdAndLabelLogString() + "\" AlpineSession command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        AlpineSession newAlpineSession = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newAlpineSession = getAlpineSessionForShellName(executionCommand.shellName);
            if (newAlpineSession != null)
                Logger.logVerbose(LOG_TAG, "Existing AlpineSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing AlpineSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newAlpineSession == null)
            newAlpineSession = createAlpineSession(executionCommand);
        if (newAlpineSession == null) return;

        handleSessionAction(DataUtils.getIntFromString(executionCommand.sessionAction,
            ALPINE_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY),
            newAlpineSession.getTerminalSession());
    }

    /**
     * Create a {@link AlpineSession}.
     * Currently called by {@link AlpineTerminalSessionActivityClient#addNewSession(boolean, String)} to add a new {@link AlpineSession}.
     */
    @Nullable
    public AlpineSession createAlpineSession(String executablePath, String[] arguments, String stdin,
                                             String workingDirectory, boolean isFailSafe, String sessionName) {
        ExecutionCommand executionCommand = new ExecutionCommand(AlpineShellManager.getNextShellId(),
            executablePath, arguments, stdin, workingDirectory, Runner.TERMINAL_SESSION.getName(), isFailSafe);
        executionCommand.shellName = sessionName;
        return createAlpineSession(executionCommand);
    }

    /** Create a {@link AlpineSession}. */
    @Nullable
    public synchronized AlpineSession createAlpineSession(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" AlpineSession");

        if (!Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createAlpineSession()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;
        executionCommand.terminalTranscriptRows = mProperties.getTerminalTranscriptRows();

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        // If the execution command was started for a plugin, only then will the stdout be set
        // Otherwise if command was manually started by the user like by adding a new terminal session,
        // then no need to set stdout
        AlpineSession newAlpineSession = AlpineSession.execute(this, executionCommand, getAlpineTerminalSessionClient(),
            this, new AlpineShellEnvironment(), null, executionCommand.isPluginExecutionCommand);
        if (newAlpineSession == null) {
            Logger.logError(LOG_TAG, "Failed to execute new AlpineSession command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                AlpinePluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mAlpineSessions.add(newAlpineSession);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        // Notify {@link AlpineSessionsListViewController} that sessions list has been updated if
        // activity in is foreground
        if (mAlpineTerminalSessionActivityClient != null)
            mAlpineTerminalSessionActivityClient.alpineSessionListNotifyUpdated();

        updateNotification();

        // No need to recreate the activity since it likely just started and theme should already have applied
        AlpineActivity.updateAlpineActivityStyling(this, false);

        return newAlpineSession;
    }

    /** Remove a AlpineSession. */
    public synchronized int removeAlpineSession(TerminalSession sessionToRemove) {
        int index = getIndexOfSession(sessionToRemove);

        if (index >= 0)
            mShellManager.mAlpineSessions.get(index).finish();

        return index;
    }

    /** Callback received when a {@link AlpineSession} finishes. */
    @Override
    public void onAlpineSessionExited(final AlpineSession alpineSession) {
        if (alpineSession != null) {
            ExecutionCommand executionCommand = alpineSession.getExecutionCommand();

            Logger.logVerbose(LOG_TAG, "The onAlpineSessionExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" AlpineSession command");

            // If the execution command was started for a plugin, then process the results
            if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                AlpinePluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

            mShellManager.mAlpineSessions.remove(alpineSession);

            // Notify {@link AlpineSessionsListViewController} that sessions list has been updated if
            // activity in is foreground
            if (mAlpineTerminalSessionActivityClient != null)
                mAlpineTerminalSessionActivityClient.alpineSessionListNotifyUpdated();
        }

        updateNotification();
    }





    private ShellCreateMode processShellCreateMode(@NonNull ExecutionCommand executionCommand) {
        if (ShellCreateMode.ALWAYS.equalsMode(executionCommand.shellCreateMode))
            return ShellCreateMode.ALWAYS; // Default
        else if (ShellCreateMode.NO_SHELL_WITH_NAME.equalsMode(executionCommand.shellCreateMode))
            if (DataUtils.isNullOrEmpty(executionCommand.shellName)) {
                AlpinePluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                    getString(R.string.error_alpine_service_execution_command_shell_name_unset, executionCommand.shellCreateMode));
                return null;
            } else {
               return ShellCreateMode.NO_SHELL_WITH_NAME;
            }
        else {
            AlpinePluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                getString(R.string.error_alpine_service_unsupported_execution_command_shell_create_mode, executionCommand.shellCreateMode));
            return null;
        }
    }

    /** Process session action for new session. */
    private void handleSessionAction(int sessionAction, TerminalSession newTerminalSession) {
        Logger.logDebug(LOG_TAG, "Processing sessionAction \"" + sessionAction + "\" for session \"" + newTerminalSession.mSessionName + "\"");

        switch (sessionAction) {
            case ALPINE_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mAlpineTerminalSessionActivityClient != null)
                    mAlpineTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                startAlpineActivity();
                break;
            case ALPINE_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY:
                if (getAlpineSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                startAlpineActivity();
                break;
            case ALPINE_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mAlpineTerminalSessionActivityClient != null)
                    mAlpineTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                break;
            case ALPINE_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY:
                if (getAlpineSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid sessionAction: \"" + sessionAction + "\". Force using default sessionAction.");
                handleSessionAction(ALPINE_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY, newTerminalSession);
                break;
        }
    }

    /** Launch the {@link }AlpineActivity} to bring it to foreground. */
    private void startAlpineActivity() {
        // For android >= 10, apps require Display over other apps permission to start foreground activities
        // from background (services). If it is not granted, then AlpineSessions that are started will
        // show in Alpine notification but will not run until user manually clicks the notification.
        if (PermissionUtils.validateDisplayOverOtherAppsPermissionForPostAndroid10(this, true)) {
            AlpineActivity.startAlpineActivity(this);
        } else {
            AlpineAppSharedPreferences preferences = AlpineAppSharedPreferences.build(this);
            if (preferences == null) return;
            if (preferences.arePluginErrorNotificationsEnabled(false))
                Logger.showToast(this, this.getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal), true);
        }
    }





    /** If {@link AlpineActivity} has not bound to the {@link AlpineService} yet or is destroyed, then
     * interface functions requiring the activity should not be available to the terminal sessions,
     * so we just return the {@link #mAlpineTerminalSessionServiceClient}. Once {@link AlpineActivity} bind
     * callback is received, it should call {@link #setAlpineTerminalSessionClient} to set the
     * {@link AlpineService#mAlpineTerminalSessionActivityClient} so that further terminal sessions are directly
     * passed the {@link AlpineTerminalSessionActivityClient} object which fully implements the
     * {@link TerminalSessionClient} interface.
     *
     * @return Returns the {@link AlpineTerminalSessionActivityClient} if {@link AlpineActivity} has bound with
     * {@link AlpineService}, otherwise {@link AlpineTerminalSessionServiceClient}.
     */
    public synchronized AlpineTerminalSessionClientBase getAlpineTerminalSessionClient() {
        if (mAlpineTerminalSessionActivityClient != null)
            return mAlpineTerminalSessionActivityClient;
        else
            return mAlpineTerminalSessionServiceClient;
    }

    /** This should be called when {@link AlpineActivity#onServiceConnected} is called to set the
     * {@link AlpineService#mAlpineTerminalSessionActivityClient} variable and update the {@link TerminalSession}
     * and {@link TerminalEmulator} clients in case they were passed {@link AlpineTerminalSessionServiceClient}
     * earlier.
     *
     * @param alpineTerminalSessionActivityClient The {@link AlpineTerminalSessionActivityClient} object that fully
     * implements the {@link TerminalSessionClient} interface.
     */
    public synchronized void setAlpineTerminalSessionClient(AlpineTerminalSessionActivityClient alpineTerminalSessionActivityClient) {
        mAlpineTerminalSessionActivityClient = alpineTerminalSessionActivityClient;

        for (int i = 0; i < mShellManager.mAlpineSessions.size(); i++)
            mShellManager.mAlpineSessions.get(i).getTerminalSession().updateTerminalSessionClient(mAlpineTerminalSessionActivityClient);
    }

    /** This should be called when {@link AlpineActivity} has been destroyed and in {@link #onUnbind(Intent)}
     * so that the {@link AlpineService} and {@link TerminalSession} and {@link TerminalEmulator}
     * clients do not hold an activity references.
     */
    public synchronized void unsetAlpineTerminalSessionClient() {
        for (int i = 0; i < mShellManager.mAlpineSessions.size(); i++)
            mShellManager.mAlpineSessions.get(i).getTerminalSession().updateTerminalSessionClient(mAlpineTerminalSessionServiceClient);

        mAlpineTerminalSessionActivityClient = null;
    }





    private Notification buildNotification() {
        Resources res = getResources();

        // Set pending intent to be launched when notification is clicked
        Intent notificationIntent = AlpineActivity.newInstance(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);


        // Set notification text
        int sessionCount = getAlpineSessionsSize();
        int taskCount = mShellManager.mAlpineTasks.size();
        String notificationText = sessionCount + " session" + (sessionCount == 1 ? "" : "s");
        if (taskCount > 0) {
            notificationText += ", " + taskCount + " task" + (taskCount == 1 ? "" : "s");
        }

        final boolean wakeLockHeld = mWakeLock != null;
        if (wakeLockHeld) notificationText += " (wake lock held)";


        // Set notification priority
        // If holding a wake or wifi lock consider the notification of high priority since it's using power,
        // otherwise use a low priority
        int priority = (wakeLockHeld) ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW;


        // Build the notification
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(this,
            AlpineConstants.ALPINE_APP_NOTIFICATION_CHANNEL_ID, priority,
            AlpineConstants.ALPINE_APP_NAME, notificationText, null,
            contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.drawable.ic_service_notification);

        // Set background color for small notification icon
        builder.setColor(0xFF607D8B);

        // AlpineSessions are always ongoing
        builder.setOngoing(true);


        // Set Exit button action
        Intent exitIntent = new Intent(this, AlpineService.class).setAction(ALPINE_SERVICE.ACTION_STOP_SERVICE);
        builder.addAction(android.R.drawable.ic_delete, res.getString(R.string.notification_action_exit), PendingIntent.getService(this, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE));


        // Set Wakelock button actions
        String newWakeAction = wakeLockHeld ? ALPINE_SERVICE.ACTION_WAKE_UNLOCK : ALPINE_SERVICE.ACTION_WAKE_LOCK;
        Intent toggleWakeLockIntent = new Intent(this, AlpineService.class).setAction(newWakeAction);
        String actionTitle = res.getString(wakeLockHeld ? R.string.notification_action_wake_unlock : R.string.notification_action_wake_lock);
        int actionIcon = wakeLockHeld ? android.R.drawable.ic_lock_idle_lock : android.R.drawable.ic_lock_lock;
        builder.addAction(actionIcon, actionTitle, PendingIntent.getService(this, 0, toggleWakeLockIntent, PendingIntent.FLAG_IMMUTABLE));


        return builder.build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationUtils.setupNotificationChannel(this, AlpineConstants.ALPINE_APP_NOTIFICATION_CHANNEL_ID,
            AlpineConstants.ALPINE_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    /** Update the shown foreground service notification after making any changes that affect it. */
    private synchronized void updateNotification() {
        if (mWakeLock == null && mShellManager.mAlpineSessions.isEmpty() && mShellManager.mAlpineTasks.isEmpty()) {
            // Exit if we are updating after the user disabled all locks with no sessions or tasks running.
            requestStopService();
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(AlpineConstants.ALPINE_APP_NOTIFICATION_ID, buildNotification());
        }
    }





    private void setCurrentStoredTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return;
        // Make the newly created session the current one to be displayed
        AlpineAppSharedPreferences preferences = AlpineAppSharedPreferences.build(this);
        if (preferences == null) return;
        preferences.setCurrentSession(terminalSession.mHandle);
    }

    public synchronized boolean isAlpineSessionsEmpty() {
        return mShellManager.mAlpineSessions.isEmpty();
    }

    public synchronized int getAlpineSessionsSize() {
        return mShellManager.mAlpineSessions.size();
    }

    public synchronized List<AlpineSession> getAlpineSessions() {
        return mShellManager.mAlpineSessions;
    }

    @Nullable
    public synchronized AlpineSession getAlpineSession(int index) {
        if (index >= 0 && index < mShellManager.mAlpineSessions.size())
            return mShellManager.mAlpineSessions.get(index);
        else
            return null;
    }

    @Nullable
    public synchronized AlpineSession getAlpineSessionForTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return null;

        for (int i = 0; i < mShellManager.mAlpineSessions.size(); i++) {
            if (mShellManager.mAlpineSessions.get(i).getTerminalSession().equals(terminalSession))
                return mShellManager.mAlpineSessions.get(i);
        }

        return null;
    }

    public synchronized AlpineSession getLastAlpineSession() {
        return mShellManager.mAlpineSessions.isEmpty() ? null : mShellManager.mAlpineSessions.get(mShellManager.mAlpineSessions.size() - 1);
    }

    public synchronized int getIndexOfSession(TerminalSession terminalSession) {
        if (terminalSession == null) return -1;

        for (int i = 0; i < mShellManager.mAlpineSessions.size(); i++) {
            if (mShellManager.mAlpineSessions.get(i).getTerminalSession().equals(terminalSession))
                return i;
        }
        return -1;
    }

    public synchronized TerminalSession getTerminalSessionForHandle(String sessionHandle) {
        TerminalSession terminalSession;
        for (int i = 0, len = mShellManager.mAlpineSessions.size(); i < len; i++) {
            terminalSession = mShellManager.mAlpineSessions.get(i).getTerminalSession();
            if (terminalSession.mHandle.equals(sessionHandle))
                return terminalSession;
        }
        return null;
    }

    public synchronized AppShell getAlpineTaskForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        AppShell appShell;
        for (int i = 0, len = mShellManager.mAlpineTasks.size(); i < len; i++) {
            appShell = mShellManager.mAlpineTasks.get(i);
            String shellName = appShell.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return appShell;
        }
        return null;
    }

    public synchronized AlpineSession getAlpineSessionForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        AlpineSession alpineSession;
        for (int i = 0, len = mShellManager.mAlpineSessions.size(); i < len; i++) {
            alpineSession = mShellManager.mAlpineSessions.get(i);
            String shellName = alpineSession.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return alpineSession;
        }
        return null;
    }



    public boolean wantsToStop() {
        return mWantsToStop;
    }

}
