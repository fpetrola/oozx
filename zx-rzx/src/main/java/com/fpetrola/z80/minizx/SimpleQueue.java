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
package com.fpetrola.z80.minizx;

/** A ring of a fixed size; clearing it costs nothing, which matters to whoever clears it every frame. */
public class SimpleQueue<E> {
  private final E[] data;
  private int index;
  private int head;
  private volatile int counter;

  public SimpleQueue(int size) {
    data = (E[]) new Object[size];
  }

  public void add(E e) {
    data[index] = e;
    index = (index + 1) % data.length;
    counter++;
  }

  public E poll() {
    E value = data[head];
    head = (head + 1) % data.length;
    counter--;
    return value;
  }

  public boolean isEmpty() {
    return counter == 0;
  }

  public void clear() {
    index = head = counter = 0;
  }
}
