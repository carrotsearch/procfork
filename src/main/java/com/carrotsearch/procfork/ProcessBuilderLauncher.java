/*
 * procfork
 *
 * Copyright (C) 2019, Dawid Weiss.
 * All rights reserved.
 */
package com.carrotsearch.procfork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProcessBuilderLauncher implements Launcher {
  private Path executable;
  private Path cwd;
  private List<String> args = new ArrayList<>();
  private Map<String, String> envVars = new LinkedHashMap<>();
  private boolean executeViaShell;

  private Path input;

  @Override
  public Launcher args(String... values) {
    for (String v : values) {
      arg(v);
    }
    return this;
  }

  @Override
  public Launcher arg(String value) {
    args.add(value);
    return this;
  }

  @Override
  public Launcher executable(Path executable) {
    this.executable = executable;
    return this;
  }

  @Override
  public Launcher viaShellLauncher() {
    this.executeViaShell = true;
    return this;
  }

  @Override
  public Launcher cwd(Path cwd) {
    this.cwd = cwd;
    return this;
  }

  @Override
  public Launcher envvar(String key, String value) {
    assert key != null && value != null;

    envVars.put(key, value);
    return this;
  }

  @Override
  public Launcher input(Path input) {
    this.input = input;
    return this;
  }

  @Override
  public ForkedProcess execute() throws IOException {
    ProcessBuilder pb = new ProcessBuilder();
    if (cwd != null) {
      pb.directory(cwd.toFile());
    }

    List<String> command = new ArrayList<>();
    if (executeViaShell) {
      if (LocalEnvironment.IS_OS_WINDOWS) {
        command.addAll(shellInvokeWindows());
      } else if (LocalEnvironment.IS_OS_UNIXISH) {
        command.addAll(shellInvokeUnixish());
      } else {
        throw new RuntimeException("Unsupported operating system: " + LocalEnvironment.OS_NAME);
      }
    } else {
      command.add(executableName());
    }

    command.addAll(args);
    pb.command(command);
    pb.environment().putAll(envVars);

    if (input != null) {
      pb.redirectInput(ProcessBuilder.Redirect.from(input.toFile()));
    } else {
      pb.redirectInput(ProcessBuilder.Redirect.PIPE);
    }

    // merge stderr and stdout
    pb.redirectErrorStream(true);

    Path output = Files.createTempFile("process-", ".out");
    pb.redirectOutput(ProcessBuilder.Redirect.to(output.toFile()));

    try {
      Process process = pb.start();

      // If there is no input, close the pipe early.
      if (input == null) {
        process.getInputStream().close();
      }

      return new ForkedProcess(process, output);
    } catch (IOException e) {
      try {
        Files.deleteIfExists(output);
      } catch (IOException e2) {
        e.addSuppressed(e2);
      }
      throw e;
    }
  }

  protected List<String> shellInvokeUnixish() {
    Path cmd = Paths.get("/bin/sh");
    if (cmd == null || !Files.isRegularFile(cmd)) {
      throw new RuntimeException("sh interpreter couldn't be found: " + cmd);
    }

    return Arrays.asList(
        cmd.toString(),
        "-f", // don't glob.
        "-c",
        executableName() + " \"$@\"",
        executable.getFileName().toString());
  }

  protected List<String> shellInvokeWindows() {
    Path cmd = null;
    for (Map.Entry<String, String> e : System.getenv().entrySet()) {
      if (e.getKey().toLowerCase(Locale.ROOT).equals("comspec")) {
        cmd = Paths.get(e.getValue());
        break;
      }
    }
    if (cmd == null || !Files.isRegularFile(cmd)) {
      throw new RuntimeException(
          "Command line interpreter couldn't be found or is not a file: " + cmd);
    }

    return Arrays.asList(cmd.toString(), "/c", executableName());
  }

  private String executableName() {
    return executable.toString();
  }
}
