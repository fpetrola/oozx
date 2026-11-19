# Why it all fits

The clean model is not the price of the fast emulator. It is its source.

The whole project is one bet of mine: that a Spectrum modelled as plain objects, with nothing hidden in a
global and nothing special-cased, buys more than it costs, and that when it costs speed, the speed can be
bought back *from the model itself* rather than by giving the model up.

## One decision, and what you have already seen

Everything on the other pages comes out of one decision: **objects down to the Z80, and no global state**.

> **Picture** · the tree of consequences: one decision on the left, what it means in the middle, and on the
> right what it allows, each with the thing you saw working. (drawn in the plan; to redraw for the site)

| The decision means | which allows | and you saw it as |
|---|---|---|
| **The Z80 is an object** | having nine at once | Spec256: a 1984 game in 256 colours → [256 colours](256-colours) |
| | running it with no machine underneath | games translated to Java → [The translator](The-translator) |
| | wrapping it | the debugger, the trace and the heatmap, without touching it → [The game lab](The-game-lab) |
| | flattening it | the generated core, 4.3 times as fast → [The fast core](The-fast-core) |
| **Input and output are a contract** | swapping them for a recording | replays, and *Take Over* at any frame → [Recordings and Take Over](Recordings-and-Take-Over) |
| **A machine is a graph of objects** | many of them in one process | several Spectrums on one desk, and a session that comes back → [The desk](The-desk) |
| **Nothing names a peripheral** | everything arriving as a plugin | a peripheral in under fifty lines, and the plugin catalogue inside → [Build a peripheral](Build-a-peripheral) |

## What that means, part by part

**The Z80 is made of objects.** Every instruction is an object: `Ld`, `Add16`, `JR`, grouped by shape rather
than by opcode. Its operands are objects too: a register, a memory reference, `(IX+d)`, a constant, a
condition. Decoding follows Cristian Dinu's decomposition of the opcode byte, as tables of prototype
instructions. Because an instruction is something you can hold, clone, inspect and run on your own terms, a
game's code can be analysed and not only executed.

**Nothing foreign inside the processor.** Two things usually found in the middle of a Z80 emulator are
outside this one. The Spectrum's memory timing is attached from outside, by a class whose own comment says
"the Z80 model does not know this class exists". And MEMPTR, the Z80's hidden register, is an ordinary
register kept up to date by a spy attached the same way. Leave either off and the processor never notices.

**Every part behind a contract, and each one actually replaced.**

| Contract | Replaced by |
|---|---|
| the processor's core | the object model, the generated core, and Spec256's nine, switchable while the machine runs |
| input and output | the recording player's, which answers every `IN` from a recording |
| memory | the test harness's instrumented one; a follower's, in Spec256 |
| the sound card | a silent one, which is what every test runs on |

**A machine is an object graph**, built in one place. Several machines are several graphs in one process,
which is why the desk can hold six Spectrums with nothing shared to fight over.

**Nothing names a peripheral.** A peripheral answers ports on a bus that merges answers the way the real one
does, with the floating bus for the bits nobody drives. It may watch the program counter to page its ROM in.
It is found, not named: the machine asks who is there.

## The costs are the proof

I do not expect anybody who cares about design to believe adjectives. What a design is worth shows in what
it cost to add the next thing.

| What was added | What it cost |
|---|---|
| **Spec256**: nine Z80s in lockstep | 1,689 lines of its own, and 258 in the rest of the code, across 23 files, without a special case in any of them. And one line removed → [256 colours](256-colours) |
| **The ZXMMC** | three files and under fifty lines |
| **The three Pentagons** | six files and 382 lines. The first was added mostly to see what adding a machine costs |
| **The fast core** | 27,376 lines nobody wrote, 4.3 times as fast as the model they came from → [The fast core](The-fast-core) |
| **The same bar for both cores** | 1,591 tests that run without knowing which core they are on → [How right, and how we know](How-right-and-how-we-know) |

## The idea, by its name

The fast core is made by **partial evaluation**: specialising an interpreter with respect to what does not
change. It is the same idea that Truffle rests on in GraalVM. Here the interpreter is the object model, and
what does not change is the machine once it is built: which instruction sits behind which opcode, which
operands it has, which timing rule applies where.

The model can be flattened because of how it is made, not because it was made for that. The graph of
instances is fixed once a machine exists, which is the situation a partial evaluator is for. And the timing
and MEMPTR are visitors over that graph, which is the shape a specialiser resolves best.

Generating a Z80 core is not new. [Fuse generates its own from tables, with a script](https://sources.debian.org/src/fuse-emulator/1.1.1%2Bdfsg1-2/z80/z80.pl).
What is different here is the source: not a table, but a model that also runs,
that stays the reference, and that passes the same tests. I looked for a published precedent of a core
derived from an object model of the processor and found none. That is a statement about my search.

There is a symmetry worth saying aloud. The generated core freezes what is fixed in a *machine*. The
translator freezes what is fixed in a *game*. It holds as an analogy and not as a description of how the
translator works, but it puts the two under one sentence.

## Where to start reading

`emulator/src/main/java/com/fpetrola/z80/`: the instructions, the decoder's tables, the registers. Then
`machine/core/src/main/java/com/fpetrola/oozx/Speccy.java`, which is where a machine is put together. What
each module owns is on [Building from source](Building-from-source), and the rules all of it is held to are
on [The rules of the code](The-rules-of-the-code).

Next: [The parts, on one page](The-parts).
