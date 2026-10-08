package com.fpetrola.z80.minizx;

import org.junit.Test;

import static org.junit.Assert.*;

public class SpectrumApplicationTest {
  public static class Game extends SpectrumApplication {
    public int called;

    public void $9000() {
      called++;
    }
  }

  private final Game game = new Game();

  private int execute(int... bytes) {
    for (int i = 0; i < bytes.length; i++)
      game.mem[0x8000 + i] = bytes[i];
    return game.executeMutantCode(0x8000);
  }

  @Test
  public void anOpcodeSwapRunsTheVersionInMemory() {
    game.D(5);
    game.E(7);
    assertEquals(0x8001, execute(0x14));
    assertEquals(6, game.D());
    assertEquals(0x8001, execute(0x1C));
    assertEquals(8, game.E());
  }

  @Test
  public void anImmediateLoadUsesTheRewrittenOperand() {
    assertEquals(0x8002, execute(0x06, 0x2A));
    assertEquals(0x2A, game.B());
  }

  @Test
  public void anAluSwapSetsTheFlags() {
    game.A(0x55);
    game.C(0x55);
    assertEquals(0x8001, execute(0xA9));
    assertEquals(0, game.A());
    assertTrue(game.flag(0x40, false));
    game.A(0x50);
    execute(0xB1);
    assertEquals(0x55, game.A());
    assertFalse(game.flag(0x40, false));
  }

  @Test
  public void aTakenReturnLeavesTheJavaMethodWithoutPopping() {
    // Monty on the Run AE02: the game patches RET Z over LD A,(AE26) to switch a routine off
    game.SP(0xF000);
    game.F(0x40);
    assertEquals(-1, execute(0xC8));
    assertEquals(0xF000, game.SP());
    game.F(0x00);
    assertEquals(0x8001, execute(0xC8));
  }

  @Test
  public void aConditionalJumpReturnsWhereItGoes() {
    game.F(0x01);
    assertEquals(0xCEFC, execute(0xDA, 0xFC, 0xCE));
    game.F(0x00);
    assertEquals(0x8003, execute(0xDA, 0xFC, 0xCE));
  }

  @Test
  public void relativeJumpsAndDjnzWork() {
    assertEquals(0x8007, execute(0x18, 0x05));
    game.B(2);
    assertEquals(0x7FF0, execute(0x10, 0xEE));
    assertEquals(1, game.B());
  }

  @Test
  public void bitAndSetOnIndexedMemoryUseTheRewrittenBitNumber() {
    game.IX(0xC000);
    game.mem[0xC000] = 0x10;
    assertEquals(0x8004, execute(0xDD, 0xCB, 0x00, 0x66));
    assertFalse(game.flag(0x40, false));
    execute(0xDD, 0xCB, 0x00, 0x5E);
    assertTrue(game.flag(0x40, false));
    execute(0xDD, 0xCB, 0x00, 0xDE);
    assertEquals(0x18, game.mem[0xC000]);
  }

  @Test
  public void aRewrittenCallInvokesTheTranslatedMethodWithoutTouchingTheStack() {
    game.SP(0xFF00);
    assertEquals(0x8003, execute(0xCD, 0x00, 0x90));
    assertEquals(1, game.called);
    assertEquals(0xFF00, game.SP());
    game.F(0x00);
    execute(0xDC, 0x00, 0x90);
    assertEquals(1, game.called);
  }

  @Test
  public void writesGoToTheGameMemoryEvenAfterItIsReplaced() {
    game.mem = new int[0x10000];
    game.A(0x77);
    game.DE(0x9000);
    execute(0x12);
    assertEquals(0x77, game.mem[0x9000]);
  }

  @Test
  public void theRefreshRegisterIsNotAdvancedTwice() {
    game.R(0x10);
    execute(0x14);
    assertEquals(0x10, game.R());
  }

  @Test
  public void loadingAFromRCopiesTheSecondInterruptFlagIntoParity() {
    game.ei();
    game.ldAR();
    assertTrue(game.flag(0x04, false));
    game.di();
    game.ldAR();
    assertFalse(game.flag(0x04, false));
  }
}
