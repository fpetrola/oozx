/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.plugins.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Writes what a jar brings while the jar is compiled: every class that implements a way in is
 * listed in the META-INF/services of that way in.
 * <p>
 * Those files used to be written by hand, one line each, and a misspelt name was a board that
 * simply did not appear. What a class implements is already in the class; this reads it.
 * <p>
 * The annotation is matched by name rather than by type so that this has no dependencies: it is
 * on javac's path for every module, and whatever it depended on would be there too.
 */
@SupportedAnnotationTypes("*")
public class WhatAJarBrings extends AbstractProcessor {

  private static final String THE_MARK_OF_A_WAY_IN = "com.fpetrola.oozx.plugins.Plugin";

  /** Which classes answer to each way in, gathered as the rounds go and written at the end. */
  private final Map<String, Set<String>> bringing = new TreeMap<>();

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    // Everything being compiled, including what is written inside something else: a way in can
    // be answered by a nested class, and several of them are.
    for (Element root : round.getRootElements()) {
      look(root);
    }
    if (round.processingOver()) {
      bringing.forEach(this::write);
    }
    return false;
  }

  /** This and whatever is written inside it, each asked whether it answers to a way in. */
  private void look(Element element) {
    if (element instanceof TypeElement type) {
      if (canBeInstantiated(type)) {
        for (String wayIn : waysInOf(type)) {
          // By name, so what a jar says it brings does not depend on the order javac read it in.
          bringing.computeIfAbsent(wayIn, one -> new TreeSet<>())
              .add(processingEnv.getElementUtils().getBinaryName(type).toString());
        }
      }
      type.getEnclosedElements().forEach(this::look);
    }
  }

  /**
   * Whether whoever finds this could make one.
   * <p>
   * What lists a class nobody can build is worse than not listing it: a service file is read by
   * being loaded, and one line that cannot be built makes the whole look throw. The built-in
   * effects are exactly this case - a scanline is made from the knob that says how deep it is -
   * and they belong to the build rather than arriving, so they are left out and said out loud.
   */
  private boolean canBeInstantiated(TypeElement type) {
    if (type.getKind() != ElementKind.CLASS || type.getModifiers().contains(Modifier.ABSTRACT)) {
      return false;
    }
    if (!type.getModifiers().contains(Modifier.PUBLIC)
        || (type.getNestingKind().isNested() && !type.getModifiers().contains(Modifier.STATIC))) {
      return sayNothingCanBuildIt(type, "it is not public, or it is nested inside an instance");
    }
    for (Element member : type.getEnclosedElements()) {
      if (member.getKind() == ElementKind.CONSTRUCTOR
          && member.getModifiers().contains(Modifier.PUBLIC)
          && ((ExecutableElement) member).getParameters().isEmpty()) {
        return true;
      }
    }
    return sayNothingCanBuildIt(type, "it has no public constructor that takes nothing");
  }

  private boolean sayNothingCanBuildIt(TypeElement type, String why) {
    if (!waysInOf(type).isEmpty()) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
          type.getSimpleName() + " answers to a way in but nothing can find it: " + why, type);
    }
    return false;
  }

  /** Every way in this answers to, however far up it was declared. */
  private Set<String> waysInOf(TypeElement type) {
    Set<String> waysIn = new LinkedHashSet<>();
    collect(type.asType(), waysIn, new LinkedHashSet<>());
    return waysIn;
  }

  private void collect(TypeMirror what, Set<String> waysIn, Set<String> seen) {
    Element element = processingEnv.getTypeUtils().asElement(what);
    if (!(element instanceof TypeElement type) || !seen.add(type.getQualifiedName().toString())) {
      return;
    }
    if (type.getKind() == ElementKind.INTERFACE && isAWayIn(type)) {
      waysIn.add(type.getQualifiedName().toString());
    }
    type.getInterfaces().forEach(above -> collect(above, waysIn, seen));
    collect(type.getSuperclass(), waysIn, seen);
  }

  private static boolean isAWayIn(TypeElement type) {
    for (AnnotationMirror mark : type.getAnnotationMirrors()) {
      if (mark.getAnnotationType().toString().equals(THE_MARK_OF_A_WAY_IN)) {
        return true;
      }
    }
    return false;
  }

  private void write(String wayIn, Set<String> answering) {
    try {
      FileObject file = processingEnv.getFiler()
          .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + wayIn);
      try (PrintWriter out = new PrintWriter(file.openWriter())) {
        answering.forEach(out::println);
      }
    } catch (IOException cannotBeWritten) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
          "what this brings for " + wayIn + " could not be written: " + cannotBeWritten.getMessage());
    }
  }
}
