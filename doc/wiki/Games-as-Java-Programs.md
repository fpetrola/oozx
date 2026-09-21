# Games as Java programs

This is the part that only makes sense because the Z80 is made of objects.

An instruction here is an object with operands that are objects. You can execute it — or you can **run it
without a machine underneath**, symbolically, and record what it did: which addresses it reached, which
registers carried what, where it branched, what it wrote over. Do that from a game's entry point and you get
the shape of the program: its routines, its data, its self-modifying code.

Then emit Java.

## It runs

| | |
|---|---|
| ![Dynamite Dan as a Java program](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/dan-95%25.gif) | Dynamite Dan, translated to Java, **stepped in a debugger**: breakpoints on `$CD8A()`, `wMem(51326, A, pc: 52626)` in the variables pane, and the game playing in a window with no emulator under it |

![Everyone's a Wally as Java](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wally-90%25.gif)

![Jet Set Willy as Java](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/jsw1.gif)

Jet Set Willy, Manic Miner, Dynamite Dan, Everyone's a Wally and Sam Cruise have run this way. The class
that comes out looks like this — a method per routine, named after its address:

```java
public class ZxGame1 extends MiniZX {
  public void $CD8A() {
    A = 0;
    wMem(51326, A, pc: 52626);
    HL(51316);
    int var1 = HL();
    ...
  }
}
```

It is Java. You can set a breakpoint in it, watch a variable, profile it, step through the frame in which
Willy dies.

## How it is done

1. **Symbolic execution** (`translation/se`) runs the game with nobody playing: an `InstructionSpy` sits on
   the executor, the registers are backed by values that remember where they came from
   (`DirectAccessWordNumber`, `ReturnAddressWordNumber` — that is how a return address is recognised as one),
   and the memory records who wrote what.
2. **Blocks and routines** (`translation/blocks`, `translation/routines`) turn the trace into structure: what
   is code, what is data, where a routine starts and ends, which branches are real.
3. **Bytecode** (`translation/bytecode`, `translation/translator`) emits a class per game, a method per
   routine, with the Z80's registers as fields and memory as an array.
4. `MiniZX` is the runtime it extends: a screen, a keyboard, and the handful of operations the emitted code
   calls (`mem`, `wMem`, the 16-bit register pairs). The window in the animations above is exactly that.

```bash
# translate a game, from a URL and an entry point
java -cp translation/translator/target/... \
     com.fpetrola.z80.bytecode.examples.RemoteZ80Translator \
     translate jetsetwilly http://torinak.com/qaop/bin/jetsetwilly 34762
```

## What it is good for

- **Reading a game.** The routines have names, the data has been told apart from the code, and the branches
  are visible. This is the decompilation of a 1984 binary into something a person can read.
- **Debugging a game with a modern debugger** — a real one, with breakpoints and watches, on the game's own
  logic.
- It is also the honest reason the emulator is built this way. The same hooks that a symbolic executor needs
  are the ones that give tracing, profiling and instrumentation for nothing; and the same fixed object graph
  that makes the analysis possible is what the [generated core](The-Generated-Core) is produced from.

## Where this lives

`translation/` — `se` (symbolic execution), `blocks` (control-flow blocks), `routines`, `bytecode` and
`translator` (the emission), `virtual` (the runtime). 199 classes. It is a separate line of work from the
emulator and is not needed to run one.
