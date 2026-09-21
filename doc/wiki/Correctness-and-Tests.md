# Correctness and Tests

971 `@Test` methods across the reactor — and several of them drive batteries far larger than themselves: one
of them is 1,356 processor tests, another is 131,072 combinations per instruction.

The point of the arrangement is that **the same batteries run unchanged against any implementation of the
processor**. The object model and the generated core are held to the identical bar, through a
`ServiceLoader` seam (`ProcessorUnderTest`), without the tests knowing which one they are on.

## The processor

| Battery | Size | What it pins down |
|---|---|---|
| **The CPU vector battery** | 1,356 blocks in `tests.in` (the documents say 1,355) | registers, memory, MEMPTR, the total T-state count, and the exact list of bus events **with their instant** |
| **`AluReferenceTest`** | up to **131,072 combinations per instruction** | the ALU against the published Z80. It found **four instructions that passed the vector battery and were still wrong** |
| **`AllTableAluOperationsCompatibilityTest`** | MD5 per table (RLA 512, two-operand boolean 131,072, three-operand 16,777,216) | that a flag table did not drift |
| **emuStudio** | 226 `@Test` | a third party's suite, unmodified |
| **On the generated core** | "the 1,592 pass" | the same three batteries, on the other core |

There is no zexall/zexdoc here; the vectors and the ALU sweep do that job, and the ALU sweep does it harder.

## The machine

| | |
|---|---|
| **ROM boot on both cores** | 300 frames, then compare `AF..R`, MEMPTR, the T-state count **and all of RAM** between the two cores; then 150 + 150 frames switching processor halfway |
| **`EmulationRegressionTest`** | hashes of the screen, the system variables, RAM and the registers after 268 boot frames. A mismatch dumps the screen as a PNG so you can look at what changed |
| **Contention** | `ZXSpectrumContendedMemoryTests` (64), `ZXSpectrumULATests` (17), and `ContentionRunsTest`, which starts six machines **from every T-state of the frame** |
| **Per machine** | every model is asked the same questions: its label, its ROMs, its ports, the size of its contention table, whether the screen follows the beam, whether a snapshot picks it |
| **RZX** | 20 tests that replay recordings and check the fetches per frame — determinism, to the fetch |
| **Configuration** | `SectionsAreDeclaredTest` holds the config file and the classes that declare it together |

## The bridge: the reference emulator's real core

`machine/bridge` runs **Fuse's own core** — the C one, through JNA to `fuse_libretro.so` — behind the same
interface this emulator's core implements, driven by one command protocol. `LibretroCore` and
`LocalLibretroCore` are two implementations of it. 86 tests in 10 classes compare the two machines step by
step.

It is a module of its own, and **the emulator does not know it exists**. It is also what settles arguments:
the tape readers are checked against libspectrum's own regression corpus the same way
([Tapes, Disks and Snapshots](Tapes-Disks-and-Snapshots)).

## What each layer is for

- A **unit** test says what one class does.
- The **vector battery** says the processor is a Z80.
- The **ALU sweep** says the flags are the real chip's, including the undocumented bits.
- The **bridge** says the machine is the same machine as a known-good one, instruction by instruction.
- An **RZX** says the whole thing is deterministic over hours of play.
- The **regression hashes** say nothing changed that nobody meant to change.
- The **freshness test** says the generated artefact is what the model produces today.

## Running them

```bash
mvn -o test                          # everything
mvn -o -pl machine/core test         # one module
mvn -o -pl machine/generated test    # the batteries on the generated core
```

Tests run on a silent sound device rather than a muted one: the real device opens an audio line, and a crash
inside the platform's audio server takes the JVM with it.

## Where this lives

`emulator/src/test` (the batteries), `machine/*/src/test` (the machine), `machine/bridge` (the comparison),
`zx-rzx` (determinism), `doc/TABLA_ALU_TESTS.md` (what the ALU sweep found).
