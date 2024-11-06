/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.instructions.base;

import org.cojen.maker.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MethodMakerForTest implements MethodMaker {
  public ClassMakerForTest classMakerForTest;
  public Object type;
  public String name;
  public Object[] parameters;
  private List<VariableForTest> variables = new ArrayList<>();
  private List<LabelForTest> labels= new ArrayList<>();

  public MethodMakerForTest(ClassMakerForTest classMakerForTest, Object type, String name, Object... parameters) {
    this.classMakerForTest = classMakerForTest;
    this.type = type;
    this.name = name;
    this.parameters = parameters;
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public MethodMaker public_() {
    return this;
  }

  @Override
  public MethodMaker private_() {
    return null;
  }

  @Override
  public MethodMaker protected_() {
    return null;
  }

  @Override
  public MethodMaker static_() {
    return null;
  }

  @Override
  public MethodMaker final_() {
    return null;
  }

  @Override
  public MethodMaker synchronized_() {
    return null;
  }

  @Override
  public MethodMaker abstract_() {
    return null;
  }

  @Override
  public MethodMaker native_() {
    return null;
  }

  @Override
  public MethodMaker synthetic() {
    return null;
  }

  @Override
  public MethodMaker bridge() {
    return null;
  }

  @Override
  public MethodMaker varargs() {
    return null;
  }

  @Override
  public MethodMaker throws_(Object o) {
    return null;
  }

  @Override
  public MethodMaker override() {
    return null;
  }

  @Override
  public MethodMaker signature(Object... objects) {
    return null;
  }

  @Override
  public Variable class_() {
    return null;
  }

  @Override
  public Variable this_() {
    return null;
  }

  @Override
  public Variable super_() {
    return null;
  }

  @Override
  public Variable param(int i) {
    return null;
  }

  @Override
  public int paramCount() {
    return 0;
  }

  @Override
  public Variable var(Object o) {
    VariableForTest variableForTest = new VariableForTest(o);
    variables.add(variableForTest);
    return variableForTest;
  }

  @Override
  public void lineNum(int i) {

  }

  @Override
  public Label label() {
    LabelForTest labelForTest = new LabelForTest();
    labels.add(labelForTest);
    return labelForTest;
  }

  @Override
  public void goto_(Label label) {

  }

  @Override
  public void return_() {

  }

  @Override
  public void return_(Object o) {

  }

  @Override
  public Field field(String s) {
    return classMakerForTest.fieldMakers.stream().filter(f -> f.name.equals(s)).findFirst().get().field;
  }

  @Override
  public Variable invoke(String s, Object... objects) {
    return null;
  }

  @Override
  public void invokeSuperConstructor(Object... objects) {

  }

  @Override
  public void invokeThisConstructor(Object... objects) {

  }

  @Override
  public Variable invoke(MethodHandle methodHandle, Object... objects) {
    return null;
  }

  @Override
  public Variable new_(Object o, Object... objects) {
    return null;
  }

  @Override
  public Variable catch_(Label label, Label label1, Object o) {
    return null;
  }

  @Override
  public Variable catch_(Label label, Label label1, Object... objects) {
    return null;
  }

  @Override
  public void catch_(Label label, Object o, Consumer<Variable> consumer) {

  }

  @Override
  public void finally_(Label label, Runnable runnable) {

  }

  @Override
  public Variable concat(Object... objects) {
    return null;
  }

  @Override
  public Field access(VarHandle varHandle, Object... objects) {
    return null;
  }

  @Override
  public void nop() {

  }

  @Override
  public ClassMaker addInnerClass(String s) {
    return null;
  }

  @Override
  public MethodHandle finish() {
    return null;
  }

  @Override
  public ClassMaker classMaker() {
    return null;
  }

  @Override
  public AnnotationMaker addAnnotation(Object o, boolean b) {
    return null;
  }

  @Override
  public void addAttribute(String s, Object o) {

  }
}
