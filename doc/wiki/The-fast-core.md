# The fast core

The fast core is 27,376 lines of Java that nobody wrote. A program reads the object model and writes them,
and what it writes runs 4.3 times as fast as the model and passes the same tests.

The object model stays the reference. It is also, literally, the *source* of the fast one: a partial
evaluator reads the model, its Java source **and** the live objects of a machine built for the occasion, and
writes one class in which the instructions, their operands and the timing have been flattened away. No
objects on the hot path, no virtual calls, no tables of prototypes: just code.

## One instruction, three views

`ADD HL,BC`, opcode `09`, from the model to the test both cores have to pass.

**In the model** it is an object. The decoder's table makes it out of two references, the pair it adds to
and the pair it adds:

```java
i.Add16(hlOrIx, rp[p])
```

and the class says what an addition of sixteen bits is, and nothing about a Spectrum:

```java
public class Add16 extends Binary16BitsOperation {
  public Add16(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag, new Add16TableAluOperation());
  }

  protected int operation(int v1, int v2, int f) {
    return v1 + v2;
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingAdd16(this))
      super.accept(visitor);
  }
}
```

There is no timing in it and no MEMPTR. Those are visitors that come to it through `accept`.

**In the generated core** the same instruction is a `case` in a `switch`, with everything that used to come
from outside woven in:

```java
case 0x09: {
    int _F14;
    contend7x1(((I << 8) | R));
    MEMPTR = ((((H << 8) | L) + 1) & 0xFFFF);
    int a_24 = ((H << 8) | L);
    int b_25 = ((B << 8) | C);
    int result_26 = (a_24 + b_25);
    int value1_27 = ((a_24 & 0x0800) >> 4 | result_26 >> 11);
    int value2_28 = F;
    int value3_29 = (b_25 >> 11) & 1;
    _F14 = value2_28;
    int add16temp_30 = value1_27 << 11;
    int lookup_31 = (((value1_27 << 4) & 0x0800) >> 11) | ((value3_29 << 11) >> 10) | ((add16temp_30 & 0x0800) >> 9);
    _F14 = (_F14 & 0xC4) | ((add16temp_30 & 0x10000) != 0 ? 1 : 0) | ((add16temp_30 >> 8) & 0x28) | HALF_CARRY_ADD[lookup_31];
    F = (_F14 & 0xFF);
    int value_33 = (result_26 & 0xffff);
    H = (value_33 >>> 8);
    L = value_33 & 0xFF;
    PC = (PC + 1) & 0xFFFF;
    break;
}
```

The registers are fields. `contend7x1` is the Spectrum's timing: the seven T-states this instruction spends
after its fetch, each one a check of whether the ULA holds the processor up. The line after it is MEMPTR. The rest is `Add16` and its
flags, with the constants folded in. Nobody wrote this by hand, and nobody maintains it.

**In the test** it is a list of what has to happen, and when. These are Fuse's vectors, and this is the one
called `09`. Going in, `BC` is `5678` and `HL` is `9abc`:

```
    0 MC 0000
    4 MR 0000 09
    4 MC 0001
    5 MC 0001
    6 MC 0001
    7 MC 0001
    8 MC 0001
    9 MC 0001
   10 MC 0001
0030 5678 0000 f134 0000 0000 0000 0000 0000 0000 0000 0001 9abd
00 01 0 0 0 0 11
```

The first column is the T-state. The seven lines from 4 to 10 are the seven checks of `contend7x1`. `f134` is
`5678 + 9abc`, `9abd` at the end of that line is MEMPTR, and the `11` closing the last line is how many
T-states it all took. The model and the generated core both have to produce this list, event for event.

## What is woven in

To write the core, the build makes a silent machine on the model and hands the generator four things:

1. **The instruction instances** of the decoder's tables: not classes, *instances*, with their operands
   already bound.
2. **The MEMPTR aspect**, a visitor, resolved against each instance.
3. **The timing aspect**, recording what it would do at each point of each instruction.
4. **The machine's memory**, as `read` and `write` helpers.

The specialiser then applies a handful of rules: configuration fields become literals, calls to
collaborators are inlined recursively, type checks are folded, the arithmetic goes through the same
operation objects the model uses. The tables become nested `switch`es, split into methods of sixteen opcodes
so that no method crosses HotSpot's limit for inlining.

What comes out today is 27,376 lines with 1,792 `case`s in 231 decoding methods, and six timing helpers. Its
first line says which model it came from.

## The boundary: what is frozen and what is read

One rule, stated once and applied everywhere:

> Freeze what cannot change while this core instance lives; read, on every access, everything the machine
> mutates.

Frozen: the memory, the ULA, the clock, the display, the ports. Read every time: the page table, a page's
fields, the contention tables, the T-state counter. The whole banking model of this machine is "change the
page table", so the page table can never be baked in.

## When it is written

It is a source file, committed, and the build owns it:

- On every build, the model's hash, taken over the model's sources and the generator's, is compared with the
  one on the file's first line.
- Same hash: nothing happens.
- Different: the file is written again, which takes about eleven seconds, and compiled, which takes two.
- At run time a machine simply loads the class. **Nothing is generated or compiled while the emulator runs.**

## How a machine gets it

The fast core is found, not named: it arrives in a jar of its own. If the jar is there, a machine runs on
it; if it is not, you get the model. The model is used instead when the settings say so (*Processor*, under a machine's
settings), and by tests that count T-states for themselves.

A running machine can be moved from one to the other **in the middle of a game**, which is also how the two
are compared on the same machine in the same session.

## How it is kept honest

| | |
|---|---|
| **The processor's batteries, on the generated core** | Fuse's vectors, the ALU reference and emuStudio's suite: 1,591 tests that run without knowing which core they are on |
| **The machine's suite, on the generated core** | the same tests the machine passes on the model |
| **A ROM booted on both** | 300 frames on each core, then every register, MEMPTR, the T-state count and all of RAM compared; then 150 and 150 frames, switching processor in the middle |
| **Freshness** | the build rewrites the file whenever the model's hash moves, and a slower test compares the whole file, character for character, with what the model produces today |

## Why the model allows it

This is the part that is not an accident.

- The model does not adapt to the tool. The generator exists because the model has the shape a specialiser
  wants, not the other way round.
- The graph of instances is **fixed** once a machine is built.
- The aspects are **visitors**.
- There are no per-instruction templates anywhere: one mechanism, applied to a graph.
- What the generator inlines arrives with its identities resolved; what the JIT inlines does not.

What the generated core still does not do is on [Limits](Limits): it reports no memory events to whoever
watches the bus, and one peripheral has never been run on it under a test.

The diaries of how it was built are `doc/plan-nucleo-generado.md`, `doc/plan-nucleo-generado-jvm.md` and
`doc/plan-nucleo-generado-modulo.md`.

Next: [How fast, and how we know](How-fast-and-how-we-know).
