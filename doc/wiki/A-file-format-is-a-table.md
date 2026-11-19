# A file format is a table

A snapshot format here is not a reader and a writer. It is one table that says which byte of the file is
which part of the machine, and the same table is read in both directions.

## A whole format

This is the processor's part of SNA, as it is in the code:

```java
static final Fixed HEADER = Fixed.of("header", 27);

static final Layout<Cpu> CPU = Layout.<Cpu>of()
    .u8(HEADER, 0, I)
    .u16(HEADER, 1, HL_).u16(HEADER, 3, DE_).u16(HEADER, 5, BC_).u16(HEADER, 7, AF_)
    .u16(HEADER, 9, HL).u16(HEADER, 11, DE).u16(HEADER, 13, BC).u16(HEADER, 15, IY).u16(HEADER, 17, IX)
    .bit(HEADER, 19, 2, IFF2).alsoSets(IFF1)
    .u8(HEADER, 20, R).u16(HEADER, 21, AF).u16(HEADER, 23, SP)
    .bits(HEADER, 25, 0, 0x03, IM)
    .u16(TAIL, 0, PC).u16(STACKED, 0, PC);
```

Each line is a place in the file and a field of a part. Loading walks the machine and fills each part from
its table; saving walks the same machine and the same tables the other way. There is no second piece of
code to keep in step with the first.

The rest of SNA is as short: the border, the paging port, the RAM, and two *shapes*, the 48K one and the
128K one, each a list of what comes after what. The 48K has nowhere to keep the PC and leaves it on the
stack; that is one rule of a few lines. The whole format is 129 lines.

| Format | Lines | What is particular to it |
|---|---|---|
| **SNA** | 129 | the PC on the stack |
| **SP** | 120 | |
| **SZX** | 138, and 179 for its blocks | a file made of named blocks |
| **EZX** | 190 | packed with NRV2E, and it carries a Spec256 game's colours |
| **Z80** | 265 | three versions of the header, and its own packing |

What they share is the engine under them: places, shapes, packing, pages of RAM, and the refusal of a file
that is cut short or does not add up.

## How they are held

- **Against the reference.** Eighty-seven snapshot files are in the tree, and they are read here and by
  libspectrum, the library Fuse reads its files with. The two have to agree on the machine that comes out.
- **A cut file is refused.** Every format is given its own files truncated, and has to say no before it
  touches the machine.
- **A format answers for what it reads.** What a format does not carry is said out loud when saving, not
  dropped quietly.

## The session is an SZX

When you quit, each machine is kept as a snapshot in SZX, the format with named blocks. That is why the
desk comes back as you left it, with every machine running where it was.

- **The machine goes in the blocks the format defines**: the processor, the RAM, the paging, the AY, the
  palette.
- **Each peripheral goes in a block of its own**: the Multiface's RAM and whether it is paged in, the +D's
  and the Beta 128's controller, the DivIDE's pages, the byte last written to a Covox, the ZX Printer, the
  µSpeech.
- **Everything else goes in one block of this emulator's, `OOZX`**, which another emulator passes over. It
  is plain text, a key a line: which machine it is exactly and the ROMs it runs on, every setting of the
  machine and of its peripherals, which peripherals are plugged in, how the window shows the picture,
  whether it is paused, the file it came from, its processor and speed, the pokes applied, and the windows
  clipped onto it.

Where each window is, the desk keeps beside it.

So a session file is still an SZX. Another emulator can open the machine in it; this one also gets back
everything around the machine.

## A peripheral is saved without knowing it

A peripheral here never hears of a file. It has no `save` and no `load`, and neither does a tool or a
window. What it has is what it is: the Covox can say the byte last written to it, and be told one.

Whoever knows the file does the rest. This is the whole of how the Covox is kept:

```java
@OnlyWith("device-covox")
public final class CovoxMapping implements Mapping {

  static final Fixed COVX = Fixed.of("COVX", 4);

  static final Layout<CovoxPeripheral> TABLE = Layout.<CovoxPeripheral>of()
      .u8(COVX, 0, Field.of("covox.written", CovoxPeripheral::written, CovoxPeripheral::written));

  public Class<? extends DeclaredFormat> format() { return SzxFormat.class; }

  public Bindings bindings() { return Bindings.of().on(CovoxPeripheral.class, TABLE); }

  public List<Fixed> blocks() { return List.of(COVX); }

  public void plug(SnapshotFile file, Speccy speccy) {
    if (file.has(COVX) && speccy.peripheralRegistry.find(CovoxPeripheral.class) instanceof CovoxPeripheral covox)
      covox.plugIn(true);
  }
}
```

A mapping says which format it is for, the block it writes, the table, and what a file that has the block
plugs in. The format asks which mappings are for it; the peripheral is never asked anything about files.
`@OnlyWith` means the mapping exists only while the Covox plugin is installed, so a missing peripheral
never breaks the reading of a file.

Twenty-two parts are kept this way, most in under a hundred lines each.

Two tests hold it. One plugs the parts in, saves, loads into a fresh machine and compares what each part
holds: thirty-four cases. The other is the fixed point: a desk is kept, brought back on another desk and
kept again, and every window and every file of the second has to equal the first, byte for byte.

## Adding yours

- **A format**: a class that declares its places, its tables and its shapes, and answers for an extension.
  It is one of the ways in on [Plugins](Plugins).
- **Your peripheral's state**: give the peripheral the two methods that say and set what it holds, and write
  a mapping like the one above, with a block name of your own. The mapping is a role like any other, so a
  plugin can bring one.

I have not yet built a peripheral outside the tree that brings its own mapping, so that last step is
described from the ones inside it.

Next: [Build a peripheral](Build-a-peripheral).
