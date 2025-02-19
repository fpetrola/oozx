package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.registers.RRegister;

/**
 * Restores the fetch-notification that {@code 0.0.2-alu}'s {@link RRegister#increment()}
 * dropped while overriding bit 7 handling: RZX frame/interrupt timing is driven by fetch
 * count, counted here off the R register's per-opcode increment. Without it the frame
 * counter never moves, so a recording never advances and the run hangs until OutOfMemory
 * with no compile-time signal. Fixed here rather than in alu, which stays an external
 * dependency; bit 7 handling is untouched.
 */
public class ObservableRRegister extends RRegister {

  @Override
  public void increment() {
    int before = data;
    super.increment();
    incrementing(before);
  }
}
