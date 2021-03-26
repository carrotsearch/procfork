/*
 * procfork
 *
 * Copyright (C) 2019, Dawid Weiss.
 * All rights reserved.
 */
package com.carrotsearch.procfork;

import com.carrotsearch.randomizedtesting.ThreadFilter;

public class CustomThreadFilters implements ThreadFilter {
  @Override
  public boolean reject(Thread t) {
    return t.getName().startsWith("ForkJoinPool.");
  }
}
