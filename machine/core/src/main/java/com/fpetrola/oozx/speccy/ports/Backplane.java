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

package com.fpetrola.oozx.speccy.ports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The handlers registry into the bus, in the order they answer. Counts its changes, so whoever
 * remembered something about them can tell it no longer holds without being told.
 */
public class Backplane {
  private final List<Wired> wired = new ArrayList<>();
  private int version;

  /**
   * @param inFront the first to attach wins the bits it drives, so something registry
   *                in by hand goes ahead of the machine's own chips: a board on the even ports
   *                the ULA answers has to be heard over the keyboard.
   */
  public void attach(Wired[] ports, boolean inFront) {
    for (int i = 0; i < ports.length; i++) {
      wired.add(inFront ? i : wired.size(), ports[i]);
    }
    version++;
  }

  public void detach(Wired[] ports) {
    wired.removeAll(Arrays.asList(ports));
    version++;
  }

  public void clear() {
    wired.clear();
    version++;
  }

  int version() {
    return version;
  }

  List<Wired> wired() {
    return wired;
  }
}
