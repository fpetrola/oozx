/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

import java.util.Queue;

public class SimpleQueue<E> {
  int index = 0;
  int head = 0;
  int size = 100;
  volatile int counter = 0;
  E[] data;


  public SimpleQueue(int size) {
    this.size = size;
    data = (E[]) new Object[size];
  }

  public void add(E e) {
    data[index] = e;
    index = (index + 1) % size;
    counter++;
  }

  public E poll() {
    E value = data[head];
    head = (head + 1) % size;
    counter--;
    return value;
  }

  public boolean empty() {
    return counter == 0;
  }
}