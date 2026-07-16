/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.z80.analysis;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

import java.util.*;

/**
 * The static footprint of ONE decoded Z80 instruction — the same information the
 * Java-translation pipeline derives per site by parsing the decompiled source with Spoon,
 * obtained here directly from the opcode's addressing modes. For each instruction it yields:
 * <ul>
 *   <li><b>reads/writes</b>: the {@link Tracer} register slots the instruction consumes and
 *       produces (F included), driving the runtime provenance edges;</li>
 *   <li><b>roles</b>: which role each read channel plays at this site — a register composing
 *       a memory operand's address is ADDR, a summand/operand is VAL, a tested flag or loop
 *       counter is COND — the discriminator every detector relies on;</li>
 *   <li><b>kind + equation</b>: BRANCH/INSTR plus the normalized one-line semantics in the
 *       SAME dialect the detectors probe ({@code mem[HL] = A}, {@code cp(A, 32)},
 *       {@code A = A & 15}, {@code HL = add16(HL, BC)}, {@code if (flagZ(F))} ...).</li>
 * </ul>
 * The equation needs immediate operand values, which are only fetchable once the instruction
 * has executed — so structure ({@link #of}) and equation ({@link #equation}) build in two
 * phases. Memory traffic itself is NOT modeled here: it arrives through the memory listeners
 * at runtime. Stack-machinery instructions (push/pop/call/ret) are flagged so their memory
 * traffic can be suppressed, mirroring the Java translation where they become native
 * constructs with no observable memory ops.
 */
class Z80OpcodeInfo {
  final List<Integer> reads = new ArrayList<>();   // Tracer slots read (in order)
  final List<Integer> writes = new ArrayList<>();  // Tracer slots written
  final Map<String, String> roles = new LinkedHashMap<>(); // channel -> role letters (e.g. "A", "AV")
  String kind = "INSTR";
  String equation;          // filled after first execution (needs immediates)
  boolean bulk;             // LDIR family: emit Tracer.bulk, suppress per-byte traffic
  boolean bulkBackward;     // LDDR/LDD: HL/DE point at the range END
  boolean cpBlock;          // CPIR family: coarse read at HL
  boolean suppressMem;      // stack machinery: memory traffic not attributable to the game
  boolean push, pop, ioIn;

  private static final Map<String, int[]> SLOTS = new HashMap<>();

  static {
    SLOTS.put("A", new int[]{Tracer.R_A});
    SLOTS.put("F", new int[]{Tracer.R_F});
    SLOTS.put("B", new int[]{Tracer.R_B});
    SLOTS.put("C", new int[]{Tracer.R_C});
    SLOTS.put("D", new int[]{Tracer.R_D});
    SLOTS.put("E", new int[]{Tracer.R_E});
    SLOTS.put("H", new int[]{Tracer.R_H});
    SLOTS.put("L", new int[]{Tracer.R_L});
    SLOTS.put("IXH", new int[]{Tracer.R_IXH});
    SLOTS.put("IXL", new int[]{Tracer.R_IXL});
    SLOTS.put("IYH", new int[]{Tracer.R_IYH});
    SLOTS.put("IYL", new int[]{Tracer.R_IYL});
    SLOTS.put("SP", new int[]{Tracer.R_SP});
    SLOTS.put("I", new int[]{Tracer.R_I});
    SLOTS.put("R", new int[]{Tracer.R_R});
    SLOTS.put("AF", new int[]{Tracer.R_A, Tracer.R_F});
    SLOTS.put("BC", new int[]{Tracer.R_B, Tracer.R_C});
    SLOTS.put("DE", new int[]{Tracer.R_D, Tracer.R_E});
    SLOTS.put("HL", new int[]{Tracer.R_H, Tracer.R_L});
    SLOTS.put("IX", new int[]{Tracer.R_IXH, Tracer.R_IXL});
    SLOTS.put("IY", new int[]{Tracer.R_IYH, Tracer.R_IYL});
    SLOTS.put("AFx", new int[]{Tracer.R_AX, Tracer.R_FX});
    SLOTS.put("BCx", new int[]{Tracer.R_BX, Tracer.R_CX});
    SLOTS.put("DEx", new int[]{Tracer.R_DX, Tracer.R_EX});
    SLOTS.put("HLx", new int[]{Tracer.R_HX, Tracer.R_LX});
  }

  private static int[] slots(Register<?> r) {
    return SLOTS.getOrDefault(r.getName(), new int[0]);
  }

  // ---------- structural analysis (phase 1: no memory access needed) ----------

  static Z80OpcodeInfo of(Instruction<?> instr) {
    Z80OpcodeInfo o = new Z80OpcodeInfo();
    o.analyze(instr);
    return o;
  }

  private void analyze(Instruction<?> instr) {
    if (instr instanceof Ldir || instr instanceof Lddr || instr instanceof Ldi || instr instanceof Ldd) {
      bulk = true;
      bulkBackward = instr instanceof Lddr || instr instanceof Ldd;
      suppressMem = true;
      readPair("HL", 'A');
      readPair("DE", 'A');
      readPair("BC", 'C');
      addRole("MEM", 'V');
      writePair("HL");
      writePair("DE");
      writePair("BC");
      write("F");
      return;
    }
    if (instr instanceof Cpir || instr instanceof Cpdr || instr instanceof Cpi || instr instanceof Cpd) {
      cpBlock = true;
      suppressMem = true;
      read("A", 'V');
      readPair("HL", 'A');
      readPair("BC", 'C');
      addRole("MEM", 'V');
      writePair("HL");
      writePair("BC");
      write("F");
      return;
    }
    if (instr instanceof Inir || instr instanceof Indr || instr instanceof Ini || instr instanceof Ind
        || instr instanceof Outir || instr instanceof Outdr || instr instanceof Outi || instr instanceof Outd) {
      ioIn = instr instanceof Inir || instr instanceof Indr || instr instanceof Ini || instr instanceof Ind;
      readPair("HL", 'A');
      readPair("BC", 'C');
      writePair("HL");
      write("B");
      write("F");
      return;
    }
    if (instr instanceof Push p) {
      push = true;
      suppressMem = true;
      readRef(p.getTarget(), 'V');
      read("SP", 'A');
      write("SP");
      return;
    }
    if (instr instanceof Pop p) {
      pop = true;
      suppressMem = true;
      read("SP", 'A');
      writeRef(p.getTarget());
      write("SP");
      return;
    }
    if (instr instanceof Call c) {
      suppressMem = true; // the return-address push is call machinery, not game data
      condition(c.getCondition());
      return;
    }
    if (instr instanceof RST) {
      suppressMem = true;
      return;
    }
    if (instr instanceof Ret r) {
      suppressMem = true;
      condition(r.getCondition());
      return;
    }
    if (instr instanceof DJNZ) {
      kind = "BRANCH";
      read("B", 'C');
      write("B");
      return;
    }
    if (instr instanceof JP<?> j) {
      jumpOperand(j.getPositionOpcodeReference());
      condition(j.getCondition());
      return;
    }
    if (instr instanceof JR<?> j) {
      condition(j.getCondition());
      return;
    }
    if (instr instanceof In<?> in) {
      ioIn = true;
      readRef(in.getSource(), 'A'); // the port
      writeRef(in.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Out<?> out) {
      readRef(out.getSource(), 'V');
      readRef(out.getTarget(), 'A'); // the port
      return;
    }
    if (instr instanceof Ex<?> ex) {
      // EX (SP),HL touches stack memory as machinery; EX DE,HL / EX AF,AF' are pure swaps
      if (ex.getTarget() instanceof IndirectMemory16BitReference)
        suppressMem = true;
      readRef(ex.getTarget(), 'V');
      readRef(ex.getSource(), 'V');
      writeRef(ex.getTarget());
      writeRef(ex.getSource());
      return;
    }
    if (instr instanceof Exx) {
      for (String p : new String[]{"BC", "DE", "HL", "BCx", "DEx", "HLx"}) {
        readPair(p, 'V');
        writePair(p);
      }
      return;
    }
    if (instr instanceof LdAI) {
      read("I", 'V');
      write("A");
      write("F");
      return;
    }
    if (instr instanceof LdAR) {
      read("R", 'V');
      write("A");
      write("F");
      return;
    }
    if (instr instanceof Ld<?> ld) {
      readRef(ld.getSource(), 'V');
      addrReads(ld.getTarget());
      writeRef(ld.getTarget());
      return;
    }
    if (instr instanceof Cp<?> cp) {
      read("A", 'V');
      readRef(cp.getSource(), 'V');
      write("F");
      return;
    }
    if (instr instanceof BIT<?> b) {
      readRef(b.getTarget(), 'V');
      write("F");
      return;
    }
    if (instr instanceof SET<?> s) {
      rmw(s.getTarget());
      return;
    }
    if (instr instanceof RES<?> r) {
      rmw(r.getTarget());
      return;
    }
    if (instr instanceof Add16<?> a) {
      readRef(a.getTarget(), 'V');
      readRef(a.getSource(), 'V');
      writeRef(a.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Adc16<?> a) {
      read("F", 'V');
      readRef(a.getTarget(), 'V');
      readRef(a.getSource(), 'V');
      writeRef(a.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Sbc16<?> a) {
      read("F", 'V');
      readRef(a.getTarget(), 'V');
      readRef(a.getSource(), 'V');
      writeRef(a.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Inc16<?> i) {
      readRef(i.getTarget(), 'V');
      writeRef(i.getTarget());
      return;
    }
    if (instr instanceof Dec16<?> d) {
      readRef(d.getTarget(), 'V');
      writeRef(d.getTarget());
      return;
    }
    if (instr instanceof Adc<?> || instr instanceof Sbc<?>) {
      read("F", 'V');
      TargetSourceInstruction<?, ?> a = (TargetSourceInstruction<?, ?>) instr;
      readRef(a.getTarget(), 'V');
      readRef(a.getSource(), 'V');
      writeRef(a.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Add<?> || instr instanceof Sub<?> || instr instanceof And<?>
        || instr instanceof Or<?> || instr instanceof Xor<?>) {
      TargetSourceInstruction<?, ?> a = (TargetSourceInstruction<?, ?>) instr;
      readRef(a.getTarget(), 'V');
      readRef(a.getSource(), 'V');
      writeRef(a.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Inc<?> i) {
      rmw(i.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Dec<?> d) {
      rmw(d.getTarget());
      write("F");
      return;
    }
    if (instr instanceof Neg || instr instanceof CPL || instr instanceof DAA) {
      read("A", 'V');
      if (instr instanceof DAA)
        read("F", 'V');
      write("A");
      write("F");
      return;
    }
    if (instr instanceof RLA || instr instanceof RRA) {
      read("A", 'V');
      read("F", 'V'); // carry rotates through
      write("A");
      write("F");
      return;
    }
    if (instr instanceof RLCA || instr instanceof RRCA) {
      read("A", 'V');
      write("A");
      write("F");
      return;
    }
    if (instr instanceof RL<?> || instr instanceof RR<?>) {
      read("F", 'V');
      rmw(((TargetInstruction<?>) instr).getTarget());
      write("F");
      return;
    }
    if (instr instanceof RLC<?> || instr instanceof RRC<?> || instr instanceof SLA<?>
        || instr instanceof SRA<?> || instr instanceof SLL<?> || instr instanceof SRL<?>) {
      rmw(((TargetInstruction<?>) instr).getTarget());
      write("F");
      return;
    }
    if (instr instanceof RLD || instr instanceof RRD) {
      read("A", 'V');
      readPair("HL", 'A');
      addRole("MEM", 'V');
      write("A");
      write("F");
      return;
    }
    if (instr instanceof SCF || instr instanceof CCF) {
      if (instr instanceof CCF)
        read("F", 'V');
      write("F");
      return;
    }
    // Nop, Halt, DI, EI, IM, RetN interrupt flags: no register footprint we track
    if (instr instanceof TargetSourceInstruction<?, ?> ts) { // generic fallback
      readRef(ts.getSource(), 'V');
      readRef(ts.getTarget(), 'V');
      writeRef(ts.getTarget());
      write("F");
      return;
    }
    if (instr instanceof TargetInstruction<?> t) {
      rmw(t.getTarget());
    }
  }

  private void condition(Condition c) {
    if (c instanceof ConditionFlag) {
      kind = "BRANCH";
      read("F", 'C');
    } else if (c instanceof BNotZeroCondition) {
      kind = "BRANCH";
      read("B", 'C');
      write("B");
    }
  }

  /** JP (HL): the jump target register is consumed as an address. */
  private void jumpOperand(ImmutableOpcodeReference<?> position) {
    if (position instanceof Register<?> r)
      for (int s : slots(r))
        addRead(s, Tracer.CH_NAME[s], 'A');
  }

  private void rmw(OpcodeReference<?> target) {
    readRef(target, 'V');
    writeRef(target);
  }

  private void read(String reg, char role) {
    for (int s : SLOTS.get(reg))
      addRead(s, Tracer.CH_NAME[s], role);
  }

  private void readPair(String pair, char role) {
    read(pair, role);
  }

  private void write(String reg) {
    for (int s : SLOTS.get(reg))
      if (!writes.contains(s))
        writes.add(s);
  }

  private void writePair(String pair) {
    write(pair);
  }

  private void addRead(int slot, String ch, char role) {
    if (!reads.contains(slot))
      reads.add(slot);
    addRole(ch, role);
  }

  /** a channel can play several roles at one site (LD (HL),H: H is ADDR and VAL) — letters accumulate. */
  private void addRole(String ch, char role) {
    roles.merge(ch, String.valueOf(role), (old, add) -> old.contains(add) ? old : old + add);
  }

  /** register slots a reference READS when evaluated, with the proper role. */
  private void readRef(Object ref, char valueRole) {
    if (ref instanceof Register<?> r) {
      for (int s : slots(r))
        addRead(s, Tracer.CH_NAME[s], valueRole);
    } else if (ref instanceof IndirectMemory8BitReference<?> m) {
      addrReads(m);
      addRole("MEM", 'V');
    } else if (ref instanceof IndirectMemory16BitReference<?> m) {
      addrReads(m);
      addRole("MEM", 'V');
    } else if (ref instanceof MemoryPlusRegister8BitReference<?> m) {
      addrReads(m);
      addRole("MEM", 'V');
    } else if (ref instanceof MemoryAccessOpcodeReference<?>) {
      addRole("MEM", 'V');
    }
    // immediates and constants contribute nothing
  }

  /** the registers composing a memory operand's ADDRESS (read even when the operand is written). */
  private void addrReads(Object ref) {
    if (ref instanceof IndirectMemory8BitReference<?> m) {
      if (m.target instanceof Register<?> r)
        for (int s : slots(r))
          addRead(s, Tracer.CH_NAME[s], 'A');
    } else if (ref instanceof IndirectMemory16BitReference<?> m) {
      if (m.target instanceof Register<?> r)
        for (int s : slots(r))
          addRead(s, Tracer.CH_NAME[s], 'A');
    } else if (ref instanceof MemoryPlusRegister8BitReference<?> m) {
      if (m.getTarget() instanceof Register<?> r)
        for (int s : slots(r))
          addRead(s, Tracer.CH_NAME[s], 'A');
    }
  }

  /** register slots a reference WRITES when assigned (memory writes flow via listeners). */
  private void writeRef(Object ref) {
    if (ref instanceof Register<?> r)
      for (int s : slots(r))
        if (!writes.contains(s))
          writes.add(s);
  }

  String rolesString() {
    if (roles.isEmpty())
      return null;
    StringBuilder sb = new StringBuilder();
    roles.forEach((ch, role) -> sb.append(sb.isEmpty() ? "" : ";").append(ch).append('=').append(role));
    return sb.toString();
  }

  // ---------- equation rendering (phase 2: after first execution, immediates readable) ----------

  /**
   * The normalized equation in the detector dialect. Rendered AFTER the instruction executed
   * once, so immediate operands can be re-read from memory (those reads carry delta&gt;0 and
   * are filtered from the stats).
   */
  static String equation(Instruction<?> instr) {
    if (instr instanceof Ldir || instr instanceof Ldi)
      return "ldir: mem[DE..DE+BC-1] = mem[HL..HL+BC-1]";
    if (instr instanceof Lddr || instr instanceof Ldd)
      return "lddr: mem[DE-BC+1..DE] = mem[HL-BC+1..HL]";
    if (instr instanceof Cpir || instr instanceof Cpi || instr instanceof Cpdr || instr instanceof Cpd)
      return "cpir(A, mem[HL], BC)";
    if (instr instanceof Push p)
      return "push(" + render(p.getTarget()) + ")";
    if (instr instanceof Pop p)
      return render(p.getTarget()) + " = pop()";
    if (instr instanceof Call c)
      return prefix(c.getCondition()) + "call " + target(c);
    if (instr instanceof RST r)
      return "call " + target(r);
    if (instr instanceof Ret r)
      return prefix(r.getCondition()) + "ret";
    if (instr instanceof DJNZ)
      return "if (--B != 0)";
    if (instr instanceof JP<?> j)
      return j.getCondition() instanceof ConditionFlag ? prefix(j.getCondition()) + "goto"
          : "goto " + render(j.getPositionOpcodeReference());
    if (instr instanceof JR<?> j)
      return j.getCondition() instanceof ConditionFlag ? prefix(j.getCondition()) + "goto" : "goto";
    if (instr instanceof In<?> in)
      return render(in.getTarget()) + " = in(" + render(in.getSource()) + ")";
    if (instr instanceof Out<?> out)
      return "out(" + render(out.getTarget()) + ", " + render(out.getSource()) + ")";
    if (instr instanceof Ex<?> ex)
      return "ex(" + render(ex.getTarget()) + ", " + render(ex.getSource()) + ")";
    if (instr instanceof Exx)
      return "exx()";
    if (instr instanceof LdAI)
      return "A = I";
    if (instr instanceof LdAR)
      return "A = R";
    if (instr instanceof Ld<?> ld)
      return render(ld.getTarget()) + " = " + render(ld.getSource());
    if (instr instanceof Cp<?> cp)
      return "cp(A, " + render(cp.getSource()) + ")";
    if (instr instanceof BIT<?> b)
      return "bit(" + render(b.getTarget()) + ", " + bitOf(b) + ")";
    if (instr instanceof SET<?> s)
      return render(s.getTarget()) + " = set(" + render(s.getTarget()) + ", " + bitOf(s) + ")";
    if (instr instanceof RES<?> r)
      return render(r.getTarget()) + " = res(" + render(r.getTarget()) + ", " + bitOf(r) + ")";
    if (instr instanceof Add16<?> a)
      return binary16(a.getTarget(), "add16", a.getSource());
    if (instr instanceof Adc16<?> a)
      return binary16(a.getTarget(), "adc16", a.getSource());
    if (instr instanceof Sbc16<?> a)
      return binary16(a.getTarget(), "sbc16", a.getSource());
    if (instr instanceof Inc16<?> i)
      return render(i.getTarget()) + " = inc16(" + render(i.getTarget()) + ")";
    if (instr instanceof Dec16<?> d)
      return render(d.getTarget()) + " = dec16(" + render(d.getTarget()) + ")";
    if (instr instanceof Adc<?> a)
      return alu(a, "adc");
    if (instr instanceof Sbc<?> a)
      return alu(a, "sbc");
    if (instr instanceof Add<?> a)
      return alu(a, "add");
    if (instr instanceof Sub<?> a)
      return alu(a, "sub");
    if (instr instanceof And<?> a)
      return infix(a, "&");
    if (instr instanceof Or<?> a)
      return infix(a, "|");
    if (instr instanceof Xor<?> a)
      return infix(a, "^");
    if (instr instanceof Inc<?> i)
      return render(i.getTarget()) + " = inc(" + render(i.getTarget()) + ")";
    if (instr instanceof Dec<?> d)
      return render(d.getTarget()) + " = dec(" + render(d.getTarget()) + ")";
    if (instr instanceof Neg)
      return "A = 0 - A";
    if (instr instanceof CPL)
      return "A = A ^ 255";
    if (instr instanceof DAA)
      return "A = daa(A)";
    if (instr instanceof RLA)
      return "A = rl(A)";
    if (instr instanceof RRA)
      return "A = rr(A)";
    if (instr instanceof RLCA)
      return "A = rlc(A)";
    if (instr instanceof RRCA)
      return "A = rrc(A)";
    if (instr instanceof RL<?> r)
      return unary(r.getTarget(), "rl");
    if (instr instanceof RR<?> r)
      return unary(r.getTarget(), "rr");
    if (instr instanceof RLC<?> r)
      return unary(r.getTarget(), "rlc");
    if (instr instanceof RRC<?> r)
      return unary(r.getTarget(), "rrc");
    if (instr instanceof SLA<?> r)
      return unary(r.getTarget(), "sla");
    if (instr instanceof SRA<?> r)
      return unary(r.getTarget(), "sra");
    if (instr instanceof SLL<?> r)
      return unary(r.getTarget(), "sll");
    if (instr instanceof SRL<?> r)
      return unary(r.getTarget(), "srl");
    if (instr instanceof RLD)
      return "rld(A, mem[HL])";
    if (instr instanceof RRD)
      return "rrd(A, mem[HL])";
    if (instr instanceof SCF)
      return "flagC = 1";
    if (instr instanceof CCF)
      return "flagC = !flagC";
    if (instr instanceof Halt)
      return "halt";
    return instr.getClass().getSimpleName().toLowerCase() + "()";
  }

  private static String alu(TargetSourceInstruction<?, ?> a, String op) {
    String t = render(a.getTarget());
    return t + " = " + op + "(" + t + ", " + render(a.getSource()) + ")";
  }

  private static String infix(TargetSourceInstruction<?, ?> a, String op) {
    String t = render(a.getTarget());
    return t + " = " + t + " " + op + " " + render(a.getSource());
  }

  private static String binary16(OpcodeReference<?> target, String op, Object source) {
    String t = render(target);
    return t + " = " + op + "(" + t + ", " + render(source) + ")";
  }

  private static String unary(OpcodeReference<?> target, String op) {
    String t = render(target);
    return t + " = " + op + "(" + t + ")";
  }

  private static String bitOf(BitOperation<?> b) {
    return String.valueOf(b.getN());
  }

  private static String target(Instruction<?> c) {
    try {
      Object v = ((AbstractInstruction<?>) c).getNextPC();
      if (v instanceof WordNumber w)
        return String.valueOf(w.intValue());
      if (c instanceof ConditionalInstruction<?, ?> ci && ci.getPositionOpcodeReference() != null)
        return render(ci.getPositionOpcodeReference());
    } catch (Exception ignored) {
    }
    return "?";
  }

  /** the flag-condition prefix in the detector dialect: {@code if (flagZ(F)) }... */
  private static String prefix(Condition c) {
    if (!(c instanceof ConditionFlag))
      return "";
    String s = c.toString(); // Z / NZ / C / NC / PE / PO / P / M
    boolean neg = s.startsWith("N") && !s.equals("N");
    String flag = switch (neg ? s.substring(1) : s) {
      case "Z" -> "flagZ(F)";
      case "C" -> "flagC(F)";
      case "PE", "PO", "P" -> "flagPV(F)";
      case "M", "S" -> "flagS(F)";
      default -> "flag(F)";
    };
    return "if (" + (neg ? "!" : "") + flag + ") ";
  }

  /** one operand in the detector dialect: {@code HL}, {@code mem[IX + 3]}, {@code 16384}. */
  private static String render(Object ref) {
    if (ref instanceof Register<?> r)
      return r.getName();
    if (ref instanceof IndirectMemory8BitReference<?> m)
      return "mem[" + render(m.target) + "]";
    if (ref instanceof IndirectMemory16BitReference<?> m)
      return "mem[" + render(m.target) + "]";
    if (ref instanceof MemoryPlusRegister8BitReference<?> m) {
      String reg = m.getTarget() instanceof Register<?> r ? r.getName() : "?";
      int d = m.fetchRelative();
      return "mem[" + reg + (d < 0 ? " - " + (-d) : " + " + d) + "]";
    }
    if (ref instanceof MemoryAccessOpcodeReference<?> m)
      return "mem[" + render(m.getC()) + "]";
    if (ref instanceof ImmutableOpcodeReference<?> imm) {
      try {
        Object v = imm.read();
        if (v instanceof WordNumber w)
          return String.valueOf(w.intValue());
      } catch (Exception ignored) {
      }
    }
    return String.valueOf(ref);
  }
}
