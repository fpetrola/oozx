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

  public CollectionHandler() {
  }

  public void add(T item) {
    currentInvoker = currentInvoker.add(item);
  }

  public void forAll(ItemInvoker<T> invoker) {
    currentInvoker.execute(invoker);
  }

  public List<T> getList() {
    return currentInvoker.getList();
  }

  public void remove(T item) {
    currentInvoker = currentInvoker.remove(item);
  }

  public void removeAll() {
    currentInvoker = new EmptyItemHandler<>();
  }

  public void disable() {
    enabled = false;
    lastInvoker = currentInvoker;
    currentInvoker = new EmptyItemHandler<>();
  }

  public void enable() {
    enabled = true;
    if (lastInvoker != null) {
      currentInvoker = lastInvoker;
      lastInvoker = null;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }
}