/*
 * procfork
 *
 * Copyright (C) 2019, Dawid Weiss.
 * All rights reserved.
 */
package com.carrotsearch.procfork;

import java.io.IOException;
import java.nio.file.Path;

/**
 * An abstraction layer over the functionality required to fork a subprocess and collect its output.
 */
public interface Launcher {
  Launcher executable(Path executable);

  Launcher cwd(Path cwd);

  Launcher viaShellLauncher();

  Launcher arg(String value);

  Launcher args(String... values);

  Launcher envvar(String key, String value);

  Launcher input(Path input);

  /** Execute and return the resulting process handle/ wrapper. */
  ForkedProcess execute() throws IOException;
}
