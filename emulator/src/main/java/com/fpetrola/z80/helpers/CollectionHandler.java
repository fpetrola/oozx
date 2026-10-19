/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.fpetrola.z80.helpers;

import java.util.List;

public class CollectionHandler<T> {
  protected ItemHandler<T> currentInvoker = new EmptyItemHandler<>();
  private boolean enabled = true;
  private ItemHandler<T> lastInvoker;
  /** the items now, kept in step with every change: see {@link #items()} */
  private Object[] items = new Object[0];

  public CollectionHandler() {
  }

  public void add(T item) {
    currentInvoker = currentInvoker.add(item);
    changed();
  }

  /**
   * The items, as an array that is replaced when they change and never changed itself: for a caller called millions of
   * times a second, which walks it with a plain loop instead of handing {@link #forAll} a lambda that captures its
   * arguments -- one object per call (a memory write's was a gigabyte of garbage a second in oozx's 3D viewer).
   */
  public Object[] items() {
    return items;
  }

  private void changed() {
    items = currentInvoker.getList().toArray();
  }

  public void forAll(ItemInvoker<T> invoker) {
    currentInvoker.execute(invoker);
  }

  public List<T> getList() {
    return currentInvoker.getList();
  }

  public void remove(T item) {
    currentInvoker = currentInvoker.remove(item);
    changed();
  }

  public void removeAll() {
    currentInvoker = new EmptyItemHandler<>();
    changed();
  }

  public void disable() {
    enabled = false;
    lastInvoker = currentInvoker;
    currentInvoker = new EmptyItemHandler<>();
    changed();
  }

  public void enable() {
    enabled = true;
    if (lastInvoker != null) {
      currentInvoker = lastInvoker;
      lastInvoker = null;
    }
    changed();
  }

  public boolean isEnabled() {
    return enabled;
  }
}