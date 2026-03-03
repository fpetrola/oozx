package com.fpetrola.z80.bytecode.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fpetrola.z80.bytecode.tests.aa.Z80Registers;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simplified field access analyzer.
 * Stores:
 * - fieldLastWritePath: last path where each field was written
 * - methodFieldDeps: parameters and returns for each method
 */
public class JetSetWilly2FieldAccessAnalyzer3 extends JetSetWilly2 {
  Z80Registers z80Registers= new Z80Registers();

  public JetSetWilly2FieldAccessAnalyzer3(MiniZXIO<WordNumber> rzxPlayerIO,
                                          Predicate<Integer> interruptionCondition) {
    super(rzxPlayerIO, interruptionCondition);
  }

  public JetSetWilly2FieldAccessAnalyzer3() {
    super();
  }


  public void $34463() {
    super.$34463();
  }

  @Override
  public void pc(int address, int rdelta) {
    super.pc(address, rdelta);
    z80Registers.pc(address);
  }

  @Override
  public void AF(int val) {
    z80Registers.AF(val);
    super.AF(val);
  }

  @Override
  public int AF() {
    z80Registers.AF();
    return super.AF();

  }

  @Override
  public void A(int val) {
    z80Registers.A(val);
    super.A(val);
  }


  @Override
  public int A() {
    z80Registers.A();
    return super.A();

  }

  @Override
  public void F(int val) {
    z80Registers.F(val);
    super.F(val);
  }

  @Override
  public int F() {
    z80Registers.F();
    return super.F();

  }

  @Override
  public void BC(int val) {
    z80Registers.BC(val);
    super.BC(val);
  }

  @Override
  public int BC() {
    z80Registers.BC();
    return super.BC();

  }

  @Override
  public void B(int val) {
    z80Registers.B(val);
    super.B(val);
  }

  @Override
  public int B() {
    z80Registers.B();
    return super.B();

  }

  @Override
  public void C(int val) {
    z80Registers.C(val);
    super.C(val);
  }

  @Override
  public int C() {
    z80Registers.C();
    return super.C();

  }

  @Override
  public void DE(int val) {
    z80Registers.DE(val);
    super.DE(val);
  }

  @Override
  public int DE() {
    z80Registers.DE();
    return super.DE();

  }

  @Override
  public void D(int val) {
    z80Registers.D(val);
    super.D(val);
  }

  @Override
  public int D() {
    z80Registers.D();
    return super.D();

  }

  @Override
  public void E(int val) {
    z80Registers.E(val);
    super.E(val);
  }

  @Override
  public int E() {
    z80Registers.E();
    return super.E();

  }

  @Override
  public void HL(int val) {
    z80Registers.HL(val);
    super.HL(val);
  }

  @Override
  public int HL() {
    z80Registers.HL();
    return super.HL();

  }

  @Override
  public void H(int val) {
    z80Registers.H(val);
    super.H(val);
  }

  @Override
  public int H() {
    z80Registers.H();
    return super.H();

  }

  @Override
  public void L(int val) {
    z80Registers.L(val);
    super.L(val);
  }

  @Override
  public int L() {
    z80Registers.L();
    return super.L();

  }

  @Override
  public void IX(int val) {
    z80Registers.IX(val);
    super.IX(val);
  }

  @Override
  public int IX() {
    z80Registers.IX();
    return super.IX();

  }

  @Override
  public void IXH(int val) {
    z80Registers.IXH(val);
    super.IXH(val);
  }

  @Override
  public int IXH() {
    z80Registers.IXH();
    return super.IXH();

  }

  @Override
  public void IXL(int val) {
    z80Registers.IXL(val);
    super.IXL(val);
  }

  @Override
  public int IXL() {
    z80Registers.IXL();
    return super.IXL();

  }

  @Override
  public void IY(int val) {
    z80Registers.IY(val);
    super.IY(val);
  }

  @Override
  public int IY() {
    z80Registers.IY();
    return super.IY();

  }

  @Override
  public void IYH(int val) {
    z80Registers.IYH(val);
    super.IYH(val);
  }

  @Override
  public int IYH() {
    z80Registers.IYH();
    return super.IYH();

  }

  @Override
  public void IYL(int val) {
    z80Registers.IYL(val);
    super.IYL(val);
  }

  @Override
  public int IYL() {
    z80Registers.IYL();
    return super.IYL();

  }


}
