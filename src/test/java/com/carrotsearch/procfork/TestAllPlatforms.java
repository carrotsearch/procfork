/*
 * procfork
 *
 * Copyright (C) 2019, Dawid Weiss.
 * All rights reserved.
 */
package com.carrotsearch.procfork;

import com.carrotsearch.randomizedtesting.MixWithSuiteName;
import com.carrotsearch.randomizedtesting.annotations.*;
import com.google.common.base.Stopwatch;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class TestAllPlatforms {
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
      output = cmd.processOutput();

      String out = new String(Files.readAllBytes(cmd.processOutput()), Charset.defaultCharset());
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
      output = cmd.processOutput();
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
          new BufferedReader(new InputStreamReader(cmd.processOutputAsStream()))) {
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
