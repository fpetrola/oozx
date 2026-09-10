package model.tests.regression;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.Processors;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.TimeUnit;

/**
 * How many instructions a second this emulator runs, on a program that is the same every frame.
 * <p>
 * Frames a second - which is what the app shows as a speed - only measures the emulator while the
 * program does the same work every frame, and a ROM booting does not: measured over twelve blocks
 * it climbs and falls by 70% with nothing changing but which part of the boot it is in. So the
 * machine runs a loop of its own in uncontended RAM, with interrupts off, and counts what it did:
 * the loop keeps its own iteration count at 0x9000, so instructions are that count times the
 * instructions in one turn, and nothing has to be counted from this side.
 */
@EnabledIfSystemProperty(named = "oozx.measure", matches = "true")
@Timeout(value = 20, unit = TimeUnit.MINUTES)
class InstructionsPerSecond extends MachineTest {
  private static final int PROGRAM = 0x8000, COUNTER = 0x9000, SCRATCH = 0x9100;
  private static final int INSTRUCTIONS_PER_TURN = 20;

  /** DI, then twenty instructions that touch registers, memory and the ALU, and back to the top. */
  private static final int[] TURN = {
      0xF3,                                     // DI
      0x2A, COUNTER & 0xff, COUNTER >> 8,       // LD HL,(0x9000)
      0x23,                                     // INC HL
      0x22, COUNTER & 0xff, COUNTER >> 8,       // LD (0x9000),HL
      0x01, 0x34, 0x12,                         // LD BC,0x1234
      0x11, 0x78, 0x56,                         // LD DE,0x5678
      0x78, 0x81, 0x82, 0xAB,                   // LD A,B / ADD A,C / ADD A,D / XOR E
      0xE6, 0x0F,                               // AND 0x0F
      0xCB, 0x27, 0xCB, 0x3F,                   // SLA A / SRL A
      0x21, SCRATCH & 0xff, SCRATCH >> 8,       // LD HL,0x9100
      0x36, 0x55,                               // LD (HL),0x55
      0x7E, 0x34, 0x35,                         // LD A,(HL) / INC (HL) / DEC (HL)
      0x0C, 0x0D,                               // INC C / DEC C
      0xC3, (PROGRAM + 1) & 0xff, (PROGRAM + 1) >> 8  // JP back, past the DI
  };

  @Test
  void instructionsASecond() {
    for (String core : new String[]{"Generated", "OOP"}) {
      Processors.startsOn = core;
      Speccy speccy = silentMachine();
      speccy.sound.output.enabled = false;
      speccy.picture.active = Boolean.getBoolean("oozx.picture");

      for (int i = 0; i < TURN.length; i++) speccy.memory.poke(PROGRAM + i, (byte) TURN[i]);
      speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(PROGRAM);

      System.out.printf("== %s, pantalla %s%n", speccy.processors.current(), speccy.picture.active);
      runFrames(speccy, 2000);

      // Cuantas vueltas entran en cien frames, que es poco para que el contador de dos bytes
      // de la vuelta. De ahi sale cuantas instrucciones tiene un bloque, que es siempre lo mismo
      // porque los frames son siempre los mismos y el programa tambien.
      long before = turns(speccy);
      runFrames(speccy, 100);
      long turnsPerHundred = turns(speccy) - before;
      long instructionsPerBlock = turnsPerHundred * 30 * INSTRUCTIONS_PER_TURN;
      System.out.printf("   %d vueltas cada cien frames, %d instrucciones por bloque%n",
          turnsPerHundred, instructionsPerBlock);

      for (int block = 0; block < 12; block++) {
        long start = System.nanoTime();
        runFrames(speccy, 3000);
        double seconds = (System.nanoTime() - start) / 1e9;
        System.out.printf("MIPS %.2f%n", instructionsPerBlock / seconds / 1e6);
      }
    }
    Processors.startsOn = null;
  }

  /** The loop's own count, which wraps at 65536 and is read as it grows. */
  private long turns(Speccy speccy) {
    return (speccy.memory.peek(COUNTER) & 0xff) | ((speccy.memory.peek(COUNTER + 1) & 0xff) << 8);
  }
}
