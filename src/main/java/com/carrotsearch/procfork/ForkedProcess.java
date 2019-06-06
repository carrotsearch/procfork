/*
 * procfork
 *
 * Copyright (C) 2019, Dawid Weiss.
 * All rights reserved.
 */
package com.carrotsearch.procfork;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ForkedProcess implements Closeable {
  private final Process p;
  private final Path processOutput;

  public ForkedProcess(Process p, Path processOutput) {
    this.p = p;
    this.processOutput = processOutput;
  }

  public Process process() {
    return p;
  }

  public void destroyForcibly() {
    ArrayList<ProcessHandle> procList =
        p.descendants().collect(Collectors.toCollection(ArrayList::new));
    procList.add(p.toHandle());

    for (ProcessHandle ph : procList) {
      if (ph.isAlive()) {
        ph.destroyForcibly();
      }
    }
  }

  public Path processOutput() {
    return processOutput;
  }

  public InputStream processOutputAsStream() throws IOException {
    return new TailInputStream(processOutput, () -> !p.isAlive());
  }

  public int waitFor() throws InterruptedException {
    return process().waitFor();
  }

  @Override
  public void close() throws IOException {
    destroyForcibly();

    try {
      p.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }

    Files.deleteIfExists(processOutput);
  }
}
