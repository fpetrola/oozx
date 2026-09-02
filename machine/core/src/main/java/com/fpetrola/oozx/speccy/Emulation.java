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

package com.fpetrola.oozx.speccy;

/**
 * Whether a machine is being run rather than exercised.
 * <p>
 * A test drives the emulator by hand and wants none of what an emulator does for a person: no
 * sound card opened, no screen built, no history kept. The flag has always existed - it lived as a
 * static on the libretro entry point, so everything from the Z80 to the sound device asked a
 * launcher whether it was under test.
 */
public class Emulation {
  /** True when something is running this for a person to look at. Tests leave it false. */
  public static boolean noTest;
}
