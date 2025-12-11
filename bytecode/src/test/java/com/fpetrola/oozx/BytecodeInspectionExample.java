package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.Memory16BitReference;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;

import java.nio.file.Path;

/**
 * Ejemplo de cómo inspeccionar el bytecode generado por BytecodeInliner
 * 
 * Ejecuta este ejemplo con:
 * mvn exec:java -Dexec.mainClass="com.fpetrola.oozx.BytecodeInspectionExample"
 */
public class BytecodeInspectionExample {

  public static void main(String[] args) throws Exception {
    System.out.println("╔════════════════════════════════════════════════════════╗");
    System.out.println("║  BytecodeInliner - Inspection Example                 ║");
    System.out.println("╚════════════════════════════════════════════════════════╝\n");

    // 1. Crear instrucción
    var instruction = createLdInstruction();
    System.out.println("1️⃣  Instrucción creada: " + instruction.getClass().getSimpleName());

    // 2. Analizar
    var analyzer = new InstructionAnalyzer();
    analyzer.analyze(instruction);
    System.out.println("2️⃣  Variables requeridas: " + analyzer.getRequiredVariables().keySet());

    // 3. Generar bytecode
    Path sourcePath = Path.of("./emulator/src/main/java");
    var inliner = new BytecodeInliner(analyzer, sourcePath);
    Class<?> generatedClass = inliner.inlineLd(instruction);
    System.out.println("3️⃣  Clase generada: " + generatedClass.getName());

    // 4. Inspeccionar información de clase
    System.out.println("\n4️⃣  Información de clase:");
    ClassInspector.showClassInfo(generatedClass);

    // 5. Mostrar comando javap para ver bytecode completo
    System.out.println("5️⃣  Para ver el bytecode completo:");
    System.out.println("   javap -v -p -c -classpath target/test-classes " + generatedClass.getName());
  }

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
