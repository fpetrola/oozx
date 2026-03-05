package com.fpetrola.z80.bytecode.tests.minimal;

import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.StackException;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.function.Predicate;

public class JSWTest1 {

  private int DE;
  private int BC;
  private int HL;

  public void $1000 (){
    DE= BC;
    DE++;
  }

  public void $2000 (){
    BC= 0x1234;
    $1000();
    HL= DE;
  }

  public void $3000 (){
    BC= 0x1234;
  }
}