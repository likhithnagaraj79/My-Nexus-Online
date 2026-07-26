package com.exhibitorreg.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Seam over {@link ProcessBuilder} so tests can avoid really invoking pg_dump/pg_restore. */
public interface ProcessRunner {

    Process start(List<String> command, Map<String, String> extraEnv) throws IOException;
}
