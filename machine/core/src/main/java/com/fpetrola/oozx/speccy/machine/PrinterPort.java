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
package com.fpetrola.oozx.speccy.machine;

import java.util.function.Consumer;

/**
 * A machine with a Centronics port on its back - the +2A and the +3 - whose strobe line is a bit
 * of one of its own ports. The printer that is plugged in asks to hear that line.
 */
public interface PrinterPort extends SpectrumMachine {
  /** Who hears the strobe from now on; null unplugs the printer. */
  void onStrobe(Consumer<Boolean> printer);
}
