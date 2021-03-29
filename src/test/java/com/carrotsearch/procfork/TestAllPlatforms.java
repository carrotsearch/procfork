/*
 * procfork
 *
 * Copyright (C) 2019, Dawid Weiss.
 * All rights reserved.
 */
package com.carrotsearch.procfork;

import com.carrotsearch.randomizedtesting.LifecycleScope;
import com.carrotsearch.randomizedtesting.MixWithSuiteName;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.*;
import com.google.common.base.Stopwatch;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import org.assertj.core.api.Assertions;
import org.junit.Test;

@TimeoutSuite(millis = 60 * 1000)
@ThreadLeakGroup(ThreadLeakGroup.Group.MAIN)
@ThreadLeakScope(ThreadLeakScope.Scope.TEST)
@ThreadLeakZombies(ThreadLeakZombies.Consequence.IGNORE_REMAINING_TESTS)
@ThreadLeakLingering(linger = 1000)
@ThreadLeakAction({ThreadLeakAction.Action.WARN, ThreadLeakAction.Action.INTERRUPT})
@SeedDecorators({MixWithSuiteName.class})
@ThreadLeakFilters(filters = CustomThreadFilters.class)
public class TestAllPlatforms extends RandomizedTest {
  @Test
  public void testSpaceInPathAndArgument() throws Exception {
    Path cwdDir = newTempDir(LifecycleScope.TEST).resolve("space (inside)");
    Files.createDirectories(cwdDir);
    Path cmdDir = newTempDir(LifecycleScope.TEST).resolve("New Folder (2)");
    Files.createDirectories(cmdDir);

    String expectedOutput;
    Path command;
    if (LocalEnvironment.IS_OS_WINDOWS) {
      command = cmdDir.resolve("script.cmd");
      Files.write(command, "@echo 1=%1 2=%2".getBytes(StandardCharsets.UTF_8));
      // Quotes added by cmd or echo - they're passed to subprocess properly.
      expectedOutput = "1=\"one and two\" 2=three";
    } else {
      command = cmdDir.resolve("script.sh");
      Files.write(command, "echo 1=$1 2=$2".getBytes(StandardCharsets.UTF_8));
      Files.setPosixFilePermissions(command, PosixFilePermissions.fromString("u+x"));
      expectedOutput = "1=one and two 2=three";
    }

    Path output;
    try (ForkedProcess cmd =
        new ProcessBuilderLauncher()
            .cwd(cwdDir)
            .executable(command.toAbsolutePath())
            .viaShellLauncher()
            .args("one and two", "three")
            .execute()) {

      int exitStatus = cmd.waitFor();
      output = cmd.getProcessOutputFile();
      String out =
          new String(Files.readAllBytes(cmd.getProcessOutputFile()), Charset.defaultCharset());
      Assertions.assertThat(out).isEqualToIgnoringWhitespace(expectedOutput);
      Assertions.assertThat(exitStatus).isEqualTo(0);
    }

    Assertions.assertThat(output).doesNotExist();
  }

  @Test
  public void testSpaceInPathAndNoArguments() throws Exception {
    Path cwdDir = newTempDir(LifecycleScope.TEST).resolve("space (inside)");
    Files.createDirectories(cwdDir);
    Path cmdDir = newTempDir(LifecycleScope.TEST).resolve("New Folder (2)");
    Files.createDirectories(cmdDir);

    String expectedOutput;
    Path command;
    if (LocalEnvironment.IS_OS_WINDOWS) {
      command = cmdDir.resolve("script.cmd");
      Files.write(command, "@echo args: %1".getBytes(StandardCharsets.UTF_8));
      expectedOutput = "args: ";
    } else {
      command = cmdDir.resolve("script.sh");
      Files.write(command, "echo args: $1".getBytes(StandardCharsets.UTF_8));
      Files.setPosixFilePermissions(command, PosixFilePermissions.fromString("u+x"));
      expectedOutput = "args: ";
    }

    Path output;
    try (ForkedProcess cmd =
        new ProcessBuilderLauncher()
            .cwd(cwdDir)
            .executable(command.toAbsolutePath())
            .viaShellLauncher()
            .execute()) {

      int exitStatus = cmd.waitFor();
      output = cmd.getProcessOutputFile();
      String out =
          new String(Files.readAllBytes(cmd.getProcessOutputFile()), Charset.defaultCharset());
      Assertions.assertThat(out).isEqualToIgnoringWhitespace(expectedOutput);
      Assertions.assertThat(exitStatus).isEqualTo(0);
    }

    Assertions.assertThat(output).doesNotExist();
  }

  @Test
  public void testEcho() throws Exception {
    Path output;
    try (ForkedProcess cmd =
        new ProcessBuilderLauncher()
            .cwd(Paths.get("."))
            .executable(Paths.get("echo"))
            .args("foo", "bar")
            .viaShellLauncher()
            .execute()) {

      Assertions.assertThat(cmd.waitFor()).isEqualTo(0);
      output = cmd.getProcessOutputFile();

      String out =
          new String(Files.readAllBytes(cmd.getProcessOutputFile()), Charset.defaultCharset());
      Assertions.assertThat(out).isEqualToIgnoringNewLines("foo bar");
    }

    Assertions.assertThat(output).doesNotExist();
  }

  @Test
  public void testTermination() throws Exception {
    Path output;
    try (ForkedProcess cmd =
        new ProcessBuilderLauncher()
            .cwd(Paths.get("."))
            .executable(Paths.get("ping"))
            .args(pingArgs(5))
            .viaShellLauncher()
            .execute()) {
      Thread.sleep(1000);
      cmd.destroyForcibly();
      output = cmd.getProcessOutputFile();
    }

    Assertions.assertThat(output).doesNotExist();
  }

  private String[] pingArgs(int secs) {
    if (LocalEnvironment.IS_OS_WINDOWS) {
      return new String[] {"localhost", "-n", Integer.toString(secs)};
    } else {
      return new String[] {"localhost", "-c", Integer.toString(secs)};
    }
  }

  @Test
  public void testOutputStreaming() throws Exception {
    try (ForkedProcess cmd =
        new ProcessBuilderLauncher()
            .cwd(Paths.get("."))
            .executable(Paths.get("ping"))
            .args(pingArgs(5))
            .viaShellLauncher()
            .execute()) {
      Stopwatch sw = Stopwatch.createStarted();
      ArrayList<String> lines = new ArrayList<>();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(cmd.getProcessOutputAsStream()))) {
        String line;
        while ((line = reader.readLine()) != null && sw.elapsed().getSeconds() < 1) {
          lines.add(line);
        }
      }

      Assertions.assertThat(lines).isNotEmpty();
      cmd.destroyForcibly();
    }
  }
}
