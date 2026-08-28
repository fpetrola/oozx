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

package com.fpetrola.oozx;

/**
 * Whether the emulator should keep running.
 * <p>
 * The flag is written from one thread and read from another: closing the window finishes the
 * session on the event dispatch thread, while the emulation loop reads it on its own. It is
 * volatile for that reason — without it the loop is free to hoist the read and never observe
 * the change, and the emulator keeps running after its window is gone.
 */
public class EmulationSession {

  private volatile boolean alive = true;

  public boolean isAlive() {
    return alive;
  }

  public void finish() {
    alive = false;
  }
}
