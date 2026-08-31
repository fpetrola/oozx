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

package com.fpetrola.oozx.speccy.modules;

public interface ZxModule {
  /** Brings the module up. The context parameter it used to take was never read by anyone. */
  void start();

  void end();

  /**
   * The machine was reset; put this module back to where it starts.
   * <p>
   * A notification and not an order - it is said to every module after the machine has reset
   * itself, and most have nothing to do about it, which is why there is a default. Fuse says the
   * same thing to its modules and twenty-eight of thirty-six listen.
   */
  default void machineWasReset(boolean hard) {
  }
}
