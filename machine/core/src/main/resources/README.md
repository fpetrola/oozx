# OOZX: Object-Oriented ZX Spectrum Emulator

OOZX is a modern, high-performance emulator for the ZX Spectrum and its various models, written entirely in Java with a pure object-oriented architecture. Built on principles of modularity, extensibility, and polymorphic design, OOZX provides an authentic emulation experience while maintaining clean, maintainable code through advanced design patterns.

## 🎮 Features

### Multi-Model Emulation
- **Multiple ZX Spectrum Models**: Support for Spectrum 16K, 48K, 128K, Plus 2, Plus 3, and Pentagon
- **Accurate Z80 CPU Emulation**: Precise instruction-by-instruction emulation inspired by [FUSE](http://fuse-emulator.sourceforge.net/)
- **ULA & Graphics Rendering**: Real-time video memory rendering with accurate color and timing
- **Sound Support**: AY-3-8912 sound chip emulation for authentic audio
- **Tape Loading**: Support for TAP and TZX tape formats

### Unified Multi-Game Environment
- **Multi-Window Interface**: Run and manage multiple game emulators simultaneously, each in separate windows
- **Independent Controls**: Each emulator instance has its own controls, state, and configuration
- **Game Browser**: Built-in game discovery using public APIs - search and load games directly from the interface
- **State Preservation**: Automatic saving and restoration of application state between sessions
  - Saves window positions, sizes, and z-order
  - Preserves game snapshots and load states
  - Remembers recent files and game browser queries

### Object-Oriented Architecture
The emulator leverages advanced OOP principles and design patterns:

- **Polymorphic Peripherals**: CPU, Memory, ULA, Sound, and other hardware components are fully decoupled through abstract interfaces
- **Pluggable Architecture**: Add or remove peripherals transparently without affecting core emulation logic
- **Visitor Pattern**: Efficient instruction processing and analysis
- **Factory Pattern**: Centralized component instantiation for consistency
- **High Performance**: Strategic use of polymorphism without sacrificing execution speed through modern JVM optimizations

### User Experience
- **Modern GUI**: Swing-based interface with tool bar and menu system
- **Keyboard Shortcuts**: Comprehensive shortcuts for all major operations
  - **Ctrl+N**: New Emulator
  - **Ctrl+O**: Open File
  - **Ctrl+Space**: Pause/Resume
  - **Ctrl+T**: Toggle Turbo Mode
  - **Ctrl+M**: Toggle Mute
  - **Ctrl+W**: Close Window
  - **F1**: View README
  - And more...
- **Multiple Look & Feel Options**: Darcula, OneDark, Solarized, IntelliJ themes
- **Window Management**: Cascade, Tile, and manual window arrangements
- **Help System**: Integrated README viewer and About dialog

## 🏗️ Architecture Highlights

### Modular Design
Each component (CPU, Memory, Graphics, Sound) implements well-defined interfaces:
```
┌─────────────────────────────────────┐
│    ZX Spectrum Emulator             │
├─────────────────────────────────────┤
│  ├─ Z80 CPU (Polymorphic)           │
│  ├─ Memory System (Pluggable)       │
│  ├─ ULA Graphics (Decoupled)        │
│  ├─ Sound Chip (Modular)            │
│  └─ Peripherals (Optional)          │
└─────────────────────────────────────┘
```

### Polymorphic Instruction Handling
Instructions are modeled as first-class objects, enabling:
- Dynamic instruction dispatch without massive switch statements
- Easy addition of new instruction types
- Efficient performance through JVM optimization of virtual methods

## 🚀 Getting Started

### Prerequisites
- **Java 21 or later**
- **Maven 3.6+**

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/fpetrola/oozx.git
   cd oozx
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the emulator:
   ```bash
   cd machine
   mvn exec:java -Dexec.mainClass="com.fpetrola.oozx.fuse.OOSpectrumLauncher"
   ```

### Using the Emulator
1. **Load a Game**:
   - File → Open (Ctrl+O)
   - Or use Game Browser (Ctrl+B) to search online
   - Or select from Recent Files

2. **Create Multiple Emulators**:
   - Emulator → New Emulator (Ctrl+N)
   - Each runs independently with separate controls

3. **Save & Load Game States**:
   - File → Save State (Ctrl+S)
   - File → Load State (Ctrl+L)
   - States are preserved automatically between sessions

4. **Control Playback**:
   - Pause/Resume: Ctrl+Space
   - Turbo Mode: Ctrl+T (faster emulation)
   - Mute/Unmute: Ctrl+M

## 📊 Supported Formats
- **Games**: Z80, SNA, TAP, TZX (snapshot and tape formats)
- **Models**: 16K, 48K, 128K, +2, +3, Pentagon

## 🎯 Performance
OOZX achieves high-performance emulation through:
- **JVM Optimization**: Modern JIT compilation of polymorphic calls
- **Efficient Memory Model**: Direct array-backed memory with proper cache alignment
- **Turbo Mode**: Variable speed execution for fast-forward gameplay
- **Minimal Overhead**: Polymorphism implemented with zero runtime cost on modern JVMs

Typical performance: 50+ FPS on modern hardware

## 🔧 Advanced Features

### Game State Persistence
- **Automatic Snapshots**: Game state is automatically saved to the application configuration
- **Compressed Storage**: State snapshots are compressed and stored in `~/.oozx/config.json`
- **Multi-Game Sessions**: Run 10+ games simultaneously, each with its own preserved state

### Game Discovery
- **Online Game Browser**: Integrated browser for finding classic ZX Spectrum games
- **Public API Integration**: Uses ZxInfo and World of Spectrum APIs
- **Direct Loading**: Download and run games directly from the interface

## 📝 Configuration
Application state is stored in `~/.oozx/config.json`:
- Window positions and sizes
- Recent files list
- Game browser search history
- Saved game snapshots
- User preferences

## 🤝 Contributing
Contributions are welcome! Areas for improvement include:
- Additional ZX Spectrum models
- Enhanced audio emulation
- Debugger and disassembler tools
- Improved game compatibility
- Performance optimizations

Please submit pull requests via GitHub.

## 📄 License
OOZX is licensed under the **Apache License 2.0**.
See [LICENSE](LICENSE) file for details.

## 🏛️ Inspiration
This project is inspired by [FUSE](http://fuse-emulator.sourceforge.net/), a legendary ZX Spectrum emulator, but reimplemented from the ground up with a focus on modern software engineering practices and Java best practices.

## 🙏 Acknowledgments
- [FUSE Emulator](http://fuse-emulator.sourceforge.net/) - for inspiration and emulation reference
- [ZxInfo API](https://zxinfo.dk/) - for game discovery
- [World of Spectrum](http://www.worldofspectrum.org/) - for game preservation
- The retro computing community for their invaluable knowledge

## 📚 Learn More
- Visit the [GitHub Repository](https://github.com/fpetrola/oozx)
- Check out retro computing communities:
  - [World of Spectrum](http://www.worldofspectrum.org/)
  - [Sinclair Wiki](http://www.sinclairzxworld.com/)
  - [ZX Spectrum forums](https://www.spectrumcomputing.co.uk/)

---

**Happy Emulating!** 🎮✨
