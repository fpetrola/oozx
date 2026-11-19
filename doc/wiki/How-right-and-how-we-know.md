# How right, and how we know

The same tests run against every implementation of the processor, without knowing which one they are on.
The object model and the fast core generated from it are held to the identical bar, and the referee for the
whole machine is another emulator's real core.

## The processor

| Battery | Size | What it pins down |
|---|---|---|
| **Fuse's vectors** | 1,356 tests | registers, memory, MEMPTR, the total of T-states, and the exact list of what went over the bus, **with the instant of each** |
| **The ALU reference** | up to 131,072 combinations for one instruction | the arithmetic and its flags against the published Z80, undocumented bits included. It found **four instructions that passed the vectors and were still wrong** |
| **emuStudio's suite** | 226 tests | a third party's tests, unmodified |

Together they are 1,591 tests: the 1,356 vectors, the nine of the ALU reference, each of which sweeps its
combinations, and emuStudio's 226. All of them run a second time on the generated core. What one of those tests looks like, and how one
instruction answers it in both cores, is on [The fast core](The-fast-core).

## The machine

| | |
|---|---|
| **A ROM booted on both cores** | 300 frames, then every register, MEMPTR, the count of T-states **and all of RAM** compared between the two; then 150 and 150 frames, switching processor halfway |
| **Regression hashes** | the screen, the system variables, RAM and the registers after a boot. When a hash moves, the screen is written out as a picture so you can look at what changed |
| **Memory timing** | tests that start machines **from every T-state of the frame** |
| **Each machine** | every model is asked the same questions: what it is called, which ROMs it runs, which ports answer, how big its timing table is, whether its screen follows the beam, whether a snapshot picks it |
| **Recordings** | twenty-three tests that replay somebody's game and count the instructions fetched in every frame |

## The referee: Fuse's own core

`machine/bridge` runs **Fuse's core**, the C one, behind the same interface this emulator's own core
implements. When there is a disagreement about timing, both machines are run over the same program and
compared step by step.

It is the best thing I can say about another emulator: when I need to know what a Spectrum does, I ask
Fuse. The readers of tape and snapshot files are checked the same way, against libspectrum and its own
collection of awkward files. → [Formats](Formats)

The emulator does not know the bridge exists. It is a module of its own.

## Hours of somebody's game

A recording is the strictest test here. It only replays if the emulator does what the original machine did,
instruction for instruction, for as long as the recording lasts. One fetch too many anywhere and the replay
falls out of step, and says so. → [Recordings and Take Over](Recordings-and-Take-Over)

## What each layer is for

- A **unit** test says what one class does.
- The **vectors** say the processor is a Z80.
- The **ALU reference** says the flags are the real chip's.
- The **bridge** says the machine is the same machine as a known good one, instruction by instruction.
- A **recording** says the whole thing does the same thing every time, over hours of play.
- The **regression hashes** say nothing changed that nobody meant to change.

## A picture with no window

A machine can be built, run and photographed with nothing on the screen. That is how the Spectrum pictures
in these pages were made, and how a test shows you what changed:

```java
Speccy speccy = silentMachine();
select(speccy, speccy.machine.model(Spec48.class));
speccy.picture.active = true;
Snapshots.of(speccy).load("manicminer.z80");
runFrames(speccy, 120);
// speccy.picture.pixels is the picture, border included
```

## What I have not run

The test programs the Spectrum scene uses as its yardstick, zexall, zexdoc, z80test, are not part of this.
The vectors and the ALU reference do that job, and I believe the ALU reference does it harder, but those are
the house's names and not the scene's. A page that runs the scene's own tests, and the games known to be
hard to get right, is still to be written.

## Running them

```bash
mvn test                          # everything
mvn -pl machine/core test         # one module
mvn -pl machine/generated test    # the batteries on the generated core
```

Tests run on a silent sound card rather than a muted one: a real one opens an audio line, and a crash inside
the computer's sound system takes the tests down with it.

Next: [The game lab](The-game-lab).
