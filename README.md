# OOZX: Object-Oriented Z80 Emulator for ZX Spectrum to Java Translation

OOZX is an open-source project that provides a Z80 emulator with a unique object-oriented approach. This emulator can automatically translate ZX Spectrum games into pure Java code, leveraging the power and flexibility of Object-Oriented Programming (OOP).

![Gameplay de ZX Spectrum en oozx](doc/jsw1.gif)

## Key Features

- **Automatic Translation**: Converts ZX Spectrum games into Java code, providing a way to enjoy these classic games within a modern, portable environment.
- **Object-Oriented Design**: Uses OOP principles to organize and manage code, making the emulator modular and easy to maintain.
- **Advanced Z80 Instruction Handling**:
    - **Instruction Visitor Pattern**: Allows instructions to be processed at different levels, simplifying emulation and translation.
    - **Instruction Factory**: Provides a centralized way to create instructions, supporting consistency and extensibility.
    - **Prototype Pattern**: Enables efficient management and reuse of instructions.
    - **Virtual Registers**: Abstracts Z80 registers, allowing for a cleaner and more adaptable implementation.
    - **Generic Data Types for Instructions**: Facilitates flexible handling of different instruction types.
    - **Instruction Hierarchy**: Organizes instructions in a hierarchical manner for better code structure and readability.
    - **Z80 Opcode Decoding**: Leverages Cristian Dinu’s decoding approach, based on his excellent documentation at [z80.info](http://www.z80.info/decoding.htm).

## How It Works

The core of **oozx** is built around an object-oriented Z80 emulator, where each instruction is individually modeled, categorized, and accessible via a visitor pattern. Registers are also modeled and designed to handle a variety of data types beyond basic integers or bytes; for example, they can store complex data structures, enabling dataflow analysis features such as value tainting. This design allows extensive use of the `InstructionVisitor` to perform tasks such as cloning instructions, replacing registers with advanced `VirtualRegister` objects, verifying data scopes, and translating instructions into bytecode equivalents.

To ensure coverage of all code paths without needing to manually play through every game level, oozx employs a symbolic execution algorithm. This algorithm leverages the emulator's instruction model, register structure, and control flow capabilities to traverse all potential execution paths in the code.

A simple algorithm is used to detect self-modifying code (SMC). It works by monitoring instructions that write to memory regions not designated for static data. When only an instruction argument is modified (mutant data), it’s replaced by memory access to fetch this data as needed. While this algorithm is basic and works for certain cases, future improvements will aim to handle more complex SMC strategies.

### Screen Visualization

For visualizing the ZX Spectrum screen memory, oozx includes a simple screen component built with Java Swing. Using `Graphics2D`, this component provides a straightforward yet effective view of the screen memory, helping developers visualize the game's display.

### Bytecode Generation

For generating bytecode, oozx uses the [Maker](https://github.com/cojen/Maker) library, which simplifies the creation of variables, methods, fields, and bytecode on the fly, or for saving as `.class` files. To generate Java source code, oozx leverages the [Fernflower decompiler](https://github.com/windup/windup/tree/master/impl/thirdparty/fernflower), which supports converting bytecode back into readable Java code.

---

### Get Involved

If you’re interested in contributing to oozx, there are many exciting areas for development. Some potential enhancements include:
- Improving the conversion of fields to variables
- Detecting and interpreting sprites, sound data, character coordinates, lives, time variables, and more
- Exploring advanced features, such as inferring classes, objects, and methods from the original game code

I hope this overview gives you a clear picture of how oozx works! If you're excited about joining the development, feel free to reach out—there’s a lot to explore to make more games fully compatible and improve the quality of translation.


## Getting Started

### Prerequisites
- **Java 21 or higher** (required for running the translated Java code)

### Installation
1. Clone this repository:
   ```bash
   git clone https://github.com/fpetrola/oozx.git

### Games Translation

#### Usage

   ```bash
   java -jar translator/target/translator-0.0.1-SNAPSHOT.jar [execute/translate] [game-name] [url] [main-routine-address]
   ```
  Using "translate" will be creating "game-name.java" source code instead of creating bytecode on the fly.
#### Examples

   ```bash
   mvn clean install
   java -jar translator/target/translator-0.0.1-SNAPSHOT.jar execute jetsetwilly http://torinak.com/qaop/bin/jetsetwilly 34762
   java -jar translator/target/translator-0.0.1-SNAPSHOT.jar execute manicminer http://torinak.com/qaop/bin/manicminer 33792
   ```
#### Translation status
* Jet Set Willy 100%
* Manic Miner 80%