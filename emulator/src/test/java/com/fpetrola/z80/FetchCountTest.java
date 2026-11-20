package com.fpetrola.z80;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FetchCountTest {

  private static OOZ80 running(int... bytes) {
    OOZ80 cpu = ProcessorUnderTest.found().processor(null);
    for (int i = 0; i < bytes.length; i++)
      cpu.getState().getMemory().write(0x8000 + i, bytes[i]);
    cpu.getState().getPc().write(0x8000);
    return cpu;
  }

  private static void assertRan(long instructions, long fetches, long ran) {
    assertEquals(instructions, ran >>> 32);
    assertEquals(fetches, (int) ran);
  }

  @Test
  void stopsAtTheFirstInstructionThatReachesTheBudget() {
    OOZ80 cpu = running(0, 0, 0, 0, 0);
    assertRan(3, 3, cpu.runFetches(3, cpu::execute));
    assertEquals(0x8003, cpu.getState().getPc().read());
  }

  @Test
  void aPrefixedInstructionIsTwoFetchesAndMayOvershoot() {
    OOZ80 cpu = running(0xDD, 0x21, 0x34, 0x12, 0);
    assertRan(1, 2, cpu.runFetches(1, cpu::execute));
  }

  @Test
  void ldRAWritesRAndStillCountsItsTwoFetches() {
    OOZ80 cpu = running(0xED, 0x4F, 0);
    cpu.getState().getRegister(RegisterName.A).write(0x55);
    assertRan(1, 2, cpu.runFetches(1, cpu::execute));
    assertEquals(0x55, cpu.getState().getRegisterR().read() & 0x7F);
  }
}
