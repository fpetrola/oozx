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
 * A ROM a machine needs is missing or is not the size it should be.
 * <p>
 * Its own type rather than a general failure because loading a bank catches it on purpose: a
 * model asks for the ROM the settings name and falls back to the one it shipped with, and the
 * catch has to be narrow enough that a real fault on the first attempt is not swallowed as a
 * reason to try the second.
 */
public class RomNotLoadedException extends RuntimeException {

  public RomNotLoadedException(String message) {
    super(message);
  }
}
