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

package com.fpetrola.oozx.speccy.modules.scheduler;

/**
 * Something the machine has to do at a T-state. Whoever owns it registers it once with the
 * scheduler and asks for it as often as it is wanted; when it is for is the scheduler's business.
 */
public abstract class Task {
  /** What it is called: the name of its class, so it cannot drift from what it does. */
  public String name() {
    return getClass().getSimpleName();
  }

  /** Told the T-state it was due at: a task that repeats counts the next one from there. */
  public abstract void run(long due);
}
