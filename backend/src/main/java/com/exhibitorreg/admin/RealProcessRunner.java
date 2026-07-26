package com.exhibitorreg.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RealProcessRunner implements ProcessRunner {

    @Override
    public Process start(List<String> command, Map<String, String> extraEnv) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().putAll(extraEnv);
        // Only stdout is read by callers (the dump content); stderr is inherited to the server's
        // own logs so a slow/large stderr stream can never deadlock the stdout pipe.
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return builder.start();
    }
}
