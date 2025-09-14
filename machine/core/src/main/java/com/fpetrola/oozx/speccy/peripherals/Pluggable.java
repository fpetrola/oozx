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
package com.fpetrola.oozx.speccy.peripherals;

/**
 * A peripheral somebody plugs in from outside, by clipping its window onto the machine - as
 * against one a machine comes with, or one a setting asks for.
 */
public interface Pluggable {
  /** The cable. Takes effect at the next update, which is the emulator's own thread's business. */
  void plugIn(boolean connected);

  boolean isPluggedIn();
}
