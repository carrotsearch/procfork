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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ForkedProcess implements Closeable {
  private final Process p;
  private final Path processOutput;

  public ForkedProcess(Process p, Path processOutput) {
    this.p = p;
    this.processOutput = processOutput;
  }

  public Process getProcess() {
    return p;
  }

  public List<ProcessHandle> destroyForcibly() {
    ArrayList<ProcessHandle> procList = getProcessHandles();

    for (ProcessHandle ph : procList) {
      if (ph.isAlive()) {
        ph.destroyForcibly();
      }
    }

    return procList;
  }

  public Path getProcessOutputFile() {
    return processOutput;
  }

  public InputStream getProcessOutputAsStream() throws IOException {
    return new TailInputStream(processOutput, () -> !p.isAlive());
  }

  public int waitFor() throws InterruptedException {
    return getProcess().waitFor();
  }

  @Override
  public void close() throws IOException {
    List<ProcessHandle> handles = destroyForcibly();

    try {
      for (ProcessHandle handle : handles) {
        handle.onExit().get();
      }
    } catch (ExecutionException | InterruptedException e) {
      throw new IOException(e);
    }

    Files.deleteIfExists(processOutput);
  }

  private ArrayList<ProcessHandle> getProcessHandles() {
    ArrayList<ProcessHandle> procList =
        p.descendants().collect(Collectors.toCollection(ArrayList::new));
    procList.add(p.toHandle());
    return procList;
  }
}
