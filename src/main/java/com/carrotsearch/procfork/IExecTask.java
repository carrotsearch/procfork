package com.carrotsearch.procfork;

import java.io.IOException;
import java.nio.file.Path;

/**
 * An abstraction layer over the functionality required to fork a subprocess and collect its output.
 */
public interface IExecTask {
  IExecTask executable(Path executable);
  IExecTask executable(String executable);

  IExecTask currentWorkingDirectory(Path cwd);

  IExecTask executeViaShell();

  IExecTask appendArg(String value);

  IExecTask appendArgs(String... values);

  IExecTask envVar(String key, String value);

  /** Execute and return the resulting process handle/ wrapper. */
  ProcessHandler execute() throws IOException;
}
