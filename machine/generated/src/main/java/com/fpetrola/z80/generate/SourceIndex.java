/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.fpetrola.z80.generate;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/** Every type under a source root, by the name the JVM gives it, so a runtime object can be taken back to its source. */
public class SourceIndex {
  private final Map<String, TypeDeclaration<?>> types = new HashMap<>();
  private final Map<TypeDeclaration<?>, CompilationUnit> units = new IdentityHashMap<>();

  /** More than one root when the core is generated against a machine: the model and that machine. */
  public SourceIndex(Path... roots) {
    StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    for (Path root : roots)
      try (Stream<Path> files = Files.walk(root)) {
        files.filter(f -> f.toString().endsWith(".java")).forEach(this::parse);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
  }

  private void parse(Path file) {
    try {
      CompilationUnit unit = StaticJavaParser.parse(file);
      String pkg = unit.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("");
      unit.getTypes().forEach(t -> index(pkg + t.getNameAsString(), t, unit));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void index(String name, TypeDeclaration<?> type, CompilationUnit unit) {
    types.put(name, type);
    units.put(type, unit);
    type.getMembers().forEach(m -> {
      if (m instanceof TypeDeclaration<?> nested)
        index(name + "$" + nested.getNameAsString(), nested, unit);
    });
  }

  public TypeDeclaration<?> type(Class<?> c) {
    return types.get(c.getName());
  }

  public boolean has(Class<?> c) {
    return types.containsKey(c.getName());
  }

  public Optional<MethodDeclaration> method(TypeDeclaration<?> type, String name, int arity) {
    return type.getMethods().stream().filter(m -> m.getNameAsString().equals(name) && m.getParameters().size() == arity).findFirst();
  }

  /** The class a simple name means inside a type: its imports, its package, or java.lang; empty when it is not ours. */
  public Optional<Class<?>> resolve(TypeDeclaration<?> from, String simpleName) {
    CompilationUnit unit = units.get(from);
    List<String> candidates = new ArrayList<>();
    for (ImportDeclaration i : unit.getImports())
      if (!i.isAsterisk() && !i.isStatic() && i.getName().getIdentifier().equals(simpleName))
        candidates.add(i.getNameAsString());
    for (ImportDeclaration i : unit.getImports())
      if (i.isAsterisk() && !i.isStatic())
        candidates.add(i.getNameAsString() + "." + simpleName);
    unit.getPackageDeclaration().ifPresent(p -> candidates.add(p.getNameAsString() + "." + simpleName));
    for (String candidate : candidates) {
      String binary = types.containsKey(candidate) ? candidate : candidate.replaceAll("\\.(?=[^.]*$)", "\\$");
      if (types.containsKey(binary)) {
        try {
          return Optional.of(Class.forName(binary));
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return Optional.empty();
  }

  public CompilationUnit unitOf(TypeDeclaration<?> type) {
    return units.get(type);
  }
}
