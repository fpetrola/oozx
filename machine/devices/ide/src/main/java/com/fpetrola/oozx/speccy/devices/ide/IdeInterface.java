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
package com.fpetrola.oozx.speccy.devices.ide;

import java.io.File;
import java.io.IOException;

/**
 * A mass storage interface as the desk sees it: some slots that take images, and a line about
 * the rest of its state - which memory it has paged where - for the window to show. The IDE
 * boards and the MMC ones differ in what is in the slot, and that is the slot's business.
 */
public interface IdeInterface {
  int units();

  MassStorage drive(int unit);

  void insert(int unit, File image) throws IOException;

  void eject(int unit);

  /** Whether its memory is over the machine's just now, for a lamp; false for a board without any. */
  boolean isPaged();

  /** Its registers, in words, or an empty string. */
  String status();
}
