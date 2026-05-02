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
package com.fpetrola.oozx.speccy.devices.parallelprinter;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.List;

/**
 * The printer as a machine sees it. A +2A or +3 has the port on its back: bit 0 reads BUSY,
 * low for a printer that is never busy, and a write latches the data; the strobe is bit 4 of the
 * +3's own paging port, which the machine hands over. A +D or a DISCiPLE brings its own port and
 * hands the printer the bytes itself, so on those machines this has no ports and is only asked
 * whether it is there.
 */
@com.google.inject.Singleton
public class ParallelPrinterPeripheral extends PluggablePeripheral {

  private final ParallelPrinter printer;

  @com.google.inject.Inject
  public ParallelPrinterPeripheral(ParallelPrinter printer) {
    super(List.of());
    this.printer = printer;
    ports(Wired.at(0xf002, 0x0000, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(0xfe);
      }

      public void write(int port, byte value) {
        printer.write(value);
      }
    }), Wired.at(0xf002, 0x1000, new DefaultPortHandler(false, true) {
      /** The strobe is bit 4 of the +3's own paging port, next to the disk motor. */
      public void write(int port, byte value) {
        printer.strobe((value & 0x10) != 0);
      }
    }));
  }

  public ParallelPrinter printer() {
    return printer;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.pagesThrough1ffd();
  }
}
