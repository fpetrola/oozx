# OOZX: Object-Oriented Z80 Emulator for Translating ZX Spectrum Games to Java

[![Build and Deploy](https://github.com/fpetrola/oozx/actions/workflows/maven.yml/badge.svg)](https://github.com/fpetrola/oozx/actions/workflows/maven.yml)

OOZX is an open-source Z80 emulator designed with an object-oriented architecture to facilitate the automatic translation of ZX Spectrum games into Java. This project emphasizes modularity, extensibility, and advanced instruction handling for robust emulation and translation.

![Gameplay de ZX Spectrum en oozx](doc/jsw1.gif)  
![dan-95%.gif](doc/dan-95%25.gif)
![wally-90%.gif](doc/wally-90%25.gif)

## Features

### Core Capabilities
- **Game Translation**: Converts ZX Spectrum binaries into Java bytecode or source code, providing cross-platform compatibility and code analysis opportunities.
- **Symbolic Execution**: Analyzes all potential code paths without requiring gameplay, ensuring comprehensive translation coverage.
- **Basic Self-Modifying Code (SMC) Handling**: Detects and adapts instructions that write to mutable memory regions, with plans for advanced SMC analysis.

### Object-Oriented Z80 Emulation
- **Instruction Visitor Pattern**: Facilitates instruction traversal for cloning, transformation, and analysis.
- **Instruction Factory**: Centralizes instruction instantiation to maintain consistency.
- **Prototype Pattern**: Ensures efficient reuse and management of instruction objects.
- **Virtual Registers**: Abstracts Z80 registers for adaptable dataflow analysis and emulation.
- **Generic Data Types**: Supports flexible handling of diverse instruction semantics.
- **Instruction Hierarchy**: Organizes instructions for clarity and extensibility.
- **Opcode Decoding**: Employs Cristian Dinu’s comprehensive Z80 opcode decoding methodology ([z80.info](http://www.z80.info/decoding.htm)).

### Auxiliary Tools
- **Screen Renderer**: A Swing-based graphical component for real-time visualization of ZX Spectrum screen memory.
- **Bytecode Generation**: Utilizes [Maker](https://github.com/cojen/Maker) for dynamic bytecode generation and [Fernflower](https://github.com/windup/windup/tree/master/impl/thirdparty/fernflower) for decompiling to Java source code.

## Technical Overview
OOZX models Z80 instructions as objects, enabling detailed analysis and transformation via the `InstructionVisitor` interface. The emulator tracks data flow at a granular level, supporting tasks such as register substitution, memory scope verification, and precise translation of Z80 logic to Java.

### Symbolic Execution
To ensure that all execution paths are explored, OOZX employs symbolic execution to navigate code branches and identify potential edge cases without manual intervention. This approach reduces reliance on gameplay-driven discovery.

### Memory Adaptation for SMC
OOZX includes a rudimentary mechanism for detecting SMC, dynamically adapting memory writes to ensure compatibility during translation. Further improvements are planned for handling more intricate SMC patterns.

## Usage

### Prerequisites
- **Java 18 or later**

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/fpetrola/oozx.git
   ```
2. Build the project:
   ```bash
   mvn clean install
   ```

### Translating Games
Run the bytecode generator with the following syntax:
```bash
java -jar bytecodeGenerator/target/bytecodeGenerator-0.0.1-SNAPSHOT.jar [execute|translate] [game-name] [url] [main-routine-address]
```
- **execute**: Directly generates and runs bytecode.
- **translate**: Produces `game-name.java` source code instead of bytecode.

#### Examples
```bash
java -jar bytecodeGenerator/target/bytecodeGenerator-0.0.1-SNAPSHOT.jar execute jetsetwilly http://torinak.com/qaop/bin/jetsetwilly 34762
java -jar bytecodeGenerator/target/bytecodeGenerator-0.0.1-SNAPSHOT.jar execute manicminer http://torinak.com/qaop/bin/manicminer 33792
```

### Translation Status
- **Jet Set Willy**: Fully translated (100%)
- **Manic Miner**: Fully translated (100%)
- **Dynamite Dan**: Partially translated (~95%)
- **Everyone's a Wally**: Partially translated (~90%)

## Development Opportunities
The OOZX project provides a fertile ground for enhancements. Potential contributions include:
- Improving variable translation from fields to localized representations.
- Advanced detection and interpretation of game-specific data, such as sprites, sounds, and metadata (e.g., character positions, scores, timers).
- Class and object inference to extract higher-level abstractions from the original game logic.

This project offers a platform for developers to explore intersections between retrocomputing and modern software engineering. Contributions are welcome via the repository's pull request system.

