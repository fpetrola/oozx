package com.fpetrola.oozx.inliner;

import java.util.HashSet;
import java.util.Set;

/**
 * Contexto centralizado para el procesamiento de instrucciones.
 * Mantiene estado compartido durante la generación de una clase de bytecode.
 * 
 * Este objeto actúa como contenedor único de estado para evitar pasar
 * parámetros repetidamente a través de múltiples niveles de métodos.
 */
public class InstructionProcessingContext {
  private final Set<String> generatedMethods = new HashSet<>();

  /**
   * Verifica si un método ya fue generado
   */
  public boolean hasGeneratedMethod(String methodName) {
    return generatedMethods.contains(methodName);
  }

  /**
   * Registra un método como generado
   */
  public void markMethodGenerated(String methodName) {
    generatedMethods.add(methodName);
  }

  /**
   * Intenta registrar un método. Retorna true si se agregó (no existía)
   */
  public boolean addGeneratedMethod(String methodName) {
    return generatedMethods.add(methodName);
  }

  /**
   * Remueve un método del conjunto (para permitir reintentos)
   */
  public void removeGeneratedMethod(String methodName) {
    generatedMethods.remove(methodName);
  }

  /**
   * Limpia todos los métodos generados
   */
  public void clear() {
    generatedMethods.clear();
  }

  /**
   * Obtiene el conjunto de métodos generados (vista)
   */
  public Set<String> getGeneratedMethods() {
    return generatedMethods;
  }

  /**
   * Retorna el tamaño del conjunto
   */
  public int size() {
    return generatedMethods.size();
  }
}
