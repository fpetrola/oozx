package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;

import java.nio.file.Path;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Demo que muestra cómo usar BytecodeInliner para generar
 * clases compiladas dinámicamente en bytecode con cojen/maker.
 */
public class BytecodeInlinerDemo {

  public static void main(String[] args) throws Exception {
    System.out.println("=== BytecodeInliner Demo ===\n");

    // 1. Crear instrucción Ld
    var instruction = createLdInstruction();
    System.out.println("Instrucción creada: " + instruction.getClass().getSimpleName());

    // 2. Analizar la instrucción
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(instruction);
    System.out.println("Instrucción analizada");
    System.out.println("Variables requeridas: " + analyzer.getRequiredVariables().keySet());

    // 3. Generar bytecode
    Path path = Path.of("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/src/main/java");
    var inliner = new BytecodeInliner(analyzer, path);
    Class<?> generatedClass = inliner.inlineLd(instruction);

    System.out.println("\nClase generada dinámicamente:");
    System.out.println("  Nombre: " + generatedClass.getName());
    System.out.println("  Superclase: " + generatedClass.getSuperclass().getSimpleName());

    // 4. Inspeccionar la clase generada
    System.out.println("\nCampos:");
    for (var field : generatedClass.getDeclaredFields()) {
      System.out.println("  - " + field.getType().getSimpleName() + " " + field.getName());
    }

    System.out.println("\nConstructores:");
    for (var ctor : generatedClass.getDeclaredConstructors()) {
      System.out.print("  - " + ctor.getName() + "(");
      var params = ctor.getParameterTypes();
      for (int i = 0; i < params.length; i++) {
        System.out.print(params[i].getSimpleName());
        if (i < params.length - 1) System.out.print(", ");
      }
      System.out.println(")");
    }

    System.out.println("\nMétodos:");
    for (var method : generatedClass.getDeclaredMethods()) {
      System.out.println("  - " + method.getReturnType().getSimpleName() + " " + method.getName() + "()");
    }

    System.out.println("\n✓ Generación de bytecode exitosa");
  }

  /**
   * Crea una instrucción Ld de prueba que lee de memoria indirecta
   */
  private static Ld createLdInstruction() {
    MyAbstractMemory memory = new MyAbstractMemory();
    var target = new IndirectMemory8BitReference(
        new Memory16BitReference(memory, new Plain16BitRegister("IY"), 3),
        memory
    );
    var source = new Plain8BitRegister("B");
    return new Ld(target, source, new Plain8BitRegister("F"));
  }
}
