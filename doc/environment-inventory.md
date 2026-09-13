# Inventory of the emulation environment

Measured on 2026-09-06 on branch `nucleo-generado`, HEAD `f03d69c07`, with uncommitted changes in the working
tree (see §6.1). This is the material gathered to rewrite the README, kept whole so that none of it is lost:
the synthesis, the discrepancies and the decisions are in sections 1 to 8; the seven agent reports follow
verbatim and unedited as appendices A to G. When a fact matters, go to the path that backs it: every claim in
the appendices carries one.

## 1. Method

Seven read-only inventories run in parallel, one per area, each on a subagent of the lowest model that could
do it (the rule in `CLAUDE.md`), plus direct checks made in the session. Each agent received a set of
directories, a list of questions and the instruction to answer with paths as evidence, to invent nothing, and
to list separately what the old README claimed and the code did not have.

| Area | Scope | Model | Tool calls / duration | Appendix |
|---|---|---|---|---|
| Desktop and windows | `machine/app`, `machine/ui`, `machine/devices/kit`, app resources | opus | 61 / 302 s | A |
| Emulation core | `machine/core`, `machine/spectrum`, `zx-rzx`, `doc/plan-configuracion.md` | sonnet | 66 / 584 s | B |
| Peripherals | `machine/devices/*`, `doc/plan-modulos.md` | sonnet | 39 / 377 s | C |
| ZXInfo catalogue | `machine/zxinfo` and its callers in `machine/app` | sonnet | 80 / 428 s | D |
| Generated core and performance | `doc/plan-nucleo-generado*.md`, `doc/plan-superar-zxspin.md`, `doc/TABLA_ALU_TESTS.md`, `machine/generated`, `machine/bridge` | opus | 41 / 436 s | E |
| Object-oriented Z80 | `emulator/src/main`, `emulator/src/test`, `emulator/pom.xml` | sonnet | 72 / 460 s | F |
| Commit history | the whole `git log`, 1389 commits since 2024-11-05 | sonnet | 9 / 409 s | G |

### 1.1 Direct checks (made in the session, not by the agents)

- Maven modules: 44 `pom.xml`. Java files per module (excluding `target/`): `emulator` 278, `machine/app` 44,
  `machine/bridge` 71, `machine/core` 177, `machine/devices` 141, `machine/generated` 20, `machine/spectrum` 22,
  `machine/ui` 23, `machine/zxinfo` 24, `zx-rzx` 18, `translation` 199.
- `doc/`: `configuracion.json`, `plan-configuracion.md`, `plan-formas-c.md`, `plan-modulos.md`,
  `plan-nucleo-generado-jvm.md` (71 KB), `plan-nucleo-generado.md` (47 KB), `plan-nucleo-generado-modulo.md`,
  `plan-superar-zxspin.md` (30 KB), `TABLA_ALU_TESTS.md`,
  `TABLA_ALU_TESTS_EJEMPLO.md`, `manicminer.z80`, and the gifs `zxenv1.gif` (3.3 MB), `zxenv2.gif` (7.0 MB),
  `jsw1.gif`, `dan-95%.gif`, `wally-90%.gif`.
- Remote: `git@github.com:fpetrola/oozx.git`. CI: `.github/workflows/maven.yml`, "Java CI with Maven", JDK 18
  temurin. The root `pom.xml` compiles with source/target 18; `machine/generated/pom.xml` with 21;
  `prototypes/sprite-inflate` with release 17. According to the memory of earlier sessions the whole reactor
  compiles on 21 and on 25.
- Machine classes in `machine/core/.../speccy/machine/`: `Spec48`, `Spec48Ntsc`, `Spec128`, `SpecPlus2`,
  `SpecPlus2A`, `SpecPlus3`, `SpecPlus3E`, `Pentagon` (plus `SpecPlus3Constants`, `Spec48RamInfo`). No Timex,
  Scorpion or SE class.
- 3D sprite viewer: commits `765ac1e4d` "3d viewer" and `7d3641884` exist, but in `machine/app`, `machine/ui`
  and `machine/core` the word "sprite" only appears in prose in `ScreenEffects` and `ScreenSettings`. What is
  left of it is most likely `prototypes/sprite-inflate`; not verified.
- `zx-rzx` has `RzxWriter.java` besides the parser and the playback: writing RZX exists at file level.
- `OOSpectrumLauncher.main(String[] args)` ignores `args`: nothing opens from the command line. The URL the
  launcher downloads and unzips (`filename.contains("http")`) comes from the game browser, not from the console.
- `emulator/src/test/resources/fuse/tests.in`: 1356 blocks with a test id. The documents say 1355, and the
  generated core's total "1592 pass" adds up with 1355 + 226 (emuStudio) + 11 (ALU reference).
- The gifs: `zxenv1.gif` (1754×982, 826 frames) shows the desktop with several emulators, the snapshot
  history, the game browser with thumbnails, the pokes dialog ("Game Cheats/Pokes") and the details dialog
  ("Trantor: The Last Stormtrooper"). `zxenv2.gif` (1210×851, 1990 frames) starts on an empty desktop, opens
  the history, two emulators, searches "dizz" in the browser and loads a Dizzy.
- Memory of earlier sessions (`measurement-discipline`, `build-and-test-workflow`), not re-verified today:
  the app on `jsw-full.rzx` gave session 26.9 k% before and 26.3 k% after the move to a module, playback 21.2
  vs 20.8 k%, OOP 4.5 vs 4.4 k%; on 2026-09-05 with JDK 21 unpinned, generated 19,973 % playback and 19-22 k%
  session, OOP 3,558 % and 3.7-4.3 k%; pinned to 2 CPUs 14-21 k% vs 3.1-3.7 k%. The "25,000 %" that was
  remembered is recorded nowhere.

## 2. Synthesis by area

### 2.1 The object-oriented Z80 (`emulator`, appendix F)

- 76 instruction classes in `instructions/impl`, each with `execute()`, `getLength()` and
  `accept(InstructionVisitor)`. 104 files under `instructions` in total (76 impl, 17 types, 8 cache,
  3 factory). Intermediate types by shape: `TargetInstruction`, `SourceInstruction`,
  `TargetSourceInstruction`, `ConditionalInstruction<C>`, `BlockInstruction`, `RepeatingInstruction`,
  `BitOperation`, parameterised ALU instructions.
- Operands as objects: `Register` is an `OpcodeReference`; `Memory8/16BitReference` (immediates),
  `IndirectMemory8/16BitReference`, `MemoryPlusRegister8BitReference` ((IX+d)), `ConstantOpcodeReference`,
  `NullOpcodeReference`. Conditions: `Condition`, `ConditionFlag`, `ConditionAlwaysTrue`, `BNotZeroCondition`.
- Decode tables along Cristian Dinu's decomposition (`opcodes/decoder/table/*TableOpCodeGenerator`), arrays of
  prototype instructions, prefixes included. `InstructionCache` clones per PC with `InstructionCloner` and a
  `MemoryWriteListener` invalidates on self-modifying code.
- Visitor `com.fpetrola.z80.base.InstructionVisitor<R>` (~90 methods). Existing visitors: the three cloners,
  `MemptrUpdater`'s `Before`/`After`/`Repeating`, and `com/fpetrola/z80/tstates/PhaseProcessor`. There is no disassembler
  as a visitor: text comes from `toString()`.
- Registers: `Plain8BitRegister`, `Plain16BitRegister`, `RRegister`, `Composed16BitRegister`,
  `PlainComposed16BitRegister`, `InvertedComposed16BitRegister`; `RegisterBank`, `RegisterName` with `MEMPTR`
  and `VIRTUAL`. `UnrolledRegisterBank` backs every register with an `int` "so a generated core can read and
  write them without going through an object". Flags through tables in `AluOperation` classes nested in each
  instruction, with `CachedTableAluOperation` (up to 256×256×256 ints, static `TABLE_CACHE`).
- Memory contract `com.fpetrola.z80.memory.Memory`: `read(address, fetching)`, `write`, read and write
  listeners, `peek`/`poke` with no time. Contention is neither in `Memory` nor in `Instruction`:
  `PhaseProcessor` is an `InstructionVisitor<Integer>` and an `ExecutionListener`, plugged in through three
  seams (executor, memory listeners, the `AddStatesIO` decorator). Its Javadoc: "the only knowledge of the
  Spectrum's timing in the emulator; the Z80 model does not know this class exists". MEMPTR is an ordinary
  `Register` kept by `MemptrUpdateInstructionSpy`, attached the same way.
- The generic `Instruction<T extends WordNumber>` no longer exists in this module: it was removed on purpose
  (commits "remove generic type parameters..."). `DirectAccessWordNumber` and `ReturnAddressWordNumber` remain
  in `se/` for return-address provenance. Full symbolic execution lives in `translation/`.
- Tests: the CPU vector battery (`fuse/FuseTests.java`, `tests.in` 9152 lines, `tests.expected` 18912),
  `AluReferenceTest` (up to 131,072 combinations per instruction; its Javadoc documents 4 bugs the vectors did not see),
  `AllTableAluOperationsCompatibilityTest` (MD5 of 41 tables), emuStudio (`net/emustudio/...`, Peter Jakubčo's
  `cpu-testsuite_12.0`, 226 `@Test`; `Z80Tests` with 215 opcode/CRC16 entries). No zexall/zexdoc.
  `ProcessorUnderTest` is the seam: `ServiceLoader.load(ProcessorUnderTest.class)` with `Oop` as default.

### 2.2 The machine (`machine/core`, `machine/spectrum`, `zx-rzx`, appendix B)

- Machines: 48K, 48K NTSC, 128K, +2, +2A, +3, +3e (IDE-oriented ROMs, no snapshot model), Pentagon (no
  contention, no floating bus, without its built-in Beta). All eight in `EmulatorModule.MODEL_NAMES`.
  The Timex, Scorpion and SE frame tables were carried by `TimingsHandler` and used by nobody; both went on 2026-09-12 (`MachineTimings` is now the record, with the five frames a machine here has):
  scaffolding, not machines.
- Memory: 2 KB page table, `map16k/8k/4k/2k`, a `contended` flag per page, `writableRoms`. ROMs from the
  classpath `/roms/<name>` with fallback to a file and to the standard ROM. Paging `Paging128` (0x7ffd) and
  `PagingPlus3` (0x1ffd). `ContendedMemory` shared with the fast core.
- Tape: TAP, TZX, CSW read (TZX block 0x19 not supported; no PZX, no WAV). Writing: TZX blocks 0x18 (CSW) and
  0x15 (direct recording). The real auto-load: `TapeAutoLoader` types `LOAD ""`, runs at 20,000 % and drops to
  100 % afterwards, on top of `PcTraps`. `TapeHardware` picks the machine a TZX declares. `TapeBlock` parses for
  the cassette browser.
- Disk: `Disk.Type` declares UDI, FDI, TD0, MGT, IMG, SAD, CPC, ECPC, TRD, SCL, OPD, D40, D80, LOG. Read and
  write: MGT/IMG/OPD, TRD, SCL, D40/D80. Read-only: CPC/ECPC (.dsk). UDI/FDI/TD0/SAD throw "images are not read
  yet". Controllers: `UpdFdc` (uPD765) in `SpecPlus3`; `WdFdc` (WD1793) in `Beta128Peripheral` (TR-DOS, built
  into the Pentagon, pluggable on 48K/128K). `Fdd` models the drive and write protection.
- Snapshots (`machine/spectrum`): SNA, Z80, SZX, SP, all four with `load()` and `save()`. `SnapshotSZX` skips
  the ZXATASP/ZXCF/SIMPLEIDE/COVOX/BETA128/MOUSE/PLUSD/SPECDRUM/MICRODRIVE blocks on read. Embedded tape in SZX.
  `SnapshotUnicodePacker` packs a gzip+Base64 snapshot inside the configuration JSON (the session).
- RZX (`zx-rzx`): `RzxParser`, `RzxWriter`, `RzxPlayback` closes the frame by counting M1 fetches through the
  R delta, `RZXPlayerIO` substitutes the INs and keeps sync statistics. `goLive()` hands back the real keyboard
  and can record; `RzxWriter.Mode` allows extend, splice and prepend. No interactive rewind. `RzxArchive.java`
  (in core) catalogues ~4291 third-party recordings for the browser.
- Sound: `Beeper`; `Ay` derived from the reference emulator with a timestamped write queue, `AyPeripheral` (128/+2) and
  `AyPlus3Peripheral` (+3). **AY output is mono**: `Ay.mixInto` adds the three channels equally into both
  ears; `Options.enumerateSoundStereoAY()` returns 0. Generic `Dac`/`DacDevice`. Backend `JavaSoundDevice` with
  `BlipBuffer`, 44100 Hz, equalisation presets, `dropWhenAhead`.
- Video: `Ula` with per-T-state contention tables, `UlaPeripheral` (partial decode) and
  `UlaFullDecodePeripheral` (Pentagon). `Display` draws each cell as the beam passes and keeps the border as a
  list of changes by beam position; flash every 16 frames. `Picture` is an `int[]` the window wraps without a
  copy. No snow, no scanlines in core (scanlines are in `machine/ui`), no Timex modes. Floating bus per model
  (`hasFloatingBus()` false on +2A/+3/Pentagon).
- Input: `Keyboard` with the full matrix, issue 2/3 in `Machine.Config.issue2`,
  `Input.Config.recreatedSpectrum` wired. Joysticks in `Joystick.java`: Cursor, Kempston, Sinclair 1/2,
  Timex 1/2, Fuller. Two 15-button pads in `Input.Config`.
- Peripheral bus: `Peripherals` implements `PeripheralBus`: a per-port cache (0x10000 entries) of which
  `PortHandler`s answer, wired-AND of answers with a "driven" flag, floating bus for undriven bits, `Pluggable`
  devices answer before the machine's own chips, `busListeners` for snoopers. `ContendedPeripheralBus`
  decorates with port contention. `Speccy.create()` is the composition root.
- Configuration: a `@Section` per class, one JSON at `~/.oozx/config.json`, unknown sections preserved.
  Sections in core: `machine`, `machine.48k/128k/plus2/plus2a/plus3/plus3e/pentagon`, `memory`, `sound`,
  `speed`, `input`, `beta128`, `floppy`, `desktop`. Guard: `SectionsAreDeclaredTest`.
- Fast core: `OptionalBinder<Core>` with `OopCore` as default; the `generated` module replaces it from the
  classpath. Hot switch through `Z80.useProcessor`; the preference is `OOZxConfiguration.processor`.
- Pacing: `Timer.Config.emulation` in percent (default 20000, `UNLIMITED` 1,000,000); sound sets the pace;
  `Scheduler`/`Task`/`Timetable` for events at a T-state.

### 2.3 Peripherals (`machine/devices`, appendix C)

- 22 peripheral modules plus `kit`, `ide` and `all`. 8,055 lines in the 22; 10,429 with the infrastructure.
  None has a TODO, a stub or an `UnsupportedOperationException`. All 22 are done, and five more are planned
  with no directory yet: Currah uSpeech, Currah uSource, SpeccyBoot, Spectranet,
  TTX2000S.
- Discovery: two independent `ServiceLoader`s. `Extension` (a Guice module, installed by
  `EmulatorModule.configure()`) contributes `Peripheral`s to a `Multibinder` and optionally a configuration
  section. `Equipment` (`name()`, `open()` → `DeviceFrame`) is loaded by `ZXSpectrumDesktopApp` and becomes an
  entry of the Equipment menu. Two one-line service files per jar. `devices/all` has no sources: it depends on
  the 22 jars so the classpath carries every service file; `machine/app` depends only on it.
- `Peripheral` contract: `activate(machine)`, `deactivate()`, `getPorts()`, `fitsOn(machine)`, `isWanted()`,
  `hasHardReset()`; `Pluggable`: `plugIn(connected)`, `isPluggedIn()`.
- Window shapes in `kit`: `DriveBayFrame` (one bay per drive: a `MediaSlot` with motor LED,
  Insert/Eject/New/Save/Flip/Write-protect, a paged-ROM lamp, the board's own button, an expanded view with
  track per head), `IdeBayFrame` (one slot per disk, Save is the commit), `DacFrame` (level meter and volume).
  All work goes to the emulator thread through `z80.later(...)`.
- The smallest: `zxmmc`, 3 files, 100 lines (`ZxmmcPeripheral extends MmcBoard`; `ZxmmcDevices` 6 lines;
  `ZxmmcEquipment` 8 lines), plus two service files. Without a window: `melodik` 2 files/110 lines, `joystick`
  4/206.
- Table module → hardware → window → lines: appendix C §1. The ones with no window: `joystick` and `melodik`
  (settings flag only). `multiface` registers three `Equipment`s (One/128/3) over one `MultifaceFrame`. So the
  Equipment menu has 22 entries but they are not the 22 modules: 20 modules with a window + 3 Multiface − 1.
- Design rule (`plan-modulos.md`): "not how much a peripheral knows, but who knows whom"; a peripheral may use
  the ULA, the mixer and the clock; the emulator never names a peripheral; one-way dependencies
  base → ui → devices → app, enforced by the compiler. What the machine brings stock (the 128/+3 pager, both
  ULAs, the +3's FDC, a 128's AY) stays in core: "that is not plugged in, that IS the computer". Numbers:
  on 2026-09-01 core had 263 files, 51 with Swing; 338 tests (260/16/33/29). On 2026-09-02 against the reference
  emulator: 23,572 lines of C and 47 peripheral types.

### 2.4 Desktop and windows (`machine/app`, `machine/ui`, appendix A)

- MDI: one `JFrame` (`ZXSpectrumDesktopApp`) with a `JDesktopPane`; everything else is a `JInternalFrame`.
  Several emulators at once, each `EmulatorInternalFrame` with its `EmulatorCore`, its toolbar, its status bar
  and its model combo. Cascade (Alt+1) and Tile (Alt+2).
- Session: main window bounds and the list of open windows in `~/.oozx/config.json`; restores EMULATOR (from
  an embedded packed snapshot, the machine comes back running), GAME_BROWSER and SNAPSHOT_HISTORY; persists
  turbo/mute/pause and applied pokes per emulator.
- `AttachedFrame` (`machine/ui`): a satellite window clips onto a machine's window, follows it, stays in
  front, closes with it. Sticky edges of 40 px decided on mouse release, a glow line that previews the snap,
  windows sharing an edge re-share it, compact/expanded fold, a progress bar under the buttons. Tests:
  `AttachedFrameTest`, `PrinterDockingTest`, `RzxPlayerDockingTest`, `CollapseSizesTheFrameTest`,
  `SlidersUnderTheButtonsTest`.
- Keyboard: a global `KeyEventDispatcher` sends keys to "the machine in front, or the machine the thing in
  front is clipped to", suppressed while a text field, combo or modal dialog has focus.
- `DeviceFrame`/`Equipment` (`kit`): **clipping the window onto a machine's window is what plugs the device
  in; unclipping or closing it unplugs it**, on the emulator thread, with a reset when the memory map changes.
- Menus: File (Open Ctrl+O, Recent Ctrl+1..0, Save State Ctrl+S, Load State Ctrl+L, Quit Ctrl+Q); Emulator
  (New Ctrl+N, Open Tape, RZX Player, Cassette Browser Ctrl+T, Equipment via `ServiceLoader`, Real Cassette,
  Joystick, Game Browser Ctrl+B, Pause Ctrl+Space, Turbo Ctrl+T, Mute Ctrl+M); Options (Settings, TV: RGB
  Monitor / Scart / Composite / Aerial, Scan lines); Window (Close Ctrl+W, Close All, Cascade, Tile,
  Look&Feel); Help (README F1, About). Main toolbar: New Emulator, Game Browser, Snapshot History, Favorites,
  Settings. Per-emulator toolbar: turbo (right click: two-segment slider 25 %–40,000 %), border, play/pause,
  mute (right click: volume), pokes, details, fullscreen (Esc leaves), zoom 1x/2x/3x, `.z80` snapshot, screen
  settings, favourite. Status bar: speed, model combo, indicators.
- Windows: Game Browser, Game Details (9 tabs: General, Technical, Publishers, Authors, Description,
  Screenshots, Game map, Releases, Downloads), Snapshot History (double click loads, "View Details" queries
  ZXInfo), Favorites (game or recording, remembers the entry inside the zip), Pokes (search over `.pok`, a
  checkbox per modification, Apply/Clear, un-applying restores the previous bytes) with 3683 `.pok` files in
  `machine/core/src/main/resources/pokes/`, README viewer (commonmark), About, Download viewer (one file),
  Game-not-found, RZX Player (Open, Play, Stop, **Take Over** = `RzxSession.release()`, Loop, Favorite, table
  of blocks with progress; the picture goes in an emulator window "Spectrum #N"; several at once; multi-part
  zips ask which part), Cassette Browser (blocks, progress, Play/Pause/Stop/Open, works without an emulator,
  one deck per machine, clipped on), Audio-in (oscilloscope of the sound card input, line selector, zoom,
  follow the head; a monitor today, does not feed the EAR), Joystick (direction/fire as the Kempston port reads
  them, gamepad name), Screen settings (self-describing knobs from `ScreenSettings`: Scaler, Show border,
  Lead, Scan lines, Phosphor layout, Phosphor depth, Persistence, Tint, Brightness, Colour depth; named
  profiles, "Use as default", "Restore default"). Scalers: Nearest, Bilinear, Sharp bilinear, Scale2x,
  Scale3x, Edge directed, xBRZ.
- Device windows: appendix A §3 and C §1: ZX Printer (paper, tear off, PNG, zoom), parallel printer
  (continuous paper, text), Interface 1 (eight Microdrives with motor lights, shadow-ROM lamp, RS-232
  terminal, ZX Net plugs), Interface 2 (cartridge and two sockets), Kempston Mouse (the desk mouse moves over
  the picture, "Hold" captures the pointer, sensitivity, swap, "Record"), DivIDE/DivMMC (EPROM, write-protect
  jumper, NMI), Fuller (joystick socket and write light). No photos of the hardware: Swing widgets and SVG
  icons.
- Look and feel: Darcula, One Dark, Solarized Light, Solarized Dark, IntelliJ (DarkLaf) and Metal; saved and
  re-applied; the launcher installs Solarized Light before reading the config. No automatic light/dark
  detection.
- Input: `SwingKeyboard` is not an on-screen keyboard, it is the `KeyListener` mapping AWT → keysyms. Physical
  gamepad through Jamepad/SDL onto the Kempston of the machine in front. Mouse capture only in the Kempston
  Mouse window.
- Files: filter `.tap .tzx .z80 .sna .szx .rzx` (RZX also `.zip`); disk images per device
  (`DiskInterface.imageExtensions()`); `.csw` treated as a tape. `open(path)` routes recordings to the RZX
  player and everything else to a new emulator. No drag and drop. Recent files persisted.
- **Settings dialog** (`SettingsDialog`, 7 tabs): only border and scanlines in Video, volume in Audio, the
  model in Machine, "processor implementation" and turbo-by-default in General are wired. The rest
  (`setAudioOption`, `setInputOption`, `setStorageOption`, `setPeripheralOption`) falls through to
  `MockEmulatorCore` and prints to stdout. The real mechanisms are the device windows and `ScreenSettings`.

### 2.5 ZXInfo catalogue and recordings (`machine/zxinfo`, appendix D)

- Base `https://api.zxinfo.dk/v3`. Used: `/search` (only `query`, `machinetype`, `genretype`, `size=150`,
  `mode=compact`), `/games/{id}?mode=full`, `/metadata/` (for the machine and genre combos). Implemented and
  never called: `/suggest/{term}`, `/suggest/author`, `/suggest/publisher`, `/filecheck/{hash}`.
- `GameEntry` carries a lot (releases, screens, additionalDownloads, magazineReferences, youTubeLinks, tosec,
  md5hash, ...); `GameDetail` keeps a subset. Never populated: `machines`, `memoryRequired`, `description`,
  `coverImageUrl`, `rating`. Never shown: votes, YouTube, magazines, related, TOSEC. Screens are not typed
  (loading/in-game/inlay go into one list).
- Use: search only on Enter/button (no type-ahead); server filters machine and genre; client filters
  RZX/Map/Loadable over the same 150; only `contentType=="SOFTWARE"`; the first two screens per row with an
  external placeholder when missing (`i.sstatic.net/wAz1X.gif`). Context menu: Load Game (submenu of the
  8 machines in `EmulatorModule.MODEL_NAMES` or "as the file says"), Load Version, View Details, Add to
  Favorites, Download, Play Recording.
- Loading: URL → `DownloadAndUnzip.unzip()` into `${java.io.tmpdir}/zxinfo_extracted`, always treated as a
  zip, no disk cache. File priority (`scoreOf`): `.rzx` 40 > `.z80/.sna/.szx` 30 > `.tzx/.tap` 20 > `.csw`
  10; +5 if the name has "48", −5 for "128", −50 under `/denied/`, −2 for "alternate/different"; `.dsk` is
  not loadable. The machine is picked from the TZX block 0x33 (`TapeHardware`), else "128" in the name, else
  48K.
- Images without a cache (`ScreenshotPair`, `LazyImageIconLoader`, `GameDetailsDialog.loadImageAsync`).
- Favourites with a `gameId` that is saved and never read. Pokes by fuzzy name match (`PokesManager`,
  Levenshtein), unrelated to ZXInfo ids.
- Recordings: `RzxOption` joins ZXDB's `additionalDownloads` and `RzxArchive` (Javadoc: 26 in both, 15
  ZXDB-only, 108 archive-only, over the sample it describes).
- Oddities: the method doing the real search is called `createMockResults`; the rating says "0-100 to 0-5"
  but only rounds `score`; "Play Game" in the details dialog only shows a message; "Download" in the browser
  says "coming soon"; `GameSearchResult`, `GameBrowserListener`, `DownloadAndUnzip`, `RzxOption` live in
  `app`/`core` although the `zxinfo` pom says the emulator does not know it.

### 2.6 Generated core and performance (appendix E)

- What it is: the OOP model specialised by partial evaluation over the instance graph. `GeneratedCores`
  builds a silent `Speccy` on `ModelCore` and hands `CoreGenerator` the instruction instances of the tables,
  the MEMPTR aspect (`MemptrUpdater`, resolved by double dispatch), the contention (`PhaseProcessor`) and
  the machine's memory as `read`/`write` helpers. Specialiser in
  `machine/generated/src/main/java/com/fpetrola/z80/generate/{Specializer,CoreGenerator,Simplifier,Folder,SourceIndex}`.
- Boundary (`plan-nucleo-generado-jvm.md`): freeze what cannot change while the instance lives
  (`ram, mapRead, mapWrite, ula, clock, display, io, noMreqRun2..7` as `final`); read on every access
  everything the machine mutates (page table, contention tables, T-state counter). Justification: "the whole
  banking model of this machine is 'mutate the page table'".
- Cycle (since 2026-09-07): committed, as a source of the module. The build writes it again only when the
  model changed - the file's first line says which model it came from, and the build hashes the model in
  under a second - and compiles it itself, because at that point in the build the compiler has already run.
  A machine loads the class off the classpath; nothing is generated or compiled when the emulator runs. Key =
  SHA-256 of `META-INF/model-sources` + `com/fpetrola/z80/generate` + `com/fpetrola/oozx/generated`, the
  generated core itself left out, 16 hex.
- Size of the reference copy (`machine/generated/src/main/java/.../GeneratedSpectrumZ80.java`): 27,345 lines,
  955,776 bytes, 1,792 `case 0x`, 231 `decode*` methods, 1,036 `read(`, 604 `write(`, 1,328 `contendNxM(`,
  6 contention helpers (1x1, 1x3, 2x1, 4x1, 5x1, 7x1), 926 mentions of MEMPTR.
- Discovery: `META-INF/services/...Extension` → `GeneratedCores` binds `Core` through
  `OptionalBinder.setBinding()`. The OOP core is used: under a wiring that counts its own T-states
  (`ProcessorWiring.takesACoreThatReachesMemoryItself()` false, which is the harness's `CountingWiring`),
  without `javac`, without `META-INF/model-sources` in the jar, or with "Fast Core" off.
- Measurements (all in appendix E §2 with conditions): the ones that survive the noise are the 4.5× of the
  micro benchmark (`int[]`, one JVM per core, JDK 21.0.2, ±8 %), the **4.3× over OOP and +10 % over the pure
  core** in `doOpcodes` with Manic Miner from a snapshot on one P-core (3,587/3,445 vs 3,293/3,131 vs 790 fps),
  and the diary in `plan-superar-zxspin.md`: §0 everything on 13,333 fps MM / 7,939 JSW → §5 15,256 / 11,896
  (+14 % and +50 %) → §6 JSW 12,867. Pacing with sound: 30,000 % requested → 14,177 fps (28,355 %, the
  machine's maximum). Move to a module at parity: playback 21,186 % → 20,773 %, session 26,942 % → 26,253 %.
- Declared noise floors: ±12 % unpinned; ~1 % pinned with the plateau method; ±3 % on the RZX harness; ±8 %
  micro; ±15 % between equal runs on the notebook; two equal `Where` runs differed by 12 %.
- **No measurement of the reference emulator or ZXSpin exists in the repository.** The first line of
  `plan-superar-zxspin.md` ("runs at the reference emulator's speed. ZXSpin runs 20 % faster than it") is a premise. The three
  "advantages over the reference emulator" are reasoned from the C in `fuse-emulator-fuse/`; the third (dirty
  per cell) was discarded.
- Steps in order (appendix E §3): A aspects out of the model (`49de2b1d3`, −73), B contention as values with
  one hook (`24775046d`, −537), D MEMPTR with one owner (`f395170d5`, −50), specialiser rules 1–8
  (`16d29ae4d`), weaving of the aspects, a hand-written ~60-line frame and nested `switch`es split into
  16-opcode methods, plug-in and measurement (`d6a5fdbc2`), the `GeneratedZ80IsCurrentTest` lock; C (fetch
  and executor) **not done**. JVM phase: A3+A2 (−9.2 % bytecode), register bank keeping its width (302
  `A & 0xFF` → 0), A4 (`== -1` 273 → 14, −11.1 %), A6 `GROUP_SHIFT`, M1/M2/M4 no movement, G1–G3 the
  machine's memory inside (+10 %), contention unrolled (3,815 vs 3,500), `groupShift` to the `Target`. Layer
  phase: §1 contention runs (+8..+16 % MM), §2 clock without timeout (+3..+6 % MM, +10 % JSW, bytes/frame
  951 → 756), §3a one question per write (+3 %/+7 %), §3b `byte[]` pages (JSW +48 %, exposed a signed
  compare), §4 ports and keyboard without allocating (JSW +7 %, bytes/frame → 671), §5 screen (17.4 % →
  0.9 % of own time, fps unchanged, per-cell redesign discarded), §6 AY event to event (JSW +9 %), §7 loop
  neutral, §9 sound as the pacer. Module phase (2026-09-05): `machine/generated`, the `Core` seam, `OopCore`
  through `OptionalBinder`, cache.
- Why the OOP model allows it (quotes in appendix E §5): "the model does not adapt to the tool"; the instance
  graph is fixed, the situation of a partial evaluator; the aspects are visitors, "the shape a specialiser
  resolves best"; "the generated core is, literally, the model with its aspects woven and flattened"; one
  mechanism, no per-instruction templates; "what the generator inlines arrives with its identities resolved;
  what the JIT inlines does not".
- Caveats (appendix E §6): the `Where` harness is not in the repository; C not done (three debts); three
  harnesses gave three answers to the same change; with the generated core at 8.8 % of the JSW frame "making
  the CPU infinitely fast gives 10 %"; **a hole with no gate**: `GeneratedSpectrumZ80.read/write` index
  `mapping.page[…]` with no guard, so a `DevicePage` (Opus, the planned uSpeech) is never reached on the
  generated core, and `OpusTest` and the run on the generated core "never cross" (open as of 2026-09-06); the generated core reports no MR/MW/MC events; the audio line opens mono and the mixer
  writes interleaved L/R ("effective resolution 22 kHz"); the MEMPTR aspect is not installed by the machine
  on the OOP core but is woven into the generated one.

### 2.7 The test net

| Battery | Size | What it fixes | Where |
|---|---|---|---|
| The CPU vector battery | 1355 tests (docs), 1356 blocks in `tests.in` | registers, memory, MEMPTR, total T-states and the exact list of bus events with their instant | `emulator/src/test/java/fuse/FuseTests.java` |
| `AluReferenceTest` | 131,072 combinations per instruction | against the published Z80; found 4 instructions that passed the battery and were wrong | `emulator/src/test/java/com/fpetrola/z80/AluReferenceTest.java`, `doc/TABLA_ALU_TESTS.md` |
| `AllTableAluOperationsCompatibilityTest` | MD5 per table (RLA 512, BiAndBoolean 131,072, Tri 16,777,216) | that a table did not change | `table_alu_operations_config.json` |
| emuStudio | 226 `@Test` | third-party suite | `emulator/src/test/java/net/emustudio/...` |
| The three on the generated core | "the 1592 pass" | `ProcessorUnderTest` via ServiceLoader | `machine/generated/src/test/java/.../EmulatorOnTheGeneratedCoreTests.java` |
| Machine suite on the generated core | gate 260 of core; modules 338 (260/16/33/29); app 39 | the machine | `MachineOnTheGeneratedCoreTests.java` |
| ROM boot on both cores | 300 frames; AF..R, MEMPTR, T-states and all of RAM; plus 150+150 switching processor | that the two cores are the same machine | `GeneratedMachineCoreTest.java` |
| Freshness | text equality with the reference copy; `@Slow` | that the artifact is what the model produces today | `GeneratedSpectrumZ80IsCurrentTest.java` |
| Contention on the machine | `ZXSpectrumContendedMemoryTests` 64, `ZXSpectrumULATests` 17, `ContentionRunsTest` six machines from every T-state | T-state by T-state | `machine/bridge/src/test/java/model/tests/cpu/`, `machine/core` |
| `EmulationRegressionTest` | hashes after 200 boot frames | screen, sysvars, RAM, registers | `machine/core` |
| RZX | fetches per frame; 20 `@Test` | determinism | `zx-rzx` |
| Bridge to the reference emulator | 212 `@Test` in 14 classes | `LibretroCore` (JNA to `fuse_libretro.so`) and `LocalLibretroCore` behind the same interface, one command protocol | `machine/bridge` |

## 3. What is distinctive (what most emulators do not have)

1. Several machines on one desktop, each with its model, speed, sound, pokes and screen; the keyboard goes to
   the one in front or to the one the front window is clipped to; the gamepad to the one in front.
2. The session comes back with the machines running where they were (snapshot embedded in the config), pokes
   re-applied.
3. Peripherals are windows plugged in by clipping them onto the machine's window; unclipping unplugs; they can
   be carried from one machine to another; snap glow; shared edges; folding.
4. The front panels: bays with motor LED, Insert/Eject/New/Save/Flip/Write-protect, the board's own button
   (Boot, SNAP, NMI, red button), paged-ROM lamp, IDE bays, level meters, the ZX Printer's paper, Interface 1
   with eight Microdrives and an RS-232 terminal, Interface 2, the mouse window with Hold, the DivIDE jumpers.
5. The ZXInfo catalogue inside: search, server and client filters (RZX/Map/Loadable), direct load choosing the
   machine or letting the TZX choose, version choice, tabbed details, favourites.
6. Recordings from two catalogues (ZXDB + RZX Archive); a player with several at once, loop, and **Take Over**.
7. A cassette deck per machine, inspection without a machine, audible loading, auto-load at 20,000 %, a real
   cassette through the sound card with an oscilloscope.
8. The screen modelled as a TV through a lead (RGB / Scart / Composite / Aerial), phosphor, persistence, tint,
   scalers up to xBRZ, self-describing knobs, named profiles.
9. Speed 25 %–40,000 % with sound as the pacer; the processor (OOP / generated) switchable while running.
10. 3683 pokes embedded, applied and un-applied one by one, persisted per window.
11. A physical gamepad as the Kempston; a joystick window.
12. For development: a bridge running the reference emulator's real core behind the same interface; contention from every
    T-state; RZX as a determinism test; the generated core running the same batteries without knowing.

## 4. What the README must not claim

From the old README, without backing in the code (union of the "Claimed but not found" lists in the appendices):

- Automatic light/dark theme (manual only). Filters "HQ2x, HQ3x, Dot Matrix, PAL TV, motion blur" (the combo
  lists them, `setVideoOption("filter")` does nothing; the real ones are the `ScreenSettings` knobs). Contrast,
  4:3 / pixel-perfect aspect, custom resolution.
- Everything in the Audio, Input, Storage, Machine, Peripherals tabs and almost all of General in the Settings
  dialog.
- "Create snapshots" in the File menu (it is on the emulator toolbar). A window list in the Window menu.
  Download viewer as a download manager. "Zoom" in the screenshot gallery. On-screen keyboard.
- ZXInfo: instant search, cover art, description, per-model compatibility, "Play" from details, downloads.
- Core: stereo AY with separation, snow effect, scanlines in core, sound quality levels, Timex / Scorpion /
  SE, the UDI/FDI/TD0/SAD formats, PZX/WAV, TZX block 0x19.
- RZX recording from the UI (exists at file level and in `goLive()`, no controls).
- Opening files from the command line.
- Measured comparisons with the reference emulator or ZXSpin.

## 5. Discrepancies between reports and how they were resolved

- **Location of the app classes.** Appendix A cites them under `machine/app/.../peripherals/t/`; during the
  session they appeared moved to `machine/app/.../speccy/desktop/` (21 files, an uncommitted move that was not
  in the initial `git status`). Read A's paths with that shift.
- **RZX recording.** G says "playback & recording" (`b80110529`); A says the player is playback only; B finds
  `RzxWriter` with extend/splice/prepend modes and `goLive()` optionally recording. Resolution: writing
  exists, there is no recording UI.
- **3D sprite viewer.** G lists it by commits; no class remains in `app`/`ui`/`core`. Probably
  `prototypes/sprite-inflate`.
- **Scanlines.** B: none in core. A: a "Scan lines" knob in `ScreenSettings` and in the TV menu. Both true.
- **Stereo AY.** G: commit "mix in stereo so a source can sound different in each ear" (`7c5ebcd9d`). B:
  `Ay.mixInto` adds equally into both ears. The mixer is stereo per source; the AY does not position channels.
- **22 devices.** C: 22 modules, two without a window (`joystick`, `melodik`), Multiface with three
  `Equipment`s. A: 22 entries in the Equipment menu. Different sets (20 + 3 − 1). The current README says
  "the Equipment menu lists the twenty-two devices": to be corrected.
- **Screenshots per row.** D: the first two `screens[]`. The README says "a screenshot per title".
- **The vector battery: 1355 vs 1356.** Docs and the 1592 sum say 1355; the file has 1356 blocks. Which one is skipped was
  not investigated.
- **Fuller.** B sees it only as a joystick type (core scope); C has the module with the AY on 0x3f/0x5f and
  the joystick on 0x7f.
- **Kempston Mouse.** B: only a flag in `Input.Config`; the peripheral is in `machine/devices/mouse`.

## 6. Code problems found along the way

### 6.1 Affecting the README or the build
- `machine/app/pom.xml` line 156: the shade `mainClass` is `com.fpetrola.oozx.speccy.peripherals.t.OOSpectrumLauncher`,
  and the class is in `com.fpetrola.oozx.speccy.desktop` in the working tree (uncommitted move). `java -jar`
  fails until it is updated.
- CI on JDK 18 (`maven.yml`) while `machine/generated` compiles with 21.
- `OOSpectrumLauncher.main` ignores `args`.
- Ctrl+T assigned to both Cassette Browser and Toggle Turbo Mode (`ZXSpectrumDesktopApp`).

### 6.2 In the emulator and the catalogue
- `GeneratedSpectrumZ80.read/write` with no guard for `DevicePage`: Opus does not work on the generated core
  and no gate sees it.
- Mono audio line with interleaved L/R: 22 kHz effective.
- MEMPTR not installed by the machine on the OOP core; it is on the generated one.
- `Ay` mono; `Options.enumerateSoundStereoAY()` returns 0.
- TZX block 0x19 "not supported"; UDI/FDI/TD0/SAD "not read yet".
- ZXInfo: `createMockResults` is the real search; rating not divided by 20; details "Play Game" does nothing;
  "Download" "coming soon"; `/suggest` and `/filecheck` unused; no download or image cache; external
  placeholder `i.sstatic.net`; favourites' `gameId` never read; `description`/`coverImageUrl`/`machines` never
  populated; catalogue classes in `app`/`core` against what the `zxinfo` pom says.
- Settings dialog: five and a half tabs that print to stdout (`MockEmulatorCore`).
- Audio-in is a monitor: it does not feed the EAR yet.

## 7. Numbers at hand, with their source

| Figure | Value | Source |
|---|---|---|
| Peripheral modules | 22 (+ `kit`, `ide`, `all`) | `machine/devices/pom.xml` |
| Lines of the 22 peripherals / with infrastructure | 8,055 / 10,429 | `wc -l` over `src/main/java` |
| Smallest peripheral | ZXMMC: 3 files, 100 lines, 2 service files | `machine/devices/zxmmc` |
| Z80 instructions as classes | 76 (104 files under `instructions`) | `emulator/.../instructions/impl` |
| Machines | 8 | `EmulatorModule.MODEL_NAMES` |
| Embedded pokes | 3,683 `.pok` | `machine/core/src/main/resources/pokes/` |
| RZX Archive recordings catalogued | ~4,291 | `RzxArchive.java` |
| Generated core | 27,345 lines, 955,776 bytes, 1,792 `case` | reference copy in `machine/generated/src/test/java` |
| Generate / compile / load | 11.1 s / 1.8 s / milliseconds | `doc/plan-nucleo-generado-modulo.md` |
| Generated vs OOP, pure CPU | 3.5× (same JVM), 4.5× (one JVM per core) | `plan-nucleo-generado.md`, `plan-nucleo-generado-jvm.md` step 0 |
| Generated vs OOP, Manic Miner as the app runs it | 4.3× (3,587/3,445 vs 790 fps), +10 % over the pure core | `plan-nucleo-generado-jvm.md` step 5 |
| Whole machine, everything on, generated | 15,256 fps MM 48K / 11,896 JSW 128K (§5); JSW 12,867 (§6) | `plan-superar-zxspin.md`, 2026-09-03, `taskset -c 2,3`, JDK 21 |
| Maximum with sound | 14,177 fps = 28,355 % | `plan-superar-zxspin.md` §9 |
| App on `jsw-full.rzx` | session 26,942 % → 26,253 %; playback 21,186 % → 20,773 % (move to a module) | `plan-nucleo-generado-modulo.md` step 4 |
| The vector battery / emuStudio / ALU reference tests | 1,355 / 226 / 11 (= 1,592 on the generated core) | docs and `tests.in` (1,356 blocks) |
| ALU reference combinations | 131,072 per instruction | `AluReferenceTest` |
| Bridge tests | 212 `@Test` in 14 classes | `machine/bridge` |
| Contention tests | 64 + 17 + `ContentionRunsTest` (6 machines) | `machine/bridge`, `machine/core` |
| Tests per module (2026-09-01) | 338: core 260, ui 16, devices 33, app 29 | `doc/plan-modulos.md` |
| Speed slider | 25 %–40,000 % | emulator toolbar |
| Auto-load | 20,000 % while loading | `TapeAutoLoader` |
| Commits | 1,388 (1,389 with the agent's `--reverse`), 2024-11-05 → 2026-09-06 | `git log` |

## 8. Decisions taken in the README of 2026-09-06 (to be revisited)

- Focus on the emulation environment; the translation to Java stayed as one paragraph pointing to
  `translation/` and `doc/`.
- Thesis in the first paragraph: OOP down to the Z80 → three consequences (a jar per peripheral, one owner per
  concept, the generated core). The "what the design buys" sections carry the concrete evidence.
- Figures used: 15,256 fps / 12,867 fps / 4.3× / 27,345 lines / 11 s / 1.8 s / ZXMMC 100 lines / 8,055 lines
  / 1,355 / 131,072 / 226 / 212 / 300 frames / 3,683 pokes / 25 %–40,000 %.
- The sentence "speed beyond emulators written in low-level languages with hand-optimised inner loops" is
  there because it was asked for; it has no measurement in the repository (see §2.6). The bridge can run
  the reference emulator's real core through JNA: that is the way to measure it.
- Everything in §4 was excluded. The Settings dialog was left out as a feature.
- The general list compressed into "The usual, in one screen"; the distinctive things in detailed paragraphs.
- Hero: `doc/zxenv2.gif`. Build: JDK 21+, `mvn -DskipTests install`, `java -jar machine/app/target/app-0.0.2-alu-SNAPSHOT.jar`.
- Credits: the reference emulator, Cristian Dinu, emuStudio (Peter Jakubčo), ZXInfo.dk, RZX Archive. Apache 2.0 licence.
- Corrections spotted on re-reading, still pending: "the twenty-two devices" in the Equipment menu (§5) and
  "a screenshot per title" (§5).

---

# Appendices: the reports in full

The agents' text as they returned it, unedited. Paths in A under `machine/app/.../peripherals/t/` are today
`machine/app/.../speccy/desktop/` (§5).


## Appendix A. Desktop and windows (opus)

I have a complete picture. Here is the inventory.

### 1. Window model

- **MDI**: one `JFrame` (`ZXSpectrumDesktopApp`) holding a `JDesktopPane`; everything else is a `JInternalFrame`. `/home/fernando/detodo/spectrum/versions/oozx/machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/t/ZXSpectrumDesktopApp.java`
- **Multiple simultaneous emulators**: yes — `createNewEmulator(...)` per instance, each cascaded by `(count*30)%400`. Each `EmulatorInternalFrame` owns its own `EmulatorCore`, toolbar, status bar and **its own machine-model combo** (per-emulator model selection), plus per-emulator screen settings and applied-pokes list. Same file.
- **Arrange**: `Window > Cascade` (Alt+1) and `Window > Tile` (Alt+2), both reshaping all internal frames. Same file (`cascadeWindows`, `tileWindows`).
- **Persistence across sessions**: main window bounds + list of open windows saved on close to `~/.oozx/config.json`; restores EMULATOR (from an embedded packed snapshot, so the machine comes back *running where it was*), GAME_BROWSER and SNAPSHOT_HISTORY windows. Also persists per-emulator turbo/mute/paused and applied pokes. `ZXSpectrumDesktopApp.java` (`saveOpenWindows`/`restoreOpenWindows`/`saveWindowState`), `/home/fernando/detodo/spectrum/versions/oozx/machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java`
- **"Attached"/docking window infrastructure** (unusual): `AttachedFrame` — a satellite window clips onto a machine's window, follows it, stays in front of it, closes with it. Sticky edges (40px), decided on mouse-release; a glow line previews the snap; windows sharing one edge re-share it; compact/expanded fold; a progress bar under the buttons. `/home/fernando/detodo/spectrum/versions/oozx/machine/ui/src/main/java/com/fpetrola/oozx/speccy/peripherals/t/AttachedFrame.java`; tests `.../app/src/test/java/com/fpetrola/oozx/speccy/peripherals/t/AttachedFrameTest.java`, `PrinterDockingTest.java`, `RzxPlayerDockingTest.java`, `CollapseSizesTheFrameTest.java`
- **Keyboard routing**: a global `KeyEventDispatcher` sends keys to "the machine in front, or the machine the thing in front is clipped to", suppressed while a text field/combo/modal dialog has focus. `ZXSpectrumDesktopApp.typeIntoTheMachineInFront`

### 2. Menus and toolbars

All in `ZXSpectrumDesktopApp.createMenuBar/addWindowMenu/createTvMenu/createMainToolBar`:

- **File** (Alt+F): Open… (Ctrl+O) · Recent Files submenu (items get Ctrl+1…Ctrl+0, plus "Clear Recent Files") · Save State… (Ctrl+S) · Load State… (Ctrl+L) · Quit (Ctrl+Q)
- **Emulator** (Alt+E): New Emulator (Ctrl+N) · Open Tape… · RZX Player… · Cassette Browser (Ctrl+T) · **Equipment** submenu (populated by `ServiceLoader`) · Real Cassette (audio in)… · Joystick… · Game Browser… (Ctrl+B) · Pause/Resume (Ctrl+Space) · Toggle Turbo Mode (Ctrl+T) · Toggle Mute (Ctrl+M)
- **Options** (Alt+O): Settings… · **TV** submenu — radio group of TV leads (RGB Monitor / Scart (RGB) / Composite Video / Aerial (RF)) + "Scan lines" checkbox. Leads from `/home/fernando/detodo/spectrum/versions/oozx/machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/TvScreen.java`
- **Window** (Alt+W): Close Active Window (Ctrl+W) · Close All Windows (Ctrl+Shift+W) · Cascade (Alt+1) · Tile (Alt+2) · **Look&Feel** submenu
- **Help** (Alt+H): View README (F1) · About
- **Main toolbar**: New Emulator · Open Game Browser · Snapshot History · Favorites · Settings
- **Per-emulator toolbar**: turbo/rocket (right-click → two-segment speed slider 25%–40000%) · Border toggle · Play/Pause · Mute (right-click → volume slider) · Cheats/Pokes · View Game Details · Fullscreen (Esc leaves) · Zoom 1x/2x/3x · Snapshot (save .z80) · Screen settings · Add to Favorites. Status bar: speed bar, machine-model combo, pause indicator, turbo indicator.
- Note: **Emulator menu shortcut collision** — Cassette Browser and Toggle Turbo Mode are both Ctrl+T.

### 3. Dialogs and internal frames

- **Settings dialog** (modal, Esc closes), 7 tabs — `/home/fernando/detodo/spectrum/versions/oozx/machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SettingsDialog.java`: Video (border, scanlines, brightness, contrast, ULA type, display filter, aspect, scaling, snow) · Audio (volume, AY on, beeper/AY volumes, stereo, sample rate, HQ beeper) · Input (keyboard layout QWERTY/AZERTY/Spanish/Russian, joystick type, keyboard issue 2/3, emulate mouse, joystick prompt) · Storage (tape speed, disk type, fast/auto/trap load, microdrive, write protect) · Machine (model, custom ROM browse, late timings, contention, hi-res) · Peripherals (IF1, IF2, printer, Kempston mouse, Fuller, Melodik, SpecDrum) · General (turbo by default, **processor implementation**, frame rate, confirm actions, embed snapshot, strict aspect). **Caveat**: only Video border/scanlines, audio volume, machine model, processor and turbo-by-default are actually wired; `setAudioOption`/`setInputOption`/`setStorageOption`/`setPeripheralOption` fall through to `MockEmulatorCore` which just prints to stdout — `/home/fernando/detodo/spectrum/versions/oozx/machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/MockEmulatorCore.java`, `.../SpeccyEmulatorCore.java`
- **Game Browser** (internal frame): search field + Search button, server-side Machine and Genre filters, client-side "RZX", "Map", "Loadable" checkboxes, lazy-loaded screenshot thumbnails, per-row right-click menu: Load Game (submenu of machines, incl. "As the file says"), Load Version, Play Recording (list of RZX options), View Details, Add to Favorites, Download. `.../t/GameBrowserInternalFrame.java`
- **Game Details dialog**: cover, star Rating panel, Status and Quick Info panels; tabs **General, Technical, Publishers, Authors, Description, Screenshots, Game map, Releases, Downloads**; Emulation Options (model, speed); buttons Play Game / Download / Open Link / Close; a "Search Another Game" panel. `.../t/GameDetailsDialog.java`
- **Snapshot History**: table, double-click to load, Load / Remove from History / Clear All, right-click "View Details" (queries ZXInfo). `.../t/SnapshotHistoryInternalFrame.java`
- **Favorites**: `JList`, Play / Remove, double-click to launch; a favourite can be a game *or an RZX recording* (remembers which entry inside the zip). `.../t/FavoritesInternalFrame.java`
- **Pokes/Cheats**: `PokesDialog` (search over .pok files, per-mod checkboxes, Select All / Clear All / Apply Selected / Close, un-applying reverts using stored previous values), `PokesAppliedDialog` confirmation. Database: **3683 `.pok` files** bundled at `/home/fernando/detodo/spectrum/versions/oozx/machine/core/src/main/resources/pokes/`
- **README viewer**: internal frame rendering bundled README.md → HTML via commonmark (+tables). **About**: HTML `JOptionPane`. **Download viewer**: `.../t/DownloadViewerDialog.java` (opened from Game Details downloads). **Game-not-found**: simple + retry-with-new-name modes, `.../t/GameNotFoundDialog.java`
- **RZX player** (`.../t/RzxPlayerInternalFrame.java`): **playback only, no recording**. Controls: Open Recording…, Play, Stop, **Take Over** (stops the replay and leaves the same machine playable exactly where it was — `RzxSession.release()`), Loop toggle, Favorite; a table of the recording's blocks with a progress column. The picture lives in a separate ordinary emulator window titled "Spectrum #N"; several players can run at once; closing the controls closes their machine; activating one raises the pair. Zip archives with multi-part recordings prompt which part.
- **Cassette Browser** (`.../t/TapeBrowserInternalFrame.java`): block list, current block + progress, Play/Pause/Stop/Open Tape…; a tape can be inspected with no emulator running; one deck per machine, clipped on.
- **Audio-in** (`.../t/AudioInInternalFrame.java`): live waveform of the sound card input (real cassette player through a real lead), input line combo, zoom in/out and follow-head; currently a monitor, not yet feeding the ear line.
- **Joystick window** (`.../t/JoystickInternalFrame.java`): shows direction/fire as the machine's Kempston port reads them, the gamepad name, and whether Kempston is on.
- **Screen settings** (`.../t/ScreenSettingsInternalFrame.java`): built by walking self-describing knobs from `ScreenSettings` — Scaler, Show border, Lead (TV), Scan lines, Phosphor layout, Phosphor depth, Persistence, Tint, Brightness, Colour depth; named **profiles** (Save as…/Forget), "Use as default", "Restore default". Scalers: Nearest neighbour, Bilinear, Sharp bilinear, Scale2x, Scale3x, Edge directed (`.../screen/Scalers.java`, plus `XbrzScaler.java`).

### Peripheral / "drive bay" windows (the visual hardware)

`Equipment` is discovered via `META-INF/services`; each entry opens a `DeviceFrame` — **clipping the window onto a machine's window is what plugs the device in; unclipping or closing it unplugs it**, done on the emulator thread with a reset when the memory map changes. `/home/fernando/detodo/spectrum/versions/oozx/machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/DeviceFrame.java`, `Equipment.java`

- **`DriveBayFrame`** (`.../kit/.../DriveBayFrame.java`): one bay per drive, each a `MediaSlot` with a **green motor LED**, Insert / Eject / New (blank disk to format) / Save… / **Flip** (turn the disk over) / **Write-protect** toggles; the board's own button (e.g. Multiface red button); a red lamp for "ROM paged in"; a status line ("no ROM: …"); expanded it shows each head's track and "changed since it was saved". Slot widget: `.../kit/.../MediaSlot.java`
- **`IdeBayFrame`**: a slot per drive for hard-disk images where "Save" is the *commit*, paged lamp, register status, expanded shows sectors/MB. `.../kit/.../IdeBayFrame.java`
- **`DacFrame`**: level meter + volume slider, expanded shows the last byte sent. `.../kit/.../DacFrame.java`; meter `.../kit/.../LevelMeter.java`
- **22 devices offered**, names as they appear in the Equipment menu: Beta 128, Covox, +D, Didaktik 40/80, DISCiPLE, DivIDE, DivMMC, Fuller Box, Interface 1, Kempston Mouse, Multiface One / 128 / 3, Opus Discovery, Parallel printer, Simple 8-bit IDE, SpecDrum, ZXATASP, ZXCF CompactFlash, ZX Interface 2, ZXMMC, ZX Printer (`machine/devices/*/…Equipment.java`)
- Notable individual windows: **ZX Printer** — paper you can see, Tear off, Save as PNG, Fit/zoom/drag (`machine/devices/printer/.../PrinterInternalFrame.java`); **Parallel printer** — continuous paper, tear off, save as text (`machine/devices/parallel-printer/.../ParallelPrinterFrame.java`); **Interface 1** — eight microdrive slots with motor lights, shadow-ROM lamp, and an **RS-232 terminal** plus ZX Net file plumbing (`machine/devices/interface1/.../Interface1Frame.java`); **Interface 2** — cartridge slot + two joystick sockets (`machine/devices/interface2/.../Interface2Frame.java`); **Kempston Mouse** — the desk mouse moves over the machine's picture, with a **"Hold"** pointer-capture toggle, sensitivity knobs, button swap and a "Record" trace (`machine/devices/mouse/.../MouseInternalFrame.java`); DivIDE/DivMMC EPROM choice + write-protect jumper + NMI button; Fuller Box joystick socket + write light.
- There is **no photo/bitmap of the hardware** — the "picture" is drawn from Swing widgets and SVG icons.

### 4. Themes / look and feel

`Window > Look&Feel`: **Darcula, One Dark, Solarized Light, Solarized Dark, IntelliJ** (DarkLaf) plus **Metal**; the choice is saved to config and re-applied at startup. The launcher installs Solarized Light before config is read. `ZXSpectrumDesktopApp.addLaf/applySavedLookAndFeel`, `.../t/OOSpectrumLauncher.java`. Switching is **manual only** — no automatic dark/light detection.

### 5. Input

- `SwingKeyboard` is **not** an on-screen keyboard: it is a `KeyListener` mapping AWT key codes/unicode to Spectrum keysyms. `.../t/SwingKeyboard.java`
- Physical gamepad support via Jamepad/SDL, driving the Kempston port of whichever machine is in front, d-pad or left stick + any face button. `.../t/Gamepad.java`
- Mouse capture exists, but as the Kempston Mouse device's "Hold" toggle (`machine/devices/mouse/.../MouseInternalFrame.java`), not as a global option.
- Keyboard-layout list and joystick-prompt checkbox exist in Settings > Input but are unwired (see §3 caveat).

### 6. Files

- Chooser filter: `.tap .tzx .z80 .sna .szx .rzx`; RZX chooser also accepts `.zip`; disk/IDE images per device (`DiskInterface.imageExtensions()`). Launcher also treats `.csw` as a tape.
- `open(path)` routes recordings to the RZX player and everything else to a new emulator; URLs are downloaded and unzipped (`DownloadAndUnzip`).
- Snapshots: toolbar "Snapshot" saves `.z80`; File > Save/Load State; per-directory last-used paths remembered.
- Tape insertion via Open Tape…/Cassette Browser; disks/cartridges/CF cards via each device window's slots.
- **No drag & drop** anywhere (no `DropTarget`/`TransferHandler` in the tree).
- Recent files list persisted in `~/.oozx/config.json`.

### 7. Unusual for an emulator

- Devices are **windows you physically clip onto a machine window to plug in** — attach/detach = connect/disconnect at runtime, with snap-glow preview and shared-edge layout.
- Sessions restore **live machine state**, not just window geometry: emulator windows come back from an embedded packed snapshot with pokes re-applied.
- **RZX "Take Over"**: interrupt a recorded playthrough mid-replay and keep playing that same machine by hand.
- Self-describing screen knobs with named, savable **screen profiles** and a TV-lead model (RGB / Scart / Composite / Aerial) rather than a flat "filter" list.
- Two-segment speed slider (25%–40000%) under a right-click on the turbo button, with the rocket icon greying as speed rises.
- Choice of **Z80 processor implementation at runtime** (generated core vs. model) from Settings > General.
- Integrated ZXInfo.dk browsing with RZX-recording and game-map filters, and favourites that can hold a *recording*.
- Live oscilloscope for a real cassette player on the sound card.
- Multiple independent RZX players and cassette decks, each bound to its own machine.

### Claimed in old README but not found

- **"Dark/Light Mode: Automatic … theme switching"** — only manual selection from the Look&Feel menu.
- **"Display filters (scanlines, TV effect, motion blur)" / "HQ2x, HQ3x, Dot Matrix, PAL TV"** — the Settings combo lists them but `setVideoOption("filter", …)` is unhandled; the real effects are the TV/phosphor/scanline knobs in `ScreenSettings`. No motion blur; there is "Persistence" (phosphor).
- **"Brightness and contrast adjustment", "Aspect ratio correction (strict 4:3 or pixel perfect)", "Custom resolution support"** — Settings widgets exist but are not wired (brightness does exist as a `ScreenSettings` knob; contrast, aspect and custom resolution do not).
- **Audio tab items** (master/beeper/AY volumes, AY quality, stereo separation, sample rate, beeper quality) — only the toolbar's `volume` is wired; the rest print to stdout.
- **Input tab items** (keyboard layouts, joystick type, issue 2/3, mouse emulation, joystick prompt) — unwired.
- **Storage tab items** (tape speed, disk interface, auto/fast/trap load, microdrive, write protection) — unwired at this level (real equivalents live in the device windows).
- **Machine tab items** (custom ROM, late timings, contention, hi-res colour) — unwired.
- **Peripheral tab checkboxes** (IF1/IF2, printer, Kempston mouse, Fuller, Melodik, SpecDrum) — unwired; the working mechanism is the Equipment menu + device windows.
- **General tab items** (frame-rate limiting, confirmation dialogs, snapshot embedding) — unwired.
- **"File Menu: Create snapshots"** — snapshot creation is on the emulator toolbar, not the File menu.
- **"Window Menu: Manage multiple emulator windows"** — there is no list of open windows to switch between; only close/cascade/tile.
- **"Download Viewer: Monitor and manage file downloads"** — `DownloadViewerDialog` views one downloaded file opened from Game Details; the Game Browser's own "Download" item shows "(Download feature coming soon)".
- **"Snapshot History … File Browser Integration"** and **"Game Details … zoom capability"** on screenshots — not present as described.
- No mention in code of an on-screen/virtual keyboard, which the class name `SwingKeyboard` might otherwise suggest.

## Appendix B. Emulation core (sonnet)

### Inventory of OOZX core emulation capabilities

Scope covered: `machine/core/src/main/java` (177 files), `machine/spectrum/src/main/java` (22 files), `zx-rzx/src/main/java` (18 files), `doc/plan-configuracion.md`, plus root `README.md` for the comparison section.

**1. Machines/models.** Concrete `Spectrum` subclasses in `machine/core/.../speccy/machine/`: `Spec48.java`, `Spec48Ntsc.java` (NTSC/60Hz variant), `Spec128.java`, `SpecPlus2.java`, `SpecPlus2A.java`, `SpecPlus3.java`, `SpecPlus3E.java` (IDE-oriented ROM set, no snapshot model), `Pentagon.java` (Russian clone, no contention/floating bus, built without its Beta disk). All 8 are registered in `EmulatorModule.java`'s `Multibinder<Spectrum>` and `MODEL_NAMES`. Per-unit "late timings" is `Machine.Config.lateTimings` (`machine/core/.../speccy/modules/machine/Machine.java`). `MachineCapability.java` and `TimingsHandler.java` also declare/pre-time `TIMEX_MEMORY/TIMEX_VIDEO/TIMEX_DOCK`, `SCORP_MEMORY`, `SE_MEMORY`, `PENT512/1024_MEMORY` and matching `FrameTimings` (Timex, Scorpion, SE) but **no machine class implements them** — only `SeMemoryPeripheral.java` checks `SE_MEMORY`, unreachably. So Scorpion/TC2048/TS2068/Spectrum-SE are scaffolding, not emulated machines, in this scope.

**2. Memory.** `Memory.java`: page-table model (2KB pages), `map16k/8k/4k/2k`, RAM pool, per-page `contended` flag, `Config.writableRoms`. ROMs load from classpath `/roms/<name>` with filesystem fallback (`romNamed`), and `Spectrum.loadRomBank` falls back to the machine's standard ROM (`Rom.java`) on `RomNotLoadedException.java`. Paging ports: `Paging128.java` (0x7ffd) / `PagingPlus3.java` (0x1ffd), implemented by `Spec128.java`/`SpecPlus3.java`. Cycle-timing/contention hook shared with the fast core: `ContendedMemory.java` (`machine/core/.../speccy/modules/z80/`).

**3. Tape.** `Tape.java` reads **TAP, TZX, CSW** (`TapeExtensionType`); TZX generalized-data blocks (0x19) are explicitly unimplemented ("Gen. Data Block not supported!"). No PZX, no WAV. Writing: `startRecording`/`stopRecording` emit a TZX CSW-Recording block (0x18) or Direct-Recording block (0x15). Fast-load/traps: `TapeSettingsType.java` (JAXB, ported) carries load/save-trap and flash-load flags; `Timer.Config.fastLoading`; the real working fast-loader is `TapeAutoLoader.java` (types `LOAD ""`, runs the tape at 20000% speed, drops to 100% after) using the generic `PcTraps.java` address-watch mechanism. Deck controls `insert/eject/play/stop/rewind` plus listeners are on `Tape.java`; `TapeBlock.java` parses .tap/.tzx for a cassette browser; `TapeHardware.java` picks the best machine a TZX declares support for.

**4. Disk and storage.** `Disk.java` declares `Type{NONE,UDI,FDI,TD0,MGT,IMG,SAD,CPC,ECPC,TRD,SCL,OPD,D40,D80,LOG}`. Read+write implemented: MGT/IMG/OPD, TRD, SCL, D40/D80 (Didaktik). Read-only: CPC/ECPC (.dsk). **UDI/FDI/TD0/SAD explicitly throw** "images are not read yet" — declared, unimplemented. Controllers: `UpdFdc.java` (uPD765A/B) wired into `SpecPlus3.java`, bus-exposed by `Upd765Peripheral.java`/`FdcPortHandler.java`/`FdcStatusPortHandler.java`; `WdFdc.java` (WD1793/FD1793) wired into `Beta128Peripheral.java` (TR-DOS: built into Pentagon, pluggable on 48K/128K, ROM paged via `PcTraps`). `Fdd.java` models the physical drive and write-protect (`wrprot`), enforced in `Fdd.java`/`UpdFdc.java`/`WdFdc.java`. No DivIDE/DivMMC/ZXCF/ZXATASP/simpleIDE/microdrive peripheral or `.hdf` support exists in this scope — `DivIDE`/`DivMMC` appear only as prose in `WriteProtectedEprom.java`/`RomcsDevice.java`/`Memory.java` describing an abstraction, not a shipped class here.

**5. Snapshots.** `SnapshotFactory.java` (`machine/spectrum`) dispatches by extension to `SnapshotSNA.java`, `SnapshotZ80.java`, `SnapshotSZX.java`, `SnapshotSP.java` — **all four implement both `load()` and `save()`**. State carried: `Z80State.java` (full registers incl. MEMPTR/Q), `MemoryState.java`, `AY8912State.java`, `SpectrumState.java` (model, port latches, issue2, joystick). `SpectrumState` also carries fields for hardware **not modelled in core** (ULAplus enabled/active/palette, multiface, IF1 paged, LEC RAM) — `SnapshotSZX.java` explicitly skips the on-disk blocks for `ZXATASP/ZXCF/SIMPLEIDE/COVOX/BETA128/MOUSE/PLUSD/SPECDRUM/MICRODRIVE/...` ("MDRV have some design problems, ignored now"). Embedding: SZX's `ZXSTTP_EMBEDDED` tape block and `Tape.insertEmbeddedTape()`; `SnapshotSaver.java`/`SnapshotLoader.java` also round-trip a gzip+Base64/"Unicode-packed" snapshot for embedding inside JSON config (`SnapshotUnicodePacker.java`).

**6. RZX (zx-rzx).** File model: `RzxHeader.java`, `CreatorInfo.java`, `SnapshotBlock.java` (embedded/zlib or external), `InputRecordingBlock.java` (per-frame fetch count + recorded IN bytes), read/written by `RzxParser.java`/`RzxWriter.java`. Playback: `RzxPlayback.java` drives the CPU instruction-by-instruction and closes a frame by counting Z80 M1 fetches via the R-register delta (not T-states), forcing the interrupt where the recording says; `RZXPlayerIO.java` substitutes IN results and tracks sync-quality stats (`framesExact/Short/Over`, PC histograms) for diagnosing drift. Closest thing to "rollback": `RZXPlayerIO.goLive()` hands control to the real keyboard once play diverges/ends, optionally recording it, and `RzxWriter.Mode` lets `saveExtendedTo/saveSplicedTo/savePrependedTo` stitch old+new input frames without re-encoding the original snapshot — there is no interactive rewind/undo. `RzxArchive.java` (in `machine/core/.../rzx/`) is a separate static catalogue of ~4291 third-party recordings for a game browser, not the playback engine.

**7. Sound.** Beeper: `Beeper.java`. AY-3-8912: `Ay.java` (cycle-accurate tone/noise/envelope derived from the reference emulator, from a timestamped write queue) via `AyPeripheral.java` (128/+2 decode) and `AyPlus3Peripheral.java` (+3 decode, readable data port), wired by `AyDevices.java`. **AY output is mono-only**: `Ay.mixInto` sums all three channels equally into both ears, its own comment says stereo (ACB/ABC) positioning "belongs here" but isn't done, and `Options.enumerateSoundStereoAY()` (`machine/core/.../speccy/machine/Options.java`) is a stub returning `0`. `Dac.java`/`DacDevice.java` are generic flat-DAC-on-a-port infrastructure whose Javadoc names Covox/SpecDrum/Currah µSpeech as intended users, but **no concrete Covox/SpecDrum/Melodik peripheral class exists in this scope** (consistent with the SZX skip list above); Fuller appears only as a joystick type here. Backend: `JavaSoundDevice.java` (`javax.sound.sampled`), `blip/BlipBuffer.java`/`BlipSynth.java`/`BlipEq.java` (band-limited synthesis), 44100Hz default, `Sound.Config` (`device="buffer=8192,frames=4,verbose"`, `whileLoading`), speaker EQ presets with per-preset bass/treble, and `dropWhenAhead` to never block turbo mode.

**8. Video.** `Ula.java` builds per-T-state contention tables and handles port 0xFE read (keyboard+tape EAR) and write (border/speaker/tape monitor), split into partial-decode (`UlaPeripheral.java`) vs full-decode (`UlaFullDecodePeripheral.java`, Pentagon). `Display.java` is a beam-position-aware, dirty-rectangle renderer: it plots each cell "as the beam passed it" (supporting mid-frame attribute changes) and tracks the border as a list of beam-position colour changes; flash flips via attribute bit 7 every 16 frames. `Picture.java` is the screen-to-UI interface: a plain `int[]` RGB pixel buffer a window wraps directly, no copy. **Not found in scope**: snow effect, scanline filter, or any Timex hi-res/hi-colour mode (`TIMEX_VIDEO` is declared but unimplemented, see §1). Floating bus: `Spectrum.spectrumUnattachedPort()`/`unattachedPortAmstrad()` vs. `hasFloatingBus()==false` for +2A/+3/Pentagon.

**9. Input.** `Keyboard.java` models the full matrix; issue 2/3 is `Machine.Config.issue2`, consumed in `Spec48.ulaPortIdleValue`. `Input.Config.recreatedSpectrum` is a real, wired flag (read in `Input.java`) for "Recreated ZX Spectrum"-style keyboards. Joysticks in `Joystick.java`: Cursor, Kempston, Sinclair 1/2, Timex 1/2, Fuller, each mapped to key-pairs or a dedicated port byte; `Input.Config` models two 15-button pads plus keyboard-as-joystick. Kempston mouse is only a boolean flag here (`Input.Config.kempstonMouse`); the actual peripheral lives in `machine/devices/mouse` — outside the requested scope, so not literally missing from the repo, just not in these three directories. No keyboard-layout (Spectrum/European/American) selection found in scope.

**10. Peripheral/port bus.** `Peripherals.java` implements `PeripheralBus`: a per-port (0x10000-entry) cache of which `PortHandler`s answer it, wired-AND merge of multiple answers with a "driven" flag, floating-bus merge for undriven bits, `Pluggable` peripherals queue-jump ahead of the machine's own chips, and `busListeners` for read-snooping devices. `ContendedPeripheralBus.java`/`PeripheralBusDelegate.java` decorate the raw bus with port-contention timing. Devices are discovered via `ServiceLoader.load(Extension.class)` in `EmulatorModule.java` — each `Extension.java` is itself a Guice module found on the classpath. `Speccy.java` is the composition root (`Speccy.create()`), assembling every module and machine and owning `init()`/`end()`. `EmulatorModule.java` binds `Core`/`IO`, the two buses, its own `@Section` configs, and the 8-model `Multibinder<Spectrum>` (`@DefaultMachine` = `Spec48`).

**11. Settings.** `@Section` classes found in scope: `machine` (`Machine.Config`: lateTimings, issue2), `machine.48k/128k/plus2/plus2a/plus3/plus3e/pentagon` (per-model `Rom` sockets, e.g. `SpecPlus3.Config.detectSpeedlock`), `memory` (writableRoms), `sound`, `speed` (`Timer.Config`: emulation%, fastLoading), `input`, `beta128` (`Beta128Peripheral.Config`: rom, bootOn48k, autoBoot), `floppy` (`Fdd.Config`), and `desktop` (`OOZxConfiguration.java`, `wasTheWholeFile=true`: recent files, favorites, look&feel, per-window turboMode/muted/paused, preferred `processor` name). Mechanism: `Configuration.java`/`Section.java` — one JSON file (`~/.oozx/config.json`), each section merged onto its class's field defaults via Jackson, unknown sections round-trip untouched. `doc/plan-configuracion.md` (measured 2026‑09‑06) documents this replacing the reference emulator's flat 253-field `Settings`, with `machine/core/src/test/java/model/tests/config/SectionsAreDeclaredTest.java` as the guard test. Most of the ~21 peripheral sections it counts belong to `machine/devices/*`, outside this scope — only `beta128` and `floppy` live in core.

**12. Fast core hook.** `EmulatorModule.java`: `OptionalBinder<Core>` defaults to `OopCore.class` and a `Multibinder<Core>` always includes it; per its own comment, "anything faster arrives on the classpath" — a generated `Core` (in `machine/generated`, outside scope) overrides the binding via `EmulatorModule.core(Class<? extends Core>)`. `PeripheralIO.java` is the matching seam for RZX (bound to `IO` so it "can be replaced" for playback or "wrapped" for recording). The user-facing switch is `OOZxConfiguration.processor` plus `EmulatorControl.getProcessors()/getProcessor()/setProcessor()`.

**13. Perf/pacing.** `Timer.java` samples wall-clock time to estimate real speed and drives `Sound`'s `effectiveSpeed`; `Timer.Config.emulation` (percent, default `20000` = 200×, `UNLIMITED=1_000_000`) is the turbo/speed-limit knob, temporarily forced to 20000% by `TapeAutoLoader.java` while a tape is loading. `JavaSoundDevice.dropWhenAhead(...)` drops audio frames instead of blocking once above real time, so sound never throttles turbo. `Scheduler.java`/`Task.java`/`Timetable.java`/`BeanPosition.java` are the generic event-at-T-state mechanism frame-end, tape edges and FDC timing ride on. `EmulatorControl.isTurboMode()`/`notifySpeed()` and `EmulatorListener.onTurboModeChanged()` expose turbo to any host UI.

**Claimed in old README but not found (within this scope):**
- "AY‑3‑8912 ... with stereo separation control" — mono-only mixdown (`Ay.java`); `Options.enumerateSoundStereoAY()` is a stub returning `0`.
- "Snow Effect: Optional recreation of video snow effect" — no "snow" anywhere in `Display.java`/`Ula.java`.
- "Scanline Effects: Optional scanline rendering" — no such concept in `Display.java`/`Picture.java`.
- "Printer Interface: ZX Printer emulation" — only a generic Centronics-style strobe hook for the +3 (`PrinterPort.java`), not a ZX Printer (aerial thermal printer) model.
- "Multiple sound quality levels" — not found as a setting; `BlipBuffer.java` uses one fixed `BLIP_HIGH_QUALITY` constant.
- Peripheral/Storage settings claims of "Sound card support (Melodik AY, SpecDrum)" and "Microdrive support" / "ZX Interface 1/2 support" — no such peripheral classes exist in `machine/core`/`machine/spectrum`/`zx-rzx`; only vestigial snapshot-state fields (`SpectrumState.java`) that `SnapshotSZX.java` explicitly skips on read.

## Appendix C. Peripherals (sonnet)

### Peripheral inventory — `machine/devices`

Evidence base: `machine/devices/pom.xml` (parent, lists 25 submodules), each module's `pom.xml`, `src/main/java/**/*.java`, `src/main/resources/META-INF/services/*`, `machine/core/.../devices/Extension.java`, `machine/devices/kit/.../{Equipment,DeviceFrame,DriveBayFrame,IdeBayFrame,DacFrame,MediaSlot}.java`, `machine/app/.../ZXSpectrumDesktopApp.java`, `doc/plan-modulos.md`.

### 1. The 22 peripheral modules

| module | hardware emulated | own window (class — what it shows) | files | lines |
|---|---|---|---|---|
| beta128 | TR-DOS disk i/f (same chip as Pentagon's built-in) | `Beta128Equipment`→`DriveBayFrame` (4 drives, TRD/SCL, "Boot" button) | 3 | 139 |
| covox | 1-channel parallel-port DAC | `CovoxEquipment`→`DacFrame` (VU meter + volume) | 3 | 175 |
| didaktik | Didaktik 40/80 disk i/f (WD2797, 2 drives) | `DidaktikEquipment`→`DriveBayFrame` (2 drives, D40/D80, "SNAP" button) | 3 | 276 |
| disciple | MGT DISCiPLE (WD1770, 2 drives, joystick+net stubs, ROM/RAM swap) | `DiscipleEquipment`→`DriveBayFrame` (reuses plusd's bay/`MgtDiskInterface`) | 3 | 234 |
| divide | DivIDE (EPROM+32K banked RAM+IDE behind automapper) | `DivIdeFrame` extends `IdeBayFrame` (+EPROM chooser, wp jumper, NMI) | 4 | 261 |
| divmmc | DivMMC (automapper + SD/MMC slot) | `DivMmcFrame` extends `IdeBayFrame` (+EPROM chooser, wp jumper, NMI) | 4 | 253 |
| fuller | Fuller Box (AY on 0x3f/0x5f + joystick on 0x7f) | `FullerFrame` (chip-write LED, joystick-socket toggle; no media) | 4 | 247 |
| interface1 | 8 Microdrives + RS232 + ZX Net behind shadow ROM | `Interface1Frame` (8 slots, RS232 terminal, RxD/TxD/Net file plugs) | 7 | 1304 |
| interface2 | Cartridge port + 2 joystick sockets | `Interface2Frame` (cartridge slot, 2 joystick-socket toggles) | 5 | 361 |
| joystick | Kempston joystick (strict + loose decoding) | **none** — settings flag only | 4 | 206 |
| melodik | AY-3-8912 box for machines without one | **none** — settings flag only | 2 | 110 |
| mouse | Kempston Mouse | `MouseInternalFrame` (live x/y/buttons, sensitivity knobs, hold, wire-recorder) | 6 | 885 |
| multiface | Multiface One/128/3 (freeze cart, 8K ROM+8K RAM) | 3 `Equipment`s → shared `MultifaceFrame(model)` (red button, paged LED, RAM dump) | 10 | 765 |
| opus | Opus Discovery (WD1770+6821, memory-mapped, no ports) | `OpusEquipment`→`DriveBayFrame` (2 drives, OPD/OPU, no button) | 3 | 277 |
| parallel-printer | +2A/+3 parallel printer port | `ParallelPrinterFrame` (continuous text paper, tear-off/save) | 5 | 341 |
| plusd | Miles Gordon +D (WD1770, 2 drives, 8K ROM+RAM, printer port) | `PlusDEquipment`→`DriveBayFrame` (2 drives, NMI button) | 4 | 292 |
| printer | Sinclair ZX Printer (2 port decodings) | `PrinterInternalFrame` (rendered paper roll via `PrinterPaper`) | 9 | 996 |
| simpleide | Simple 8-bit IDE (1 channel) | plain `IdeBayFrame` (master/slave), zero custom UI code | 3 | 116 |
| specdrum | SpecDrum sampler DAC (port 0xdf) | `SpecDrumEquipment`→`DacFrame` (VU meter + volume) | 3 | 168 |
| zxatasp | ZXATASP (512K banked RAM+IDE behind 8255) | `ZxataspFrame` extends `IdeBayFrame` (+wp jumper, upload toggle) | 4 | 322 |
| zxcf | ZXCF CompactFlash (1M banked RAM+CF) | `ZxcfFrame` extends `IdeBayFrame` (+upload toggle) | 4 | 227 |
| zxmmc | Bare SD/MMC slot, 2 ports | plain `IdeBayFrame` (1 slot), zero custom UI code | 3 | 100 |

Infrastructure modules (not peripherals themselves, also in scope):

| module | role | files | lines |
|---|---|---|---|
| kit | shared toolkit: `DeviceFrame`, `Equipment`, `DriveBayFrame`, `IdeBayFrame`, `DacFrame`, `MediaSlot`, `LevelMeter`, `EmulatorWindow` | 8 | 862 |
| ide | shared IDE/CF/MMC-SPI protocol lib (`IdeBoard`, `IdeChannel`, `MassStorage`, `MmcBoard`, `MmcCard`, `MmcSlot`, `BankedIdePeripheral`, `DivPeripheral`), reused by divide/divmmc/simpleide/zxatasp/zxcf/zxmmc | 9 | 1512 |
| all | `devices-all`: empty jar, depends on all 22 peripheral jars; holds `PeripheralPresenceTest` | 0 | 0 |

Total: 10,429 main-code lines across all 25 submodules (8,055 in the 22 actual peripherals; sum verified against `find machine/devices -path '*/src/main/java/*' -name '*.java' | xargs wc -l`).

### 2. Discovery mechanism

Two independent `ServiceLoader` lookups, no central registry:
- **`Extension`** (`extends com.google.inject.Module`, in `machine/core/.../devices/Extension.java`) — installed by `EmulatorModule.configure()`: `ServiceLoader.load(Extension.class).forEach(this::install)` (`machine/core/.../EmulatorModule.java:135`). Each impl's Guice `configure()` adds its `Peripheral`(s) to a `Multibinder<Peripheral>` and optionally a `Configuration.section(...)`.
- **`Equipment`** (`machine/devices/kit/.../Equipment.java`) — methods: `name()`, `open()` (returns a `DeviceFrame<?>`). Loaded and sorted by name in `ZXSpectrumDesktopApp` (`ServiceLoader.load(Equipment.class)...`, line 2309) and turned 1:1 into the desktop's "Equipment" `JMenu` items (line 1097).
- A `Peripheral` must implement: `activate(machine)`, `deactivate()`, `getPorts()`, `fitsOn(machine)`, `isWanted()`, `hasHardReset()`; a pluggable one also implements `Pluggable`: `plugIn(connected)`, `isPluggedIn()` (usually inherited from `PluggablePeripheral`).
- Wiring is two one-line text files per jar: `src/main/resources/META-INF/services/com.fpetrola.oozx.speccy.devices.{Extension,Equipment}`.
- **`devices/all`**: a jar with no source of its own, depending on all 22 peripheral jars, so the classpath carries every service file; `machine/app` depends only on `devices-all` and names no device.

### 3. Minimum to add a peripheral
- Implement `Peripheral` (+ `Pluggable`/`PluggablePeripheral` if it's plugged/unplugged at runtime).
- Add one `Extension` impl + its service file. A `Config` settings section (`Configuration.section(...)`) is optional — beta128, fuller, joystick, zxmmc skip it.
- A window is free if the hardware fits a kit shape: instantiate `DriveBayFrame` (N floppy drives), `IdeBayFrame` (IDE/CF/MMC slots) or `DacFrame` (DAC+meter) directly — zero custom Swing (zxmmc, simpleide, covox, specdrum). Unusual controls just subclass one and add a widget or two (divide, divmmc, zxatasp, zxcf). No matching shape means a fully custom `DeviceFrame` (fuller, interface1/2, mouse, multiface, printer).
- Menu entry is automatic (section 2) — add the `Equipment` service file and it appears, no other code.
- **Smallest example: `machine/devices/zxmmc`** — 3 files, 100 lines total (`ZxmmcPeripheral` extends shared `MmcBoard`; `ZxmmcDevices` is a 6-line Guice binding; `ZxmmcEquipment` is 8 lines returning `new IdeBayFrame<>(...)`), plus 2 one-line service files — and it still gets a complete insert/eject/commit card-slot window for free. (Smaller still if a window is skipped entirely: melodik, 2 files/110 lines, or joystick, 4 files/206 lines.)

### 4. `DriveBayFrame` (`machine/devices/kit/.../DriveBayFrame.java`)
Per drive: a `MediaSlot` (insert/eject via file chooser, motor LED, optional new-blank/save/flip-side/write-protect buttons); plus one shared "paged" LED (ROM paged in over the machine's) and a status line; the board's own button if it has one (Beta128 "Boot", Didaktik "SNAP", Disciple/+D "NMI" — Opus has none). Expanded view adds a line per drive with head/track and "changed since saved." Used by **beta128, didaktik, disciple, opus, plusd** (Interface1's 8-Microdrive bay is a hand-built lookalike, not this class, since cartridges aren't `Fdd`s). Images insert/eject at runtime through a file chooser; all work is dispatched onto the emulator thread (`z80.later(...)`); only a mapping change (ROM/RAM) forces a reset.

### 5. Categories
- **Mass storage**: beta128, didaktik, disciple, opus, plusd (floppy via `DriveBayFrame`); divide, divmmc, simpleide, zxatasp, zxcf, zxmmc (IDE/CF/MMC via `IdeBayFrame`); interface1 (Microdrives).
- **Sound**: covox, specdrum (DACs), melodik, fuller (AY chips).
- **Input**: joystick, mouse; interface2 also carries joystick sockets alongside its cartridge slot.
- **Printer**: printer (ZX Printer), parallel-printer (+2A/+3); plusd/disciple pass through to parallel-printer.
- **"Multiface"-style tool**: multiface (One/128/3) — the only freeze-button/NMI-menu device.
- **Stubs/incomplete**: none — `grep -rn "TODO\|FIXME\|not implemented\|UnsupportedOperationException\|stub"` over all of `machine/devices` (excluding `target/`) returns zero hits. All 22 present modules are done; five more are planned (Currah uSpeech, Currah uSource, SpeccyBoot, Spectranet, TTX2000S) have no module directory yet — future work, not stubs in the tree.

### 6. Design intent (`doc/plan-modulos.md`)
Rule: "not how much a peripheral knows, but who knows whom" — a peripheral may use the ULA/mixer/clock, but the emulator must never name a peripheral; enforced by one-way module dependencies (base→ui→devices→app) checked by the compiler, not convention/tests. What a machine comes with stock (128/+3 pager, both ULAs, +3's FDC, a 128's onboard AY) stays in core — "that's not plugged in, that IS the computer." `Extension`+`Equipment` via `ServiceLoader` make "adding a peripheral [mean] adding a jar." One module per peripheral avoids duplicating the ~25-line clip/plug boilerplate 23 times (`kit` factors it once). Numbers: `plan-modulos.md`, measured 1 Sep 2026 — core was 263 files/51 using Swing, split into base/ui/devices/app with 338 tests (260/16/33/29). Measured 2 Sep 2026 against the reference emulator's 23,572 lines of C / 47 peripheral types — 4 devices already migrated when this plan started, 23 more planned (this doc), shared non-device-specific groundwork (real NMI, PC-trap aspect, ROM-from-file, `Dac`, the `Disk`/`Fdd`/`WdFdc` stack) built into core first; 19 of the 23 are now done, 5 remain (blocked mostly on unavailable ROMs/hardware).

## Appendix D. ZXInfo catalogue (sonnet)

### ZXInfo Integration — Code Inventory

**Scope read:** all 23 files in `machine/zxinfo/src/main/java/com/fpetrola/oozx/api/`, its one test (`ScreenFromTheApiTest.java`), `machine/zxinfo/pom.xml` (no resources dir exists), and the callers in `machine/app` (`GameBrowserInternalFrame`, `GameDetailsDialog`, `DownloadViewerDialog`, `GameNotFoundDialog`, `SpeccyEmulatorCore`, `ZXSpectrumDesktopApp`, `OOSpectrumLauncher`) plus supporting classes in `machine/core` (`DownloadAndUnzip`, `RzxOption`, `TapeHardware`, `PokesManager`, `OOZxConfiguration`, `EmulatorModule`).

### 1. Endpoints & base URL
Base URL `https://api.zxinfo.dk`, all under `/v3` (`ZxInfoApiHandler.java:29`, `ZxInfoClient.java:27`).
- `GET /v3/search` — ~19 optional filters (`ZxInfoClient.java:40-63`); the app only ever supplies `query`+`machinetype`+`genretype`, fixed `size=150`, `mode=compact` (`ZxInfoApiHandler.java:45-50`).
- `GET /v3/games/{id}?mode=full` — full detail (`fetchGameDetails`).
- `GET /v3/metadata/` — facet counts (machinetypes/genretypes/features); only machinetypes+genretypes are used, to fill the two filter combos (`GameBrowserInternalFrame.java:164-189`).
- `GET /v3/suggest/{term}`, `/suggest/author/{term}`, `/suggest/publisher/{term}` — implemented but **never called** anywhere in `machine/app`.
- `GET /v3/filecheck/{hash}` — MD5/SHA512 lookup, wrapped by `identifyFile()`, also **never called** from `machine/app`.
- No separate screenshot/cover endpoint: image URLs arrive embedded in the search/game JSON (`screens[].url`/`.scrUrl`/`.filename`) and are turned into absolute URLs by hand across `worldofspectrum.net`, `zxinfo.dk/media`, `spectrumcomputing.co.uk` (`GameBrowserInternalFrame.getFileURL`).

### 2. Data fetched
`GameEntry` (raw model) is rich: title, alsoKnownAs, release date parts, machineType, genre/genreType/genreSubType, numberOfPlayers, multiplayer mode/type, language, availability, isbn, xrated, originalPrice, score+votes, publishers[]/authors[] (with roles/country/notes), releases[] (publishers/titles/year/price/code/barcode/files[]), screens[], additionalDownloads[], magazineReferences[], reviewAwards[], relatedLinks/relatedSites[], youTubeLinks[], tosec[], md5hash[], otherSystems[], compilationContents[], programmingLanguage/screenMovement/graphicalView/sport/demoParty/crossPlatform (`GameEntry.java`).
The UI-facing `GameDetail` (built by `ZxInfoApiHandler.convertGameEntryToDetail`) keeps a much smaller subset (id/title/dates/genre*/machineType-string/availability/isbn/xrated/score/publisher-names/author-names/screenshots-flat-list/additionalDownloads+gameMaps/releases-as-string-maps). **Never populated**: `machines` (compat list), `memoryRequired`, `description`, `coverImageUrl`, `rating`. **Never surfaced anywhere**: vote count, YouTube links, magazine reviews/awards, related games/sites, TOSEC/MD5 hashes, compilations, author roles/country.
Screens are not typed: `Screen.type`/`.title` (loading/in-game/inlay/cover) exist but both consumers (`GameBrowserInternalFrame.java:395-407`, `ZxInfoApiHandler.java:136-157`) only read url/scrUrl/filename and dump everything into one undifferentiated list in API order.

### 3. What the user can do
- Search triggers only on Enter/button click (no type-ahead, despite `/suggest` existing). SwingWorker off-EDT, indeterminate progress bar.
- Server filters: machine + genre combos (from `/metadata/`). Client-side-only checkboxes RZX/Map/Loadable re-filter the same fixed 150 results; no pagination.
- Only `contentType=="SOFTWARE"` hits are shown (`GameBrowserInternalFrame.java:397`).
- Each row shows the first two `screens[]` entries; if fewer, falls back to a hardcoded external placeholder GIF (`i.sstatic.net/wAz1X.gif`).
- Row context menu: Load Game / Load Version (submenu when >1 loadable file, each with a per-file "machine" override submenu of 8 hardcoded names from `EmulatorModule.MODEL_NAMES`) / View Details / Add to Favorites / Download / Play Recording.
- "View Details" opens `GameDetailsDialog`: 9 tabs (General, Technical, Publishers, Authors, Description, Screenshots, Game map, Releases, Downloads) + re-search box + cover panel.
- Loading path: browser row click / "Load Game" → `ZxInfoApiHandler` file URL → `DownloadAndUnzip.unzip()` (unconditionally treats URL as a zip) into `${java.io.tmpdir}/zxinfo_extracted` → machine boots — **no disk cache**, re-downloaded every time (`DownloadAndUnzip.java`, `OOSpectrumLauncher.java:74-96`).
- Format priority when several files exist (`DownloadAndUnzip.scoreOf`): `.rzx`(40) > `.z80/.sna/.szx`(30) > `.tzx/.tap`(20) > `.csw`(10); everything else incl. `.dsk` is unloadable; +5 for "48" in name, -5 for "128", -50 under `/denied/`, -2 for "alternate/different".
- Machine auto-pick is **not** from ZXInfo's `machineType`: for tapes it reads the TZX hardware-type block (id 0x33) via `TapeHardware`, else "128" in filename, else 48K default; snapshots load as-is.
- Images/downloads: no cache anywhere — `ScreenshotPair`/`LazyImageIconLoader`/`GameDetailsDialog.loadImageAsync` all fetch straight into Swing `ImageIcon`s each time.

### 4. Local DB / favourites / pokes
`~/.oozx/config.json` (`OOZxConfiguration.java:82`) persists recent files, window layout, favourites, and a snapshot-history map (state blobs inline as JSON).
Favourites store source/title/kind(GAME|RECORDING)/ZXInfo `gameId`/archive entry (`OOZxConfiguration.java:494-572`) — `gameId` round-trips but **is never read back** anywhere (only caller is a unit test).
Pokes are a wholly separate, bundled, offline set: 3,683 `.pok` files under `machine/core/src/main/resources/pokes/<letter>/`, matched to the loaded file/title by fuzzy/Levenshtein string match (`PokesManager`) — no relation to ZXInfo ids or the game browser.
No "recently viewed on ZXInfo" / play-history tied to ZXInfo ids.

### 5. Unusual
- The method doing the real (non-mock) ZXInfo search call is still named `createMockResults` (`GameBrowserInternalFrame.java:390`).
- Rating stars comment says "Convert 0-100 to 0-5" but the code only does `Math.round(gameDetail.score)`, no division (`GameDetailsDialog.java:282-286`).
- RZX recordings are merged from two independent, non-overlapping catalogues (ZXDB's `additionalDownloads` + a local `RzxArchive` keyed by numeric id) — see `RzxOption.java` javadoc (26 in both, 15 ZXDB-only, 108 archive-only).
- Classes like `GameSearchResult`, `GameBrowserListener`, `DownloadAndUnzip`, `RzxOption` live in `machine/app`/`machine/core`, not `machine/zxinfo`, despite the module's own pom stating "the emulator does not know it exists".

### 6. Claimed but not found (root `README.md`, "Game Browser & Library")
- "Real-time game search with instant results" — no type-ahead; Enter/click only, `/suggest` unused.
- "Game cover artwork" — `coverImageUrl` never populated; panel always falls back to a screenshot.
- "Compatibility information across Spectrum models" — `GameDetail.machines` never populated; the Technical tab's machine table never renders.
- "Screenshot gallery with zoom capability" — thumbnails have no click/zoom handler (only Game map has double-click-to-enlarge).
- "General information and descriptions" — `description` never populated; tab always reads "No description available".
- "Play Games: Click 'Play' to launch directly in the emulator" — the Details dialog's "▶ Play Game" only shows a message dialog; it starts nothing (`GameDetailsDialog.java:868-878`). Real loading only works from the browser list itself.
- "Download Viewer: Monitor and manage file downloads" — it's a single-file preview popup, not a manager/queue; both "Download" actions are stubs (one literally "coming soon" — `ZXSpectrumDesktopApp.java:2800-2804`; the other just opens a zxinfo.dk browser search).

## Appendix E. Generated core and performance (opus)

I have the full picture. Here is the evidence, with nothing rounded or inferred.

---

### Evidence for the README claim

### 1. What the generated core is

**Sources it is generated from.** The OOP model's **Java source text** (JavaParser AST), plus the **live object graph** of a model machine built for the occasion. `/home/fernando/detodo/spectrum/versions/oozx/machine/generated/src/main/java/com/fpetrola/oozx/generated/GeneratedCores.java` (`model()`, `source()`) builds a silent `Speccy` on `ModelCore`, then hands `CoreGenerator` four things to weave: the instruction instances of the decoder tables, the **MEMPTR aspect** (`MemptrUpdater`, a visitor, resolved by double dispatch against the instance), the **contention aspect** (`target.contention = new RecordingPhaseProcessor(...)`), and the **machine's memory** as helpers (`target.helpers.put("read"/"write", state.getMemory())`). The specializer is `machine/generated/src/main/java/com/fpetrola/z80/generate/{Specializer,CoreGenerator,Simplifier,Folder,SourceIndex}.java`.

**Frozen vs. held.** `held(...)` injects as `final` constructor fields: `ram, mapRead, mapWrite, ula, clock, display, io, noMreqRun2..noMreqRun7`. The declared rule (`doc/plan-nucleo-generado-jvm.md`, "La frontera"): *freeze what cannot change while this core instance exists; read on every access everything the machine mutates — the page table, a page's fields, the contention tables and the T-state counter.* Justified by the finding that "todo el modelo de bancos de esta máquina es 'mutar la tabla de páginas'", verified against `Machine.selectMachine`, `Spec128.memoryPortWrite`, `mapRomcs8k`.

**How.** Committed, as a source of the module (`machine/generated/src/main/java/com/fpetrola/oozx/generated/GeneratedSpectrumZ80.java`), written by the build and compiled by it: an `exec-maven-plugin` execution at `process-classes` runs `GeneratedCores.main` in a forked JVM, which writes the file only when its first line names a model other than the current one, and then compiles that one file into `target/classes` - the module's compiler ran before this point and would leave the stale class beside the new source. Key = SHA-256 of `META-INF/model-sources` + `com/fpetrola/z80/generate` + `com/fpetrola/oozx/generated`, with the generated core itself excluded, first 16 hex chars. Timings: **generate 10,7 s / 956 KB of source; compile 1,7 s / 326 KB of classes; a machine that runs it: `Class.forName`.**

**Size** (reference copy, `/home/fernando/detodo/spectrum/versions/oozx/machine/generated/src/main/java/com/fpetrola/oozx/generated/GeneratedSpectrumZ80.java`): **27 345 lines, 955 776 bytes, 1 792 `case 0x`, 231 `decode*` methods, 1 036 `read(` / 604 `write(` / 1 328 `contendNxM(` sites, 6 contention helpers (`1x1, 1x3, 2x1, 4x1, 5x1, 7x1`), 926 mentions of MEMPTR.** Phase-1 pure core was 27 400 lines / 1 616 `case` / 7 tables in 112 methods, largest 3 562 bytes of bytecode (HotSpot `HugeMethodLimit` is 8 000).

**Discovery.** `machine/generated/src/main/resources/META-INF/services/com.fpetrola.oozx.Extension` → `GeneratedCores` binds `Core` through Guice `OptionalBinder.setBinding()`; the emulator's default is `OopCore`. The emulator does not depend on the module.

**When the OOP core is used instead:** under a wiring that counts its own T-states — the harness's `model.harness.CountingWiring`, whose listeners on the memory the processor reads are what add them, and a core that reads the memory tables itself never reaches those; `GeneratedCores.Tap` asks the injected `ProcessorWiring` and hands out an `OopCore` where the answer is no. Also in a build that carries no generated core, and with "Fast Core" off (`OOZxConfiguration.fastCore`). A running machine can also be switched (`Z80.useProcessor`).

### 2. Every measurement found

Conditions column is verbatim from the source. **M** = measured, **P** = planned/target/premise.

| what | number | unit | vs | conditions | source |
|---|---|---|---|---|---|
| M CPU pure, synthetic program | OOP 36–43 / gen 133–148, **3,5×** | M instr/s | gen vs OOP | `CoreBenchmark`, 2 Sept 2026, branch `nucleo-generado`, both cores same JVM | `doc/plan-nucleo-generado.md` §"Lo que quedó" |
| M CPU pure on `int[]`, no listeners | OOP 44–48 / gen 195–215, **4,5×** | M instr/s | gen vs OOP | one JVM per core, JDK 21.0.2, 2 Sept 2026; ±8 % noise; same-JVM cost the generated core 30 % | `doc/plan-nucleo-generado-jvm.md` §"Paso 0" |
| M idle-ROM own loop | OOP 1191–1398 / gen 9076–10478 | fps | gen vs OOP | `LoopCoreMeasurement`, 3000 frames | ibid. |
| M JSW RZX, 6000 frames | OOP 555 / gen 847 | fps | gen vs OOP | `RzxCoreMeasurement`; **not reproducible**: OOP gave 458 the same day | ibid. |
| M ídem + `presentFrame`, no sound | OOP 559–667 / gen 784–803 | fps | gen vs OOP | ibid. | ibid. |
| M bytecode after A2+A3 | 215 069 → 199 689 → **195 310 (−9,2 %)**; lines 27 421 → 26 391 → 26 061; largest method 3 547 → 3 291; `A & 0xFF` 302 → **0** | bytes / lines / sites | before generator changes | step 1, JDK 21.0.2 | ibid. §"Paso 1" |
| M bytecode after A4 | 191 253 (**−11,1 %** over both steps); `== -1` 273 → **14** | bytes | ibid. | step 2 | ibid. §"Paso 2" |
| M dispatch split (pure core) | shift 0: 196,5 best/190,9 mean · 3: 193,2/185,6 · **4: 222,8/216,5** · 5: 219,9/213,4 | M instr/s | between variants | `int[]`, each variant compiled apart, 12 interleaved runs in both orders | ibid. §"Paso 3" |
| M M1+M2 A/B | gen 851→798, OOP 708→697 | fps | before vs after | best of 3 runs of 6000 JSW frames, files put/removed with `git checkout`; "within the noise" | ibid. §"Paso 4" |
| M JFR profile, JSW on generated | `Z80$1.write` 31,3 %, `Z80$1.read` 17,6 %, `addTStates` 10,6 %, AY+Blip 10,5 %, RZX 7,9 %, **whole generated core 8,8 %** | % of samples | — | 227 samples | ibid. |
| M inlining of accesses | read 1 040 decisions / 89 inlined (8,6 %); write 433 / 4 (0,9 %); contend 583 / 40 (6,9 %) | sites | — | `PrintInlining`, same playback | ibid. |
| M JIT-flag experiment | 878/813 · 848/823 · 904/836 · 847/800 | fps best/mean | between flag sets | 4 runs each; ±3 % noise | ibid. |
| M all calls removed by force | as-is 858/783 · helpers 868/835 · forced 836/789 | fps | between variants | 5 interleaved runs each, all executing **exactly 29 097 384 instructions**, 1 412 `force inline by CompileCommand` sites | ibid. |
| M instrument fix | same code **830 → 1 360 fps**; dispersion ±12 % → ~1 % | fps | before/after pinning | `taskset -c 2`, 12 blocks of 500 frames, mean of last 6; i5-1335U hybrid, IntelliJ at 66 % CPU, 2,7 GB swap, `powersave` | ibid. |
| M cost of the machine's memory | base 1 360 · code inside core, private call 1 398 (+3 %) · forced 1 362 (0 %) · no screen dirtying 1 408 (+3,5 %) · no contention 1 399 (+3 %) · parallel arrays 1 400 (+3 %) · **floor: flat array 1 470 (+8 %)** | fps | vs base | one P-core, same 29 097 384 instructions | ibid. |
| M where the 3 % is | 1 359 · 1 344 · 1 372 · 1 365 · **1 399** (same work, in a private method of the core) | fps | between variants | one P-core | ibid. |
| M **machine core vs pure vs OOP** | **gen-against-machine 3 587 best / 3 445 mean; pure gen 3 293 / 3 131; OOP — / 790** → "**+10 % over the pure core, and 4,3× over OOP**" | fps | three cores | `doOpcodes` (as the app runs), Manic Miner from a snapshot, one P-core, 5 interleaved runs each | ibid. §"Paso 5" |
| M contention hook vs helper vs nothing | 3 545 · 3 526 · **3 944** → contention work = **11 % of the frame**, the call = 0 | fps mean | between variants | same harness, 4 alternated pairs | ibid. |
| M contention unrolled, `times` literal | **3 815 vs 3 500** | fps mean | vs hook | same harness; six helpers 1x1,2x1,5x1,7x1,1x3,4x1 | ibid. |
| M dispatch split (machine core) | **3: 242 methods, largest 1 668, 4 071/3 853** · 4: 130 / 3 289 / 3 907/3 705 · 5: 74 / 5 657 / 3 454/3 081 | fps best/mean | between variants | 3 interleaved runs, `doOpcodes` on Manic Miner | ibid. |
| M module move A/B vs `c83c514a3` | gen playback **21 186 % → 20 773 %**, session **26 942 % → 26 253 %**; OOP playback 3 819 % → 3 841 %, session 4 472 % → 4 380 % | % of real speed | before vs after the module extraction | `jsw-full.rzx`, 3 interleaved rounds, one JVM per core, best run; noise ±15 % between equal runs on this notebook | `doc/plan-nucleo-generado-modulo.md` step 4 |
| M subsystem breakdown, start | core only 15 240 / 8 542 · +sound 15 442 / 8 231 · +screen 14 298 / 9 208 · **all 13 333 / 7 939** | fps (MM 48K / JSW 128K) | parts switched on one at a time | 2026-09-03, `Where` harness, app loop, generated core, JDK 21, `taskset -c 2,3`, 1-second blocks, plateau of last 4 | `doc/plan-superar-zxspin.md` §0 |
| M profile at start | MM: `contend5x1` **46,6 %**, `addTStates` 16,9 %, `dirty64`+`dirty8` 9,9 %, decode 11,7 %, `updateBorder` 4,2 % · JSW: `dirty64` **32,2 %**, `addTStates` 28,4 %, decode/read/step 16,4 %, keyboard 6,3 %, `updateBorder` 4,6 %, `Ay.synthesise` 2,8 % | % own time | — | JFR at 1 ms, 7 000–14 000 samples | ibid. |
| M contention by runs (§1) | MM 14 151 / 13 743 → **16 368 / 14 898 (+8 to +16 %)**, JSW 8 580 → 8 703 (noise); helper 147 → 46 bytes | fps | before/after | alternated, same cores | ibid. §1 |
| M clock without timeout (§2) | MM 15 838 / 14 586 → 16 768 / 14 971 (**+3 to +6 %**), JSW 8 041 → **8 852 (+10 %)**; **bytes/frame 951 → 756** | fps, bytes/frame | before/after | alternated, same cores | ibid. §2 |
| M one question per write (§3a) | MM 16 595 / 15 616 → 17 039 / 16 145 (**+3 %**), JSW 8 278 → 8 889 (**+7 %**); `write` 199 → 160 bytes | fps | before/after | alternated | ibid. §3 |
| M `byte[]` pages (§3b) | MM neutral (16 649 / 13 167 → 15 933 / 14 859), **JSW 7 882 → 11 697 (+48 %)** | fps | before/after | alternated | ibid. §3 |
| M ports/keyboard (§4) | JSW 11 195 → **11 950 (+7 %)**, MM within noise; bytes/frame 791 → 839 → 831 → **671** | fps | before/after | alternated | ibid. §4 |
| M screen (§5) | `dirtySinclair` **17,4 % → 0,9 %** of own time, **fps unchanged** (MM 15 995 / 14 790 → 16 327 / 14 318); screen as a subsystem costs **7 % MM, 2 % JSW** | % / fps | before/after | measured by switching it off | ibid. §5 |
| M subsystem breakdown, close of §5 | core only 17 355 / 13 135 · +screen 16 194 / 12 905 · +sound 15 955 / 11 082 · **all 15 256 / 11 896** → **+14 % and +50 % vs §0** | fps (MM / JSW) | vs §0 | same harness | ibid. §5 |
| M AY event-to-event (§6) | JSW 11 801 / 11 689 → **12 867 / 12 702 (+9 %)**; synthesis 16 % → ~1 % of the frame; synthesis alone ×13,7 in that state, ×11,7 after reset, ×2,7 playing a melody | fps / × | before/after | alternated; JSW never writes to the AY in the recording | ibid. §6 |
| M loop per instruction (§7) | neutral within ±6 % (MM 17 332 / 15 418 before vs 15 527 / 16 371 after) | fps | before/after | with IntelliJ at 47 % | ibid. §7 |
| M sound pacing | 100 % → 51 fps = 103 %, CPU/wall 0,01 · 2 000 % → 970 = 1 941 %, 0,08 · 15 000 % → 6 866 = 13 731 %, 0,48 · 60 000 % → 13 707 = 27 413 %, 0,99 · 15 000 % silent → 13 601 = 27 202 %, 1,00 | fps, %, CPU/wall | between speeds | real audio card, JSW recording | ibid. §9 |
| M pacing after the rule | 100 % → 49 fps, card at 44 125 samples/s · 300 % → 150 fps, frames of 150 samples, 44 106/s · 3 000 % → 1 501 fps, 44 119/s · 30 000 % → **14 177 fps (28 355 %, the machine's maximum)**, card falls short at 30 457/s | fps / samples/s | between speeds | machine loop with sound | ibid. §9 |
| **P** "the generated core runs at the speed of the reference emulator; ZXSpin runs **20 %** faster than it" | 20 % | % | the reference emulator (C), ZXSpin (Delphi/asm) | **premise of the document; no measurement of the reference emulator or ZXSpin exists anywhere in this repo** | ibid. line 3–4 |
| **P** structural advantages over the reference emulator | contention runs (the reference emulator does the five `contend_read_no_mreq` lookups); one question per write (the reference emulator asks five); dirty-per-cell (the reference emulator loops eight) | — | the reference emulator's C source | reasoned from `fuse-emulator-fuse/`; the third was **discarded** after §5 | ibid. §1, §3, §5 |
| **P** expected end state | JSW playback 760 → ~1 000–1 100 fps | fps | — | hypothesis table | `doc/plan-nucleo-generado-jvm.md` §"Lo que se espera" |

Noise floors stated: **±12 %** inside one unpinned run; **~1 %** pinned with the plateau method; **±3 %** on the RZX harness; **±8 %** on the micro-benchmark; **±15 %** between equal runs on the notebook; two identical `Where` runs differed **12 %** (13 309 vs 11 672 fps).

### 3. The optimization steps, in the order they were done

Phase 1 — model first, generator after (`doc/plan-nucleo-generado.md` §"Los pasos"):
1. **A. Aspects leave the model** — no file under `cpu`/`instructions`/`opcodes`/`registers` imports `com.fpetrola.z80.tstates` (commit `49de2b1d3`, −73 lines).
2. **B. Contention becomes values with a single hook** `contend(address, times, tstates)`, replacing 4 phases, 4 visitors, `CachedPhase`, `readCount`/`writeCount` (`24775046d`, −537 lines). "El núcleo OOP sale más rápido de acá aunque no se genere nada."
3. **D. MEMPTR gets one owner and explicit inputs** (`f395170d5`, −50 lines).
4. **Specializer over leaves**, rules 1–8: config fields → literals, collaborator calls → recursive inlining, `instanceof`/probes folded, lambda fields resolved through the constructor assignment, ALU via the delegate's `calculate*` (`16d29ae4d`).
5. **Aspects woven**: MEMPTR by specializing its visitor, contention by printing the instance's value list at the syntactic point each value names.
6. **From `OOZ80.execute()`**, with a ~60-line hand-written frame (R++, opcode read, prefixes, next PC), tables → nested `switch` by rule 4; split into methods of 16 opcodes to stay under HotSpot's 8 000-byte limit.
7. **Plugged in and measured** (`d6a5fdbc2`) → 3,5× on the synthetic benchmark; OOP stayed the default.
8. **The lock**: `GeneratedZ80IsCurrentTest`.
9. **C (fetch and executor): not done.**

Phase 2 — JVM-level, all inside the generator (`doc/plan-nucleo-generado-jvm.md`):
10. **A3 + A2**: dead-store elimination, copy propagation, folding `"BC".equals("BC")` (57), `InterruptionMode.values()[n]` (8), boxing (4); range facts per expression → 1 018 masks removed → bytecode −9,2 %.
11. **The register bank keeps its width** (`UnrolledRegisterBank` masks on every write/inc/dec) so the generator can *derive* the invariant → the 302 `A & 0xFF` and 739 more masks go, 28 labelled blocks → 0; found `IRRegister.decrement()` incrementing.
12. **A4**: "the tail of a `case` that only asks which branch was taken is copied into the branch" — a rule that names no instruction → `== -1` 273 → 14, cumulative −11,1 %.
13. **A6**: dispatch split measured, `GROUP_SHIFT` 4 kept (the one already there).
14. **M1** (clock timeout stops being a `Consumer<Integer>`), **M2** (one page lookup per access, one write per write), **M4** (undefined ED = 2 bytes / 8 T instead of throwing) → measured: **no movement**; M3 and M5 not done, reasons written.
15. **G1–G3**: `GeneratedSpectrumZ80` — the machine's memory taken apart into private `read`/`write` helpers, page table and contention tables as held fields → **+10 % over the pure core**.
16. **Contention unrolled with `times` literal** into six helpers → **3 815 vs 3 500 mean**.
17. **`groupShift` moved to the `Target`** — 8 for the machine core, 16 for the pure one (the answer inverted once the methods grew).

Phase 3 — the layers that are not the core, 2026-09-03 (`doc/plan-superar-zxspin.md`), each verbatim "Hecho":
18. §1 **contention by runs**: `Ula.noMreqRun(n)` tables, five dependent lookups → one → **+8 to +16 % MM**.
19. §2 **clock without timeout**: the tape waits on an `EventManager` event; `addTStates` down to the inherited sum; no lambda per port access → **+3 to +6 % MM, +10 % JSW**, bytes/frame 951 → 756.
20. §3a **one question per write** (`MemoryPage.screenBytes`) → **+3 % MM, +7 % JSW**.
21. §3b **`byte[]` pages**, which exposed a signed-vs-unsigned compare marking every byte ≥ 0x80 dirty → **JSW +48 %**.
22. §4 **ports and keyboard without allocating** (`readersOf`/`writersOf`, `Keyboard` resolves 256 rows on key change) → **JSW +7 %**.
23. §5 **screen**: two 64-bit divisions per changed byte removed, cell tables replaced by shifts → 17,4 % → 0,9 % of own time, **fps unchanged**; the per-cell redesign **discarded**.
24. §6 **AY event to event** (`quietSteps`) → **JSW +9 %**.
25. §7 **loop**: one shared empty bridge command, `zxClock` final → neutral.
26. §9 **sound as the pacer**: audio frame sized for the requested speed; below real time the card paces, above it the `Timer` and RZX `pace()` in nanoseconds; `JavaSoundDevice.dropWhenAhead` takes or drops a whole frame above real time.

Phase 4 — packaging, 2026-09-05 (`doc/plan-nucleo-generado-modulo.md`): module `machine/generated`, `Core` seam, `OopCore` default by `OptionalBinder`, `GeneratedCores` faucet with cache, app on the generated core with no flags. Parity within noise.

### 4. The correctness safety net

| battery | size (exact) | what it fixes | where |
|---|---|---|---|
| The CPU vector battery | **1355 tests** (docs); **1356 vector blocks** in `/home/fernando/detodo/spectrum/versions/oozx/emulator/src/test/resources/fuse/tests.in`; `doc/TABLA_ALU_TESTS.md` says "1356 casos" | registers, memory, MEMPTR, total T-states **and the exact list of bus events (MR/MW/MC/PR/PW/PC) with its instant** | `emulator/src/test/java/fuse/FuseTests.java` |
| `AluReferenceTest` | **131 072 combinations per instruction** (every accumulator × every operand × both carries), undocumented bits 3 and 5, driven through the processor | correctness against the published Z80, not against a baseline; **found 4 instructions that passed the battery and were wrong** | `emulator/src/test/java/com/fpetrola/z80/AluReferenceTest.java`; `doc/TABLA_ALU_TESTS.md` |
| `AllTableAluOperationsCompatibilityTest` | MD5 seal per ALU table (RLA = 512 entries; BiAndBoolean = 131 072; Tri = 16 777 216) | that a table did not change; explicitly "un sello dorado no dice que sea correcta" | `emulator/src/test/resources/table_alu_operations_config.json` |
| emustudio Z80 tests | **226 `@Test`** | instruction behaviour from a third-party suite | `emulator/src/test/java/net/emustudio/plugins/cpu/zilogZ80/` |
| **the same three, re-run on the generated core** | "**Las 1592 pasan**" | the processor is swapped by `ProcessorUnderTest` (ServiceLoader); "los tests no cambian por tener otra implementación del procesador" | `machine/generated/src/test/java/com/fpetrola/oozx/generated/EmulatorOnTheGeneratedCoreTests.java`, `GeneratedUnderTest.java` |
| machine suite re-run on the generated core | `model.tests`, tag `slow` excluded; phase-1 gate was **260** of `machine/core`, "los mismos hashes de boot que el OOP"; module total **338** (core 260, ui 16, devices 33, app 29, 1 Sept 2026); `app` **39, 0 failures** on the generated core | the machine, not the CPU | `machine/generated/src/test/java/com/fpetrola/oozx/generated/MachineOnTheGeneratedCoreTests.java`; `doc/plan-modulos.md` |
| **ROM boot comparison between cores** | 300 frames on each core; compares AF BC DE HL AFx BCx DEx HLx IX IY SP PC I R MEMPTR, `zxClock.getTStates()` and the whole RAM; plus 150+150 frames switching processor mid-boot against 300 straight | that the two cores are the same machine | `machine/generated/src/test/java/com/fpetrola/oozx/generated/GeneratedMachineCoreTest.java` |
| freshness lock | string equality between the regenerated core and the committed reference copy; `@Slow`, `-Doozx.slow=true` | that the artifact is what the model produces today | `GeneratedSpectrumZ80IsCurrentTest.java`; `emulator/src/test/java/model/tags/Slow.java` |
| cache behaviour | first machine writes source + classes; second loads without remaking; what runs equals the reference copy | | `GeneratedCoresTest.java` |
| contention on the real machine | `ZXSpectrumContendedMemoryTests` (**64 `@Test`**), `ZXSpectrumContendedMemoryTests2`, `ZXSpectrumULATests` (17), `ContentionRunsTest` (each table against the loop, **six machines, from every T-state**) | T-state by T-state | `machine/bridge/src/test/java/model/tests/cpu/`, `machine/core` |
| `EmulationRegressionTest` | hashes of screen, sysvars, RAM and registers after **200 frames** of ROM boot | | `machine/core` |
| RZX | fetch count per frame on real games; **20 `@Test`** in `zx-rzx` | determinism against recorded play | `/home/fernando/detodo/spectrum/versions/oozx/zx-rzx` |
| **the bridge to the reference emulator** | **212 `@Test` in 14 classes**, 71 files in the module | a JNA `LibretroCore` interface to `fuse_libretro.so` and a `LocalLibretroCore` implementing the *same* interface over the Java machine, driven through one command protocol (`TestDriver`, `CommandHandler`, `model/connected/*`), "the tests that proved one behaves as the other" | `/home/fernando/detodo/spectrum/versions/oozx/machine/bridge/`; README line 249 |

### 5. What the docs say about why the OOP model makes this possible

From `doc/plan-nucleo-generado.md`:

- **"El modelo no se adapta a la herramienta."** The generated core is "un artefacto de optimización que se obtiene *analizando* ese modelo, no una razón para cambiarlo".
- **The instance graph is fixed.** "Cada instancia de instrucción que fabrica el decodificador es un grafo de objetos fijo... Lo único que varía cuando corre son los valores de registros y memoria; todo lo demás quedó decidido al construir la tabla. Ésa es la situación en la que un **evaluador parcial** produce código especializado."
- **Aspects are the lever.** MEMPTR is kept out of the instructions because "es un registro no documentado, que puede estar o no en el procesador"; contention is kept out because "no es del Z80, es de la Spectrum". Both are visitors — "la forma que un especializador resuelve mejor: doble despacho sobre tipos que en la instancia están fijos". Hence: **"El núcleo generado es, literalmente, el modelo con sus aspectos tejidos y aplanados, y qué aspectos tejer es un parámetro del generador, no una decisión del modelo."**
- **Single source of truth.** "El dueño de la semántica sigue siendo el código OOP. El archivo generado es un artefacto, como un `.class`: no se edita, se regenera." And: "El núcleo generado es exactamente tan correcto como el OOP con los aspectos que se le tejieron. Por eso el gate es el mismo."
- **One mechanism, not templates.** "El generador tiene un solo mecanismo —inlining dirigido por el grafo de instancias— y no un template por instrucción. Una regla que nombre una instrucción es la señal de que algo del modelo está mal expresado."
- **Why the OOP core is slow, stated as a list of concrete costs, not as "because it's OOP"**: megamorphic call sites (5–10 per instruction; "el JIT no puede desvirtualizar un sitio que ve más de dos tipos"), phase dispatch per memory read, five lambda fields, `ObservableRegister`, `MemoryForOpcodes`, and 192 MB of ALU tables. "Esto es lo que el `switch` elimina."

The surgical-per-layer mechanism, from `doc/plan-superar-zxspin.md` §1: the ULA gets the run tables, `PhaseProcessorBase.contend` uses them, and **"el generador emite lo que el modelo diga, no hay que tocarlo"** — change a layer in the OOP model, regenerate, the flattened core carries the change. From §3: **"lo que el generador inlinea llega con las identidades resueltas; lo que el JIT inlinea, no. No conviene devolverle al JIT lo que el generador ya decidió."** And from `doc/plan-nucleo-generado-jvm.md`: the reason the boundary works at all is that **"todo el modelo de bancos de esta máquina es 'mutar la tabla de páginas'"** — a design property of the OOP machine, verified in code before the generator relied on it.

### 6. Caveats the docs state themselves

- **No measurement of the reference emulator or of ZXSpin exists in this repo.** The opening line of `doc/plan-superar-zxspin.md` ("corre a la velocidad del emulador de referencia. ZXSpin corre un 20 % más rápido que él") is a premise. The three "ventaja sobre la referencia" claims are reasoned from the reference emulator's C source; one of the three (dirty-per-cell) was afterwards discarded as not worth doing.
- **The `Where` harness that produced every §0–§7 number is not in the repository** — the doc says "hoy en el scratchpad de la sesión" and that it "va a `machine/app/src/test`"; no `Where*.java` exists. `RzxCoreMeasurement` and `LoopCoreMeasurement` exist and are gated on `-Doozx.measure=true`.
- **Step C (fetch and executor) is not done.** The generator therefore carries three debts: repeated reads of an operand byte count as one (`MemoryForOpcodes` semantics), fetch wrappers are inlined as part of the graph, and the frame is hand-written (~60 lines).
- **Three harnesses gave three different answers to the same change**: RZX said −13 %, the idle-ROM loop said nothing, the machine loop with a game said +10 %. "El que vale es el último, porque es lo que la app hace." The third block of `RzxCoreMeasurement` (own loop after `release()`) "nunca funcionó: también en el commit viejo se cuelga a los 60 s".
- **Everything measured before pinning to a P-core carries ±12 %.** "Cualquier número de este documento tomado antes de esto tiene ±12 % encima. Lo que sobrevive es lo que se midió intercalado muchas veces con diferencias grandes: el 4,5× del núcleo generado y el 14 % del `GROUP_SHIFT` 3."
- **A1/G1–G3's stated premise turned out false in measurement**: "las llamadas están, y sacarlas no sirve"; "el trabajo de la memoria de la máquina es trabajo, y ponerlo adentro del `case` no lo hace más barato". The whole prize for optimizing the machine's memory is 8 %.
- **With the generated core at 8,8 % of the JSW frame, "hacer la CPU infinitamente rápida da 10 %."**
- **A hole with no gate**: `GeneratedSpectrumZ80.read`/`.write` index `mapping.page[…]` with no guard, so a `DevicePage` (Opus, and the planned uSpeech) is never reached on the generated core. "**Ningún gate lo puede ver**": `OpusTest` exists and the generated-core run exists, "los dos existen y nunca se cruzan". Still open as of 2026-09-06.
- **The generated core does not report memory events** (MR/MW/MC) because it reaches memory without a call; on it, the reference emulator compares registers, memory, T-states and port events only.
- **M3 and M5 were not done**, with measured reasons; 14 `== -1` remain (HALT's `nextPC` field); DD/FD and DDCB/FDCB tables are generated whole (no sharing).
- **The audio line opens mono** and the mixer writes interleaved L/R as consecutive samples — "la resolución efectiva es de 22 kHz". Noted "para otro día".
- The MEMPTR aspect is not installed by the machine on the OOP core; the generated core has it woven in — so the two cores differ in that respect outside the boot comparison.

## Appendix F. Object-oriented Z80 (sonnet)

### OOZX `emulator` module — architecture notes for the README

Scope confirmed: `emulator/src/main/java` (228 files, package root `/home/fernando/detodo/spectrum/versions/oozx/emulator/src/main/java/com/fpetrola/z80`), `emulator/src/test/java` (50 files), `emulator/pom.xml`. Total 278 `.java` files, matching the figure given.

### 1. Package layout (under `com/fpetrola/z80/`, plus two sibling roots)
- `base` — the single `InstructionVisitor<R>` contract everything else hangs off.
- `instructions/types` — `Instruction` + `AbstractInstruction` + the shape hierarchy (target/source/conditional/block/repeating/bit/ALU).
- `instructions/impl` — 76 concrete opcode classes, one per Z80 instruction (e.g. `Add.java`, `Ldir.java`, `RLCA.java`).
- `instructions/factory` — `InstructionFactory` + `DefaultInstructionFactory` (wires opcodes to shared registers/memory).
- `instructions/cache` — prototype cloning + PC-keyed instruction cache.
- `opcodes/references` — the operand model (`OpcodeReference`, `Condition`, memory/indexed references).
- `opcodes/decoder(+/table)` — the opcode dispatch tables and prefix generators.
- `registers(+/flag)` — `Register`/`RegisterPair`/`RegisterBank` and the ALU flag-table machinery.
- `cpu` — `State`, `OOZ80` (the run loop), `Core`/`OopCore`, fetch/execute plumbing, `IO`.
- `spy` — `ExecutionListener`/`InstructionSpy`/`ObservableRegister`: the generic tracing/attachment seam.
- `se` — a small, mostly vestigial symbolic-execution seam (`DataflowService`, two value classes).
- `transformations`, `helpers`, `minizx`, `bytecode`, `z80core` — small supporting/legacy pieces (base64/test helpers, a standalone mini memory+stack-exception sandbox, a Spanish-commented `bytecode` adapter, a lone `IntMode` enum).
- The package `com/fpetrola/z80/tstates`, with `RecordingPhaseProcessor` beside it, — the reference emulator's contention/timing model, kept outside `com.fpetrola.z80` entirely.

### 2. Instruction model
Every instruction is a class implementing the 3-method `Instruction` interface (`execute()`, `getLength()`, `accept(InstructionVisitor)`) — 76 files in `instructions/impl`. `AbstractInstruction` is the common base (length, `nextPC`, R-register delta, optional `AluOperation`). Intermediate types layer on shape: `TargetInstruction`/`SourceInstruction`/`TargetSourceInstruction`, `ConditionalInstruction<C extends Condition>`, `BlockInstruction`, `RepeatingInstruction`, `BitOperation`, `ParameterizedUnary/BinaryAluInstruction`, `DummyInstruction`, `FlagInstruction` (all in `instructions/types`). Operands are `OpcodeReference`/`ImmutableOpcodeReference`/`MutableOpcodeReference` implementors in `opcodes/references`: `Register` itself is one, plus `Memory8BitReference`/`Memory16BitReference` (PC-relative immediates), `IndirectMemory8/16BitReference`, `MemoryPlusRegister8BitReference` ((IX+d)/(IY+d)), `ConstantOpcodeReference`, `NullOpcodeReference`. Conditions mirror this: `Condition`/`ConditionBase`/`ConditionFlag`/`ConditionAlwaysTrue`/`BNotZeroCondition`, pluggable via a `ConditionPredicate` hook. `InstructionFactory`/`DefaultInstructionFactory` is a one-method-per-opcode factory closing over the shared register/flag/memory objects. The visitor is `com.fpetrola.z80.base.InstructionVisitor<R>` — ~90 default no-op methods, one per concrete instruction/operand type; every instruction/operand `accept`s into it. Visitors found: `InstructionCloner`+`OpcodeReferenceCloner`+`ConditionCloner` (deep clone), `MemptrUpdater`'s private `Before`/`After`/`Repeating` visitors, and `z80/tstates/PhaseProcessor(Base)` (contention). No dedicated disassembler/pretty-printer visitor exists; text form comes from ad hoc `toString()` overrides (`AbstractInstruction`, `DefaultTargetInstruction`, `BitOperation`, `ConditionFlag`). Decoding is table-driven along Cristian Dinu's opcode-byte decomposition (`opcodes/decoder/table/{Unprefixed,CBPrefix,EDPrefix,IndexerRegister,DDCBFDCBPrefix}TableOpCodeGenerator.java`, `TableBasedOpCodeDecoder.java`) — credited by name in the repo's top-level README/CLAUDE.md and cited (z80.info/decoding.htm) in `emulator/src/test/java/net/emustudio/plugins/cpu/zilogZ80/Z80Tests.java`, though not restated inside `emulator/src/main`. Each opcode's `Instruction` is built once as a shared prototype inside the 256-entry tables (`DefaultFetchNextOpcodeInstruction`, `MultiOpcodeFetcher`); `InstructionCache`/`CachedInstructionFetcher` clone a prototype per PC address via `InstructionCloner` and invalidate the clone through a `MemoryWriteListener` (`CacheInvalidatorMemoryWriteListener`) on self-modifying writes. **Instruction-class count**: `find ... -path '*instructions*' -name '*.java' | wc -l` → **104** (76 impl + 17 types + 8 cache + 3 factory).

### 3. Registers and flags
`Register` (extends `OpcodeReference`) is the base contract; `Plain8BitRegister`/`Plain16BitRegister` hold a masked `int`; `RRegister` special-cases the R register's bit-7 latch. `RegisterPair` composes two `Register`s into a 16-bit view: `Composed16BitRegister<R extends Register>`, `PlainComposed16BitRegister` (fast path when both halves are exactly `Plain8BitRegister`), `InvertedComposed16BitRegister` (shadow/IX/IY pairs). `RegisterBank` + `RegisterName` enum cover AF/BC/DE/HL, the alternate set (AFx/BCx/DEx/HLx), IX/IY, IR, PC, SP, plus two non-hardware pseudo-registers: `MEMPTR` and `VIRTUAL`. `DefaultRegisterBankFactory` builds the normal object-graph bank; `UnrolledRegisterBank` is an alternate bank backing every register with one unrolled `int` field and a nested wrapper class per register, explicitly "so a generated core can read and write them without going through an object" (its own Javadoc) — direct evidence the same `Register` contract is meant to be re-backed by a generator elsewhere. `Flags` holds the 8 flag-bit constants and get/set helpers. Per-opcode flag computation lives in `AluOperation` subclasses nested inside each instruction class (e.g. `RLA.RLAAluOperation`, `Add.Add8TableAluOperation`); `CachedTableAluOperation` wraps these into a lazily-built, class-keyed lookup table (up to 256×256×256 ints) shared statically across all instances (`TABLE_CACHE`), built from precomputed half-carry/overflow/parity/SZ53 tables in `AluOperationBase`.

### 4. Memory contract, MEMPTR, contention
`com.fpetrola.z80.memory.Memory` is the interface the CPU uses: `read(address, fetching)`/`write`, 16-bit helpers, `addMemoryReadListener`/`addMemoryWriteListener`, untimed `peek`/`poke` ("no time taken, nobody told"), `reset`/`copyFrom`. Contention/timing is not in `Memory` or `Instruction` at all: `z80/tstates/PhaseProcessor(Base).java` is itself an `InstructionVisitor<Integer>` **and** an `ExecutionListener`, wired in from outside via three seams — as the CPU's `ExecutionListener` (`OopCore.cpu()`: `cpu.getInstructionExecutor().setExecutionListener(contention)`), as `MemoryReadListener`/`MemoryWriteListener` (`z80/tstates/AddStatesMemoryReadListener`/`AddStatesMemoryWriteListener`), and as an `IO` decorator (`AddStatesIO`). Its own Javadoc: "the only knowledge of the Spectrum's timing in the emulator; the Z80 model does not know this class exists." MEMPTR is an ordinary `Register` (`RegisterName.MEMPTR`, `state.getMemptr()`); it's kept correct by `MemptrUpdater` (two private `InstructionVisitor` implementations, `Before`/`After`/`Repeating`), invoked from `MemptrUpdateInstructionSpy`, an `InstructionSpy` attached exactly like contention (`new MemptrUpdateInstructionSpy(state).addExecutionListeners(...)`). So both aspects are "hung off" the same two general seams (`InstructionVisitor` to know *what*, `ExecutionListener`/memory-listener to know *when*), never by subclassing instructions.

### 5. Generic type parameter
`Instruction`/`AbstractInstruction` carry **no** generic type parameter in this module today, and registers hold plain `int` (`Plain8BitRegister.data`, `Plain16BitRegister.data`). Narrower generics remain (`ConditionalInstruction<C extends Condition>`, `TargetSourceInstruction<S extends ImmutableOpcodeReference>`, `Composed16BitRegister<R extends Register>`) but these parameterize operand/condition *types*, not a value type. `git log -S WordNumber` shows an `Instruction<T extends WordNumber>`-shaped design existed and was deliberately removed (commits "refactor: remove generic type parameters from multiple interfaces and classes", "refactor: replace WordNumber.createValue calls with direct instantiation…"); a stale `emulator-0.0.2-alu-SNAPSHOT.jar` still contains a compiled `WordNumber.class` from before that refactor. What's left inside `emulator` is the small `com/fpetrola/z80/se` package: `DataflowService` (an as-yet-unused seam: `findValueOrigin`/`findCurrentReturnAddress`/`isSyntheticReturnAddress`) and two plain, non-generic carriers, `DirectAccessWordNumber` and `ReturnAddressWordNumber`, kept only to hold "the identity that WordNumber used to give" (its own Javadoc) for stack-provenance tracking. So: what the generic once enabled (distinguishing a value's *origin* from its bits, for symbolic execution/virtual registers) is not currently expressed as a type parameter here — it survives only as these two small value classes, with any fuller symbolic-execution machinery living in the separate `translation` module (out of scope).

### 6. Tests
- **The vector battery** (`emulator/src/test/java/fuse/{FuseTests,FuseTest,FuseTestParser,FuseResult,RecordedEvents}.java`) replay `emulator/src/test/resources/fuse/tests.{in,expected}` (9152 / 18912 lines, ~1355 test-id blocks) instruction-by-instruction against the reference emulator's recorded registers/memory/bus events.
- **ALU reference tests** (`emulator/src/test/java/com/fpetrola/z80/AluReferenceTest.java`) check every ALU/rotate/shift/BIT/16-bit/block instruction against a from-the-spec reference over its full input space (up to 131072 combinations/instruction); its Javadoc documents 4 real bugs it alone caught that the reference emulator's one-vector-per-opcode suite missed.
- **Table-ALU regression** (`registers/flag/{TableAluOperationRegistry,TableAluOperationExporter,AllTableAluOperationsCompatibilityTest,UpdateMd5Test}.java`) reflectively finds every nested `AluOperation`/table field, MD5s its precomputed table, and diffs against baselines in `table_alu_operations_config.json` (41 operations).
- **emuStudio instruction tests** (`net/emustudio/plugins/cpu/zilogZ80/*`, using the `net.emustudio:cpu-testsuite_12.0` dependency from Peter Jakubčo's emuStudio project): `ArithmeticTest`(49)/`LogicTest`(60)/`TransferTest`(52)/`ControlTest`(24)/`BitTest`(22)/`IOTest`(14)/`StackTest`(4) parameterized cases, plus `Z80Tests` (extends `InstructionsTest`, 215 opcode/CRC16 entries, "inspired by … z80.info/decoding.htm … raxoft/z80test"), driving `OOZ80` via `CpuImpl`/`EmulatorEngine`.
- No zexall/zexdoc harness exists anywhere under `emulator`.
- **`ProcessorUnderTest`** (`emulator/src/test/java/com/fpetrola/z80/ProcessorUnderTest.java`) is exactly the service seam: it extends `Core`, and `found()` does `ServiceLoader.load(ProcessorUnderTest.class).findFirst().orElseGet(Oop::new)`. Both `FuseTestParser` and `AluReferenceTest` build their CPU through it, defaulting to the OOP core (`OopCore`) here but letting another module's core register itself and rerun the same batteries unchanged.
- `model/tags/Slow.java` (30 min timeout, gated by `-Doozx.slow=true`) is defined here "for the gate that runs on every change", noting "the generator, which rebuilds the whole core from the model to see whether the committed one still matches it" — evidence of a generator-diff test elsewhere; unused by any test currently inside `emulator`.

### 7. Notable for a README
Every instruction and operand is a plain object that can be **executed**, measured, **visited**, **cloned** (`InstructionCloner`/`OpcodeReferenceCloner`/`ConditionCloner`), and **printed** — and that same model is reused unmodified by an interpreter (`OOZ80`), a timing layer matched cycle-for-cycle against the reference emulator (`PhaseProcessor`, itself just another `InstructionVisitor`), a register/memory tracer (`spy`), and, by the design evident in `Core`/`UnrolledRegisterBank`/`ProcessorUnderTest`/`Slow`'s own Javadocs, a source-code generator in a sibling module that regenerates a flat, object-free core from this model. Decoding and execution share one data structure: the opcode tables are themselves arrays of `Instruction` (including "fetch one more byte" prefix instructions). Self-modifying code is handled explicitly via cache + write-listener invalidation. MEMPTR/WZ, though undocumented on real Z80 docs, is first-class here — a real `Register` kept correct by a dedicated visitor on the same generic attachment seam as everything non-essential to raw instruction semantics.

## Appendix G. Commit history (sonnet)

### Method

Read the full `git log --format='%ad %h %s' --date=short --reverse` (1389 commits, 2024-11-05 → 2026-09-06) in three chunks, plus targeted `grep` passes to pin down exact hashes/wording for specific feature keywords. No source files were opened.

### 1. Timeline

- **2024-11**: Project start; Z80 core bring-up against the CPU vector battery (ALU/flags/tstates correctness), first game decompilation experiments (Dynamite Dan).
- **2024-12**: Core correctness continues (stack handling, virtual registers); first full game ports run end-to-end (Dynamite Dan, Sam Cruise, Wally/JSW); Java 21 CI added.
- **2025-01**: Two commits only ("wally gif"), then a ~8 month gap.
- **2025-09**: Resume after hiatus — Swing display component, dual-implementation test driver, the reference emulator's keyboard mapping, contended-memory timing tests.
- **2025-10** (312 commits): First half is a systematic refactor removing static state and introducing DI/startup-module architecture and a Port/PortHandler peripheral abstraction. Second half is the first real desktop app (ZXSpectrumDesktopApp/OOSpectrumLauncher), working TAP/TZX tape loading, 48K/+2/+3 ROM loading, and early performance work.
- **2025-11** (221 commits): Sound (AY8912 + blip-buffer), UI polish (mute, window state, z-order, shortcuts, look-and-feel), snapshot save/load, a Pokes/Cheats system, and early game-details UI.
- **2025-12** (61 commits): Core ALU/instruction consolidation only (translation internals); then an 8-month gap.
- **2026-08** (183 commits): Single "jni" commit on the 11th; from the 27th, a rapid, richly-documented sprint — Guice DI rewrite, ZXInfo-backed game browser, RZX playback/recording, cassette browser, display pipeline (xBRZ/scanlines/blur/named presets), a whimsical 3D sprite viewer, docking tool windows, Pentagon machine, 128K/+3 stereo sound, +3 disk controller turned on.
- **2026-09** (208 commits through the 6th): ZX Printer, Kempston Mouse, real-cassette-via-soundcard input, session save, gamepad support, a module-boundary split (machine/ui, machine/app, machine/devices); then a generated/fast Z80 core with a heavily-measured performance diary; a one-sweep addition of most historical expansion hardware (Interface 1/2, Multiface, +D, DISCiPLE, Beta 128, Opus Discovery, Didaktik, DivIDE/ZXATASP/ZXCF/DivMMC/ZXMMC, Covox/SpecDrum/Fuller); settings-file overhaul; final removal of dead stubs.

### 2. User-visible features (deduplicated)

**Desktop app / windowing**
- Desktop app & launcher — `dcd20858d` "ZXSpectrumDesktopApp", `20672c9f8` "OOSpectrumLauncher"
- Docking "clipped" tool windows (deck/printer follow an emulator, movable between machines) — `1151ddcbe` "make the cassette deck a deck: it plays into the machine it is clipped to", `53cffd947` "carry a window clipped onto one machine over to another, and leave the keyboard behind"
- Multiple emulator windows with edge-snapping — `f5246eb56` "While a drag would snap to a machine, both windows carry a soft edge"
- Fullscreen — `7c7334368` "wake the fullscreen button, which asked an internal frame to maximise like a window"
- Recent-files list — `a06e7643d` "An emulator opens at the Spectrum's own size, and Ctrl+1 to Ctrl+0 open the recent files"
- Session save/restore — `3168c88fd` "write the machine down, so getting back to an afternoon's work is opening a file"
- Keyboard shortcuts / status bar — `cf267b9fa` "shortcuts", `177304778` "statusbar"
- Look-and-feel / themes — `37cdd9279` "l&f", `fb8b3b171` "remember the recording, the theme, and say when something is loading"
- Unified settings dialog (one config file, per-section classes) — `9fb9600ae` "One configuration file, a section per part, and a section is a class", `3e61eff00` "Of the two hundred and fifty three settings, the hundred and forty six nobody names are gone"

**Display**
- Video "lead" simulation (choose how the picture arrives) — `352eaa18d` "show the picture the way the lead it came down would have", `a449af1c3` "choose which lead the picture arrives through, and keep the choice"
- Upscaling/CRT filters (xBRZ, scanlines, gaussian blur) — `2edb29082` "add xBRZ, which is the one that actually rounds a diagonal off", `ab9359ad2` "draw scan lines by where a row falls in a machine line, not by whether it is odd"
- Saved/named display presets — `9aa453bdc` "a box of looks above the knobs, and the ones you save survive"
- Border on/off toggle — `1d9d173ce` "put the border behind a button, and start with it off"
- 3D sprite viewer (sprites extruded into lit 3D models) — `765ac1e4d` "3d viewer", `7d3641884` "a sprite given a body: xBRZ for the outline, a distance field for the bulge"

**Sound**
- AY-3-8912 chip + 128K/+3 stereo — `b21b2ce45` "refactor audio and emulator core; add AY8912 support and improve sound handling", `7c5ebcd9d` "mix in stereo so a source can sound different in each ear, and clear out what was left behind"
- Melodik add-on for 48K — `113f627e8` "plug a Melodik into a 48K, so a machine with no sound chip can still have one"
- Mute — `6df350161` "Mute"

**Machines / core**
- Multiple machine models incl. Pentagon, with a machine picker — `dec3f8580` "add the Pentagon, a machine that contends nothing, to see what adding a machine costs", `aa02dac0c` "make the machine box say what the machine is, and mean it when somebody picks one"
- TZX-declared machine auto-selection — `ab2895c84` "ask the tape which machine it wants, since a TZX says so and was being ignored"
- Fast core generated from the OOP model, switchable (even live) via a setting — `d6a5fdbc2` "let the machine run on the generated core, behind -Doozx.cpu=generated", `1b8352d5d` "The machine can be moved from one processor to the other while it runs, and the settings window is where you ask"
- Bridge module to the reference emulator for cross-checking correctness, a module of its own that the emulator does not know exists; and every module green on both cores
- Turbo speed control (rocket icon, slider to 30000%) — `451fc2bb6` "say turbo with the rocket that turns it on, lit or grey", `497f7c1a8` "The speed slider goes up to 30000 percent"

**Storage / tape**
- Cassette browser + audible tape loading (TAP/TZX) — `182062a01` "add a cassette browser to see and drive a tape", `aa8ee73ea` "make a load audible, and let a cassette exist without an emulator"
- Fast-load / multiload preference — `ec9525d56` "prefer the 48K download, and stop spooling a multiload after the first level"
- Load a game straight out of a zip — `9ef5b490e` "pick the file to load out of a zip instead of taking the first one"
- Real cassette via sound-card line-in — `f629696f2` "read a real cassette: watch what comes in the sound card, and put it on the machine's ear"
- Snapshot save/load, machine-aware — `24f36e6d5` "SnapshotSaver", `51e07bff3` "load a snapshot on the machine it was taken on"
- +3 disk controller with real `.dsk` images — `c080a94bd` "switch on the +3's disk controller, which had never once run", `0c7608d6f` "A .dsk is read, so a real +3 disk can be put in the drive"
- Third-party disk interfaces (+D, DISCiPLE, Beta 128, Opus Discovery, Didaktik 40/80) — `fc9ee2183` "The floppy stack, the +D on it, and the printer on the +D's port and the +3's", `45d2a66e9` "Opus Discovery and Didaktik 40/80, on what every WD interface shares"
- Interface 1 (8 Microdrives, RS232 terminal, ZX Net) — `21e642605` "Interface 1: eight Microdrives, the RS232 with a terminal on the desk, and the ZX Net"
- Interface 2 cartridge slot — `bada6458c` "Interface 2: a cartridge takes the place of the ROM, in a module of its own"
- Multiface freeze cartridge (One/128/3) — `8bfe3edbb` "Multiface One, 128 and 3: the red button, and what the processor needed for it"
- IDE/SD storage (DivIDE, Simple 8-bit IDE, ZXATASP, ZXCF, DivMMC, ZXMMC) — `ed088a2b5` "DivIDE: the automapper's EPROM and RAM, and an ATA channel on HDF files", `c6874134c` "DivMMC and ZXMMC: a card the machine talks to a byte at a time"
- Covox/SpecDrum DACs, Fuller Box — `a9b5e64e8` "Beta 128, Covox, SpecDrum and the Fuller Box - and two bugs the Beta found in the core"

**Input / output devices**
- Kempston joystick via physical gamepad — `081f9ef97` "A gamepad on the desktop is the Kempston joystick of the machine in front"
- Kempston Mouse with a debug window — `a9608239a` "point at a Spectrum: a Kempston Mouse, and a window to see it working"
- ZX Printer (COPY-driven, "burn" rendering, zoomable paper) — `f8ac32afb` "the belt and the paper of a ZX Printer", `bed38115d` "draw the printout as a burn and not as a bitmap"

**Game library**
- Game browser backed by ZXInfo (search, filters, screenshots, map tab) — `2be040a18` "update ZXInfo API v3 mapping and add a Game map tab", `f9393819f` "map the ZXInfo /suggest and /filecheck endpoints"
- Favourites window — `c2232810d` "keep games as favourites, and open them again from a window of their own"
- Pokes/cheats system (.pok parsing, per-game matching) — `6bcf3a495` "feat: Add Pokes/Cheats system with game matching", `d2fcdbe15` "feat: Add structured .pok instruction parsing system"
- RZX playback & recording with a player window — `b80110529` "bring the RZX player and recorder in from the emulador-alu branch", `b061f005a` "add a window to play a recording and watch it run"
- RZX Archive catalogue joined to ZXInfo search — `5e9645f7f` "catalogue the RZX Archive as data, joinable with the games we already look up", `e56446199` "offer a game's recordings from the browser, from both catalogues"
- Multiple simultaneous RZX playbacks — `aed25ec59` "watch more than one recording at a time, each with its own machine"

**Not confirmed**: the user's example list mentioned "drive bay window," "on-screen keyboard," and "Timex modes" — none of these literally appear. Timex only shows up as `efdbefc4b`/`09ef85f8c` "removing isTimex" (2025-10-13), which reads like removing a special-case flag, not adding/dropping the machine; no later Timex-specific commits exist, so it's unclear if Timex is actually a supported model today.

### 3. Removed / stubbed features

- `0402769f6` "The movie recorder is a stub nothing can see, so it goes"
- `46e8a7b61` "The stub of libspectrum goes, and with it the snapshot path nothing ever walked"
- The imported user interface goes, and takes with it a guard that was swallowing every joystick event
- Two more imported stubs go: the phantom typist, and a utils that was one method
- `a88209341` "Settings is gone"
- `7525d169f` "remove the second emulator window, which only its own main ever opened"
- `c7ca351a1` "retire the enum of every device that might one day exist"
- `df6d51a61` "bury the peripheral struct nothing ever used"
- `8d059a514` "delete the startup manager, since there is no order left for it to resolve"

### 4. Measured performance / emulator comparisons

- `1b44442ed` "The instrument was broken; measured properly, the machine's memory is worth 8 percent"
- `69f2fda79` "Where the 3 percent is: the call, not the body, so it needs the code inside the class"
- `0e64633c0` "Measured the way the app runs: the machine's core is 10 percent over the pure one"
- `2c11dae44` "Correction: the contention hook is a real call, and its work is 11 percent"
- `be38748e0` "The contention is generated in too, unrolled, and that is 9 percent"
- `10e87e9bb` "A plan for what is left between the generated core and ZXSpin, measured" (only explicit comparison to another emulator by name)
- `1e810e50a` "The replay was waiting on the collector: its input queue was reallocated four megabytes a frame"

No commit subjects mention the reference emulator or ZXSpin with a specific speed ratio/number — only the ZXSpin comparison above is named; all other numeric performance claims are internal (percent overhead of memory/contention access in the generated core).
