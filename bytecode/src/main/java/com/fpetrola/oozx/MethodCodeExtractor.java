package com.fpetrola.oozx;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Extrae el código de métodos específicos usando JavaParser para hacer inlining recursivo.
 */
public class MethodCodeExtractor {
  private final Path sourcePath;

  public MethodCodeExtractor(Path sourcePath) {
    this.sourcePath = sourcePath;
  }

  /**
   * Extrae el cuerpo de un método de una clase especificada.
   * 
   * @param className nombre completo de la clase (ej: com.fpetrola.z80.instructions.impl.Ld)
   * @param methodName nombre del método (ej: execute)
   * @return el código del método o null si no se encuentra
   */
  public String extractMethodBody(String className, String methodName) {
    try {
      Path filePath = classNameToPath(className);
      
      if (!Files.exists(filePath)) {
        return null;
      }

      String content = Files.readString(filePath);
      CompilationUnit cu = new JavaParser().parse(content).getResult().orElse(null);
      
      if (cu == null) {
        return null;
      }

      Optional<MethodDeclaration> method = cu.findAll(MethodDeclaration.class).stream()
          .filter(m -> m.getNameAsString().equals(methodName))
          .findFirst();

      if (method.isEmpty()) {
        return null;
      }

      Optional<BlockStmt> body = method.get().getBody();
      if (body.isEmpty()) {
        return null;
      }

      String methodCode = body.get().toString();
      if (methodCode.startsWith("{") && methodCode.endsWith("}")) {
        methodCode = methodCode.substring(1, methodCode.length() - 1);
      }

      return methodCode.trim();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Extrae el código de la operación anónima (ej: xorTableAluOperation) dentro de una clase.
   * Busca la creación de objeto anónimo que contiene el método execute y filtra líneas irrelevantes.
   * 
   * @param className nombre de la clase que contiene la operación anónima
   * @param operationFieldName nombre del campo que contiene la operación (ej: xorTableAluOperation)
   * @param methodName nombre del método dentro de la clase anónima (ej: execute)
   * @param excludeVars variables a excluir de las asignaciones (ej: F, Q)
   * @return el código filtrado del método de la operación o null si no se encuentra
   */
  public String extractAnonymousOperationMethodBody(String className, String operationFieldName, String methodName, String... excludeVars) {
    try {
      Path filePath = classNameToPath(className);
      
      if (!Files.exists(filePath)) {
        return null;
      }

      String content = Files.readString(filePath);
      CompilationUnit cu = new JavaParser().parse(content).getResult().orElse(null);
      
      if (cu == null) {
        return null;
      }

      // Buscar todas las creaciones de objetos anónimos que contengan el método
      for (ObjectCreationExpr objCreation : cu.findAll(ObjectCreationExpr.class)) {
        // Verificar si esta es la creación anónima que queremos
        var anonClassBody = objCreation.getAnonymousClassBody();
        if (anonClassBody.isEmpty()) {
          continue;
        }

        // Buscar el método dentro de la clase anónima
        var methods = objCreation.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methods) {
          if (method.getNameAsString().equals(methodName)) {
            Optional<BlockStmt> methodBody = method.getBody();
            if (methodBody.isPresent()) {
              String methodCode = methodBody.get().toString();
              if (methodCode.startsWith("{") && methodCode.endsWith("}")) {
                methodCode = methodCode.substring(1, methodCode.length() - 1);
              }
              
              // Filtrar el código
              return filterOperationCode(methodCode.trim(), excludeVars);
            }
          }
        }
      }

      return null;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Filtra el código de la operación removiendo líneas irrelevantes y simplificando expresiones.
   */
  private String filterOperationCode(String code, String... excludeVars) {
    String[] lines = code.split("\n");
    StringBuilder result = new StringBuilder();
    
    for (String line : lines) {
      String trimmed = line.trim();
      
      // Remover líneas return
      if (trimmed.startsWith("return ")) {
        continue;
      }
      
      // Remover asignaciones a variables excluidas
      boolean skip = false;
      for (String excludeVar : excludeVars) {
        if (trimmed.startsWith(excludeVar + " =") || trimmed.startsWith(excludeVar + "=")) {
          skip = true;
          break;
        }
      }
      
      if (skip) {
        continue;
      }
      
      // Simplificar expresiones: A ^= (value) -> A ^= value
      line = line.replaceAll("\\(value\\)", "value");
      
      result.append(line).append("\n");
    }
    
    String filtered = result.toString().trim();
    return filtered.isEmpty() ? null : filtered;
  }

  /**
   * Convierte un nombre de clase (formato paquete.Clase) a una ruta de archivo.
   */
  private Path classNameToPath(String className) {
    String relativePath = className.replace(".", "/") + ".java";
    return sourcePath.resolve(relativePath);
  }
}
