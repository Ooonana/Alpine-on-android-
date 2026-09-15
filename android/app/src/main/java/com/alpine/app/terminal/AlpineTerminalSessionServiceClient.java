package com.alpine.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.alpine.app.AlpineService;
import com.alpine.shared.alpine.shell.command.runner.terminal.AlpineSession;
import com.alpine.shared.alpine.terminal.AlpineTerminalSessionClientBase;
import com.alpine.terminal.TerminalSession;
import com.alpine.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class AlpineTerminalSessionServiceClient extends AlpineTerminalSessionClientBase {

    private static final String LOG_TAG = "AlpineTerminalSessionServiceClient";

    private final AlpineService mService;

    public AlpineTerminalSessionServiceClient(AlpineService service) {
        this.mService = service;
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        AlpineSession alpineSession = mService.getAlpineSessionForTerminalSession(terminalSession);
        if (alpineSession != null)
            alpineSession.getExecutionCommand().mPid = pid;
    }

}
