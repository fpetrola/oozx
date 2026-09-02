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

package com.fpetrola.oozx.speccy.devices.printer;

import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

/**
 * The one port a ZX Printer has, at two decodings: a Sinclair machine only checks that bit 2 is
 * low, a Timex checks the whole byte for 0xFB.
 */
class ZxPrinterPortHandler extends DefaultPortHandler {
  private final ZxPrinter printer;

  ZxPrinterPortHandler(int mask, int value, ZxPrinter printer) {
    super(mask, value, true, true);
    this.printer = printer;
  }

  public byte read(int port, byte[] attached) {
    attached[0] = (byte) 0xff;
    return printer.read();
  }

  public void write(int port, byte value) {
    printer.write(value);
  }
}
