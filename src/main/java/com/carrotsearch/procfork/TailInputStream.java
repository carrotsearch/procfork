package com.carrotsearch.procfork;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.function.Supplier;

/** An input stream that tails a file as new input appears there. */
public class TailInputStream extends InputStream {
  /** How long to sleep (millis) before checking for updates? */
  private static final long TAIL_CHECK_DELAY = 250;

  private final RandomAccessFile raf;
  private volatile boolean closed;

  /*
   * An external predicate indicating no more input will be appended and
   * EOF should be returned.
   */
  private final Supplier<Boolean> noMoreInput;

  public TailInputStream(Path file, Supplier<Boolean> noMoreInput) throws FileNotFoundException {
    this.raf = new RandomAccessFile(file.toFile(), "r");
    this.noMoreInput = noMoreInput;
  }

  @Override
  public int read() throws IOException {
    if (closed) return -1;

    try {
      int c;
      while ((c = raf.read()) == -1) {
        if (noMoreInput.get()) {
          return -1;
        }
        try {
          Thread.sleep(TAIL_CHECK_DELAY);
        } catch (InterruptedException e) {
          throw new IOException(e);
        }
      }
      return c;
    } catch (IOException e) {
      if (closed) return -1;
      else throw e;
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (closed) return -1;

    if (b == null) {
      throw new NullPointerException();
    } else if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }

    try {
      int rafRead = raf.read(b, off, len);
      if (rafRead == -1) {
        // If nothing in the buffer, wait.
        do {
          if (noMoreInput.get()) {
            return -1; // EOF;
          }

          try {
            Thread.sleep(TAIL_CHECK_DELAY);
          } catch (InterruptedException e) {
            throw new IOException(e);
          }
        } while ((rafRead = raf.read(b, off, len)) == -1);
      }
      return rafRead;
    } catch (IOException e) {
      if (closed) return -1;
      else throw e;
    }
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public void close() throws IOException {
    closed = true;
    this.raf.close();
  }
}
