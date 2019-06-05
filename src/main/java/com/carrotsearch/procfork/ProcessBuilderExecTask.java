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

public class ProcessBuilderExecTask implements IExecTask {
  private String executable;
  private Path cwd;
  private List<String> args = new ArrayList<>();
  private Map<String, String> envVars = new LinkedHashMap<>();
  private boolean executeViaShell;

  @Override
  public IExecTask appendArgs(String... values) {
    for (String v : values) {
      appendArg(v);
    }
    return this;
  }

  @Override
  public IExecTask executable(Path executable) {
    return executable(executable.toAbsolutePath().normalize().toString());
  }

  @Override
  public IExecTask executable(String executable) {
    this.executable = executable;
    return this;
  }

  @Override
  public IExecTask executeViaShell() {
    this.executeViaShell = true;
    return this;
  }

  @Override
  public IExecTask currentWorkingDirectory(Path cwd) {
    this.cwd = cwd;
    return this;
  }

  @Override
  public IExecTask appendArg(String value) {
    args.add(value);
    return this;
  }

  @Override
  public IExecTask envVar(String key, String value) {
    assert key != null && value != null;

    envVars.put(key, value);
    return this;
  }

  @Override
  public ProcessHandler execute() throws IOException {
    ProcessBuilder pb = new ProcessBuilder();
    if (cwd != null) {
      pb.directory(cwd.toFile());
    }

    List<String> command = new ArrayList<>();
    if (executeViaShell) {
      if (OsDetection.IS_OS_WINDOWS) {
        command.addAll(shellInvokeWindows());
      } else if (OsDetection.IS_OS_UNIXISH) {
        command.addAll(shellInvokeUnixish());
      } else {
        throw new RuntimeException("Unsupported operating system: " + OsDetection.OS_NAME);
      }
    } else {
      command.add(executable);
    }

    command.addAll(args);
    pb.command(command);
    pb.redirectErrorStream(true); // merge stderr and stdout
    pb.environment().putAll(envVars);

    return new ProcessHandler(pb.start());
  }

  protected List<String> shellInvokeUnixish() {
    Path cmd = Paths.get("/bin/sh");
    if (cmd == null || !Files.isRegularFile(cmd)) {
      throw new RuntimeException("sh couldn't be found or is not a file: " + cmd);
    }

    return Arrays.asList(
        cmd.toString(),
        "-f", // don't glob.
        "-c",
        executable + " \"$@\"",
        executable);
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
      throw new RuntimeException("comspec (cmd.exe) couldn't be found or is not a file: " + cmd);
    }

    return Arrays.asList(cmd.toString(), "/c", executable);
  }
}
