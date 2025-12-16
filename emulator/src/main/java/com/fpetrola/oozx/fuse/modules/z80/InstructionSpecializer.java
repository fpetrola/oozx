package com.fpetrola.oozx.fuse.modules.z80;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.registers.Plain16BitRegister;
import com.fpetrola.z80.registers.Plain8BitRegister;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

public class InstructionSpecializer {

  public static Ld specialize(Ld ld) {
    try {
      // Análisis simple: extraer las instancias concretas relevantes
      var target = ld.getTarget();   // MemoryPlusRegister8BitReference
      var source = ld.getSource();   // Plain8BitRegister "A"
      var flags = ld.getFlag();    // Plain8BitRegister "F"

      // Aquí podrías recorrer recursivamente con reflexión o un Visitor
      // para encontrar todos los objetos hoja (memoria, registros concretos)

      ClassMaker cm = ClassMaker.begin("SpecializedLd_" + System.nanoTime())
          .public_().extend(Ld.class);

      cm.addField(target.getClass(), "t").private_();
      cm.addField(source.getClass(), "s").private_();
      // Constructor vacío (necesario)
      MethodMaker methodMaker = cm.addConstructor();
      methodMaker.public_();

      Variable t = methodMaker.field("t").setExact(target);
      Variable s = methodMaker.field("s").setExact(source);

      Variable variable = methodMaker.var(target.getClass()).setExact(target);
      Variable variable1 = methodMaker.var(source.getClass()).setExact(source);
      Variable variable2 = methodMaker.var(flags.getClass()).setExact(flags);
      methodMaker.invokeSuperConstructor(variable, variable1, variable2);
      methodMaker.return_();


      cm.addMethod(int.class, "getLength").public_().return_(ld.getLength());

      MethodMaker methodMaker1 = cm.addMethod(void.class, "execute").public_();
      Variable variable3 = methodMaker1.field("t");
      Variable variable4 = methodMaker1.field("s");

      variable3.invoke("write", variable4.invoke("read"));

      // flags.update(value)  (asumiendo que Ld actualiza flags)
//      flagsVar.invoke("write", value);

      // Si hay más lógica en execute(), replicarla aquí manualmente o via un "template"

      // Finalizar clase (hidden para que se pueda unload)

//      byte[] bytes = cm.finishBytes();
//      // Opcional: guardar el .class para inspección
//      File outputDir = new File("generated_classes");
//      outputDir.mkdirs();
//      File outputFile = new File(outputDir, "SpecializedLd_" + System.nanoTime() + ".class");
//      FileUtils.writeByteArrayToFile(outputFile, bytes);
      var lookup = cm.finishHidden();
      Class<?> clazz = lookup.lookupClass();

      Ld ld1 = (Ld) clazz.getConstructors()[0].newInstance();
      ld1.setPhaseInterceptor(ld.getCachedPhase());
      return ld1;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Ld getLd1() {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new Memory() {
      public int read(int address, int fetching) {
        return 0;
      }

      public void write(int address, int value) {
      }

      public void reset() {
      }
    }, new Plain16BitRegister("PC"), 2
    );
    var source = new Plain8BitRegister("A");
    return new Ld(target, source, new Plain8BitRegister("F"));
  }

  public static void main(String[] args) {
    Ld genericLd = getLd1();  // tu método actual

    Instruction fastLd = InstructionSpecializer.specialize(genericLd);

// Luego en el fetch-execute loop:
    fastLd.execute();  // ahora todo inlineado, sin polymorphism
  }
}
