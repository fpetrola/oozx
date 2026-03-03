package com.fpetrola.z80.bytecode.tests.rzzx;

import com.fpetrola.z80.bytecode.tests.JetSetWilly2FieldAccessAnalyzer3;
import com.fpetrola.z80.bytecode.tests.aa.Z80Registers;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Executes the game with field access analysis enabled.
 * Uses GameRZXInvoker infrastructure but with the analyzer version of JetSetWilly2.
 * 
 * Run this to generate field-dependency-analysis.json containing:
 * - Which fields each method reads
 * - Which fields each method writes
 * - Refactoring recommendations
 */
public class GameRZXInvokerAnalyzer {
  public static void main(String[] args) {
    JetSetWilly2FieldAccessAnalyzer3 analyzer = null;
    try {
      System.out.println("Starting game execution with field access analysis...");
      RZXPlayerIO miniZXIO = new RZXPlayerIO();
      Predicate<Integer> interruptionCondition = miniZXIO.getInterruptionCondition();
      String[] s = {"AF, BC", "DE", "HL", "IX", "IY", "A, B", "D", "H", "IXH", "IYH", "F, C", "E", "L", "IXL", "IYL"};
      
      Constructor<?>[] constructors = new ByteBuddy()
          .subclass(JetSetWilly2FieldAccessAnalyzer3.class)
          .method(ElementMatchers.named("pc")).intercept(MethodDelegation.to(PcInterceptor.class))
          .method(ElementMatchers.nameStartsWith("$")).intercept(MethodDelegation.to(RoutineCallInterceptor.class))
          .method(ElementMatchers.namedOneOf(s)).intercept(MethodDelegation.to(Reg16AccessInterceptor.class))
          .make()
          .load(GameRZXInvokerAnalyzer.class.getClassLoader())
          .getLoaded()
          .getConstructors();

      Constructor<?> constructor = Arrays.stream(constructors)
          .filter(c -> c.getParameterCount() == 2)
          .findFirst()
          .orElseThrow(() -> new RuntimeException("Constructor not found"));

      analyzer = (JetSetWilly2FieldAccessAnalyzer3) constructor.newInstance(miniZXIO, interruptionCondition);

      System.out.println("Executing game...");
      analyzer.$34463();
      
      System.out.println("Game execution complete. Generating analysis report...");
//      analyzer.saveAnalysis("field-dependency-analysis.json");
      
    } catch (Exception e) {
      e.printStackTrace();
      try {
        String s = Z80Registers.generarReporte();
        System.out.println(s);
//        analyzer.saveAnalysis("field-dependency-analysis.json");
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      System.err.println("Error during analysis:");
      e.printStackTrace();
    }
  }
}
