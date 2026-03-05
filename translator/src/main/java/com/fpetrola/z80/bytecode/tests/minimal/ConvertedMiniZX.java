package com.fpetrola.z80.bytecode.tests.minimal;

import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;

import java.util.function.Predicate;

public abstract class ConvertedMiniZX extends MiniZX {
  public ConvertedMiniZX(MiniZXIO miniZXIO, Predicate<Integer> interruptionCondition) {
    super(miniZXIO, interruptionCondition);
  }

  public void AF(int value) {
    AF = value;
  }

  public void BC(int value) {
    BC = value;
  }

  public void DE(int value) {
    DE = value;
  }

  public void HL(int value) {
    HL = value;
  }

  public void HL_8(int value) {
    H = value >> 8;
    L = value;
  }

  public void IX(int value) {
    IX = value;
  }

  public void IY(int value) {
    IY = value;
  }

  public void AFx(int value) {
    AFx = value;
  }

  public void BCx(int value) {
    BCx = value;
  }

  public void DEx(int value) {
    DEx = value;
  }

  public void HLx(int value) {
    HLx = value;
  }

  public int AF() {
    return AF;
  }

  public int BC() {
    return BC;
  }

  public int DE() {
    return DE;
  }

  public int HL() {
    return HL;
  }

  public int AFx() {
    return AFx;
  }

  public int BCx() {
    return BCx;
  }

  public int DEx() {
    return DEx;
  }

  public int HLx() {
    return HLx;
  }

  public int IX() {
    return IX;
  }

  public int IY() {
    return IY;
  }

  public int A_16() {
    return AF >> 8;
  }

  public int AF_8() {
    return A << 8 | F;
  }

  public int B_16() {
    return BC >> 8;
  }

  public int C_16() {
    return BC & 0xff;
  }

  public int BC_8() {
    return B << 8 | C;
  }

  public int BC_8_16() {
    return B << 8 | BC & 0xff;
  }

  public int D_16() {
    return DE >> 8;
  }

  public void D_16(int d) {
    DE = DE & 0xff | ((d) << 8);
  }

  public int E_16() {
    return DE & 0xff;
  }

  public void E_16(int e) {
    DE = DE & 0xff00 | e;
  }

  public int DE_8() {
    return D << 8 | E;
  }

  public void DE_8(int de) {
    D = de >> 8;
    E = de & 0xff;
  }

  public int DE_8_16() {
    return D << 8 | DE & 0xff;
  }

  public int H_16() {
    return HL >> 8;
  }

  public int L_16() {
    return HL & 0xff;
  }

  public int HL_8() {
    return H << 8 | L;
  }

  public int HL_8_16() {
    return H << 8 | HL & 0xff;
  }

  public int HL_16_8() {
    return (HL & 0xff00) | L;
  }

  public int A() {
    return A;
  }

  public void A(int a) {
    A = a;
  }

  public int F() {
    return F;
  }

  public void F(int f) {
    F = f;
  }

  public int B() {
    return B;
  }

  public void B(int b) {
    B = b;
  }

  public void B_16(int b) {
    BC = BC & 0xff | ((b & 0xff) << 8);
  }

  public int C() {
    return C;
  }

  public void C(int c) {
    C = c;
  }

  public void C_16(int c) {
    BC = BC & 0xff00 | c;
  }

  public int D() {
    return D;
  }

  public void D(int d) {
    D = d;
  }

  public int E() {
    return E;
  }

  public void E(int e) {
    E = e;
  }

  public int H() {
    return H;
  }

  public void H(int h) {
    H = h & 0xff;
  }

  public int L() {
    return L;
  }

  public void L(int l) {
    L = l;
  }

  public void L_16(int l) {
    HL = HL & 0xff00 | l;
  }

  public void H_16(int h) {
    HL = HL & 0xff | h << 8;
  }

  public int IXH() {
    return IXH;
  }

  public void IXH(int IXH) {
    this.IXH = IXH;
  }

  public int IXL() {
    return IXL;
  }

  public void IXL(int IXL) {
    this.IXL = IXL;
  }

  public int IYH() {
    return IYH;
  }

  public void IYH(int IYH) {
    this.IY = IYH;
  }

  public int IYL() {
    return IYL;
  }

  public void IYL(int IYL) {
    this.IY = IYL;
  }

  public int IXL_16() {
    return IX & 0xff;
  }

  public int IX_8() {
    return ((IXH & 0xff) << 8) | IXL & 0xff;
  }

  public int I() {
    return I;
  }

  public void I(int i) {
    I = i;
  }

  public int getR() {
    return R;
  }

  public void setR(int r) {
    R = r;
  }
}
