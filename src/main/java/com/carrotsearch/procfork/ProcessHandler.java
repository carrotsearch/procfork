package com.carrotsearch.procfork;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class ProcessHandler {
  private final Process p;

  public ProcessHandler(Process p) {
    this.p = p;
  }

  public Process process() {
    return p;
  }

  public void destroy() {
    p.destroyForcibly();
  }

  public String pipeToString(Charset charset) throws IOException {
    return new String(pipeToBytes(), charset);
  }

  public byte[] pipeToBytes() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    pipeTo(baos);
    return baos.toByteArray();
  }

  public void pipeTo(OutputStream out) throws IOException {
    byte[] buffer = new byte[1024 * 4];
    try (OutputStream os = p.getOutputStream();
        InputStream stdout = p.getInputStream()) {
      os.close(); // close stdin early to prevent deadlocks.
      for (int len; (len = stdout.read(buffer)) >= 0; ) {
        out.write(buffer, 0, len);
      }
    }
  }
}
