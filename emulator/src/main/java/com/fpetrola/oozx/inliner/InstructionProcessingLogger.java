package com.fpetrola.oozx.inliner;

/**
 * Logger centralizado para el procesamiento de instrucciones.
 * Proporciona mensajes de error consistentes y trazabilidad.
 */
public class InstructionProcessingLogger {
  private static final boolean VERBOSE = false;

  /**
   * Registra un error al procesar una TargetSourceInstruction
   */
  public static void logTargetSourceInstructionError(String instructionName, 
                                                    String exceptionClass, String message) {
    System.err.println("ERROR: Exception in processTargetSourceInstruction for " + 
                      instructionName + ": " + exceptionClass + ": " + message);
  }

  /**
   * Registra un error al procesar una instrucción unaria
   */
  public static void logUnaryInstructionError(String instructionName, 
                                              String exceptionClass, String message) {
    System.err.println("ERROR: Exception in processUnaryInstruction for " + 
                      instructionName + ": " + exceptionClass + ": " + message);
  }

  /**
   * Registra un error al procesar una instrucción del registry
   */
  public static void logRegistryInstructionError(String instructionName, String message) {
    System.err.println("WARNING: No se pudo procesar instrucción del registry " + 
                      instructionName + ": " + message);
  }

  /**
   * Registra un ClassFormatError con mensaje
   */
  public static void logClassFormatError(String instructionName, String message) {
    System.err.println("WARNING: ClassFormatError al procesar " + instructionName + 
                      ": " + message);
  }

  /**
   * Registra un error genérico de procesamiento
   */
  public static void logProcessingError(String instructionName, Throwable e) {
    if (VERBOSE) {
      System.err.println("DEBUG: Error procesando " + instructionName);
      e.printStackTrace();
    }
  }

  /**
   * Registra información de depuración si está habilitada
   */
  public static void logDebug(String message) {
    if (VERBOSE) {
      System.out.println("DEBUG: " + message);
    }
  }
}
