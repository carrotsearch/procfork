package com.carrotsearch.procfork;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.junit.Test;

public class TestWindows {
  @Test
  public void testMe() throws Exception {
    ProcessHandler cmd =
        new ProcessBuilderExecTask()
            .currentWorkingDirectory(Paths.get("."))
            .executable("ping")
            .appendArgs("127.0.0.1", "-n", "20")
            .executeViaShell()
            .execute();

    new Thread(
            () -> {
              try {
                Thread.sleep(3000);
                cmd.process().descendants().forEach(ProcessHandle::destroyForcibly);
                cmd.process().toHandle().destroy();
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();

    String s = cmd.pipeToString(StandardCharsets.UTF_8);
    System.out.println(s);

    System.out.println(cmd.process().isAlive());
  }
}
