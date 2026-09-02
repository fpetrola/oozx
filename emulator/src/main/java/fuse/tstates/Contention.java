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

package fuse.tstates;

/**
 * A group of the cycles an instruction spends on the bus beyond its memory accesses - Fuse's
 * contend_read_no_mreq - said as a value: at one moment of the execution, {@code times} cycles
 * of {@code tstates} each, on an address the ULA may contend.
 */
public record Contention(Moment moment, int ordinal, Base base, int delta, int times, int tstates, Kind kind) {
  public static final int ANY = 0;

  public enum Moment {BEFORE_EXECUTION, AFTER_READ, BEFORE_WRITE, AFTER_EXECUTION, AFTER_EXECUTION_IF_JUMPED, AFTER_EXECUTION_IF_NOT_JUMPED}

  public enum Base {IR, PC, HL, BC, DE, SP, LAST_ACCESS}

  /** The bus cycle as Fuse names it; the ULA contends them all alike, the machine's tests tell them apart. */
  public enum Kind {
    READ("readbyte"), WRITE("writebyte"), READ_NO_MREQ("contend_read_no_mreq"), WRITE_NO_MREQ("contend_write_no_mreq");
    public final String description;

    Kind(String description) {
      this.description = description;
    }
  }

  /** Whether this applies at the {@code ordinal}-th read or write, counted from one. */
  public boolean at(Moment moment, int ordinal) {
    return this.moment == moment && (this.ordinal == ANY || this.ordinal == ordinal);
  }
}
