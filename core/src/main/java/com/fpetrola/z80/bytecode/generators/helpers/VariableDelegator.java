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

package com.fpetrola.z80.bytecode.generators.helpers;

import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.*;

import static com.fpetrola.z80.bytecode.generators.RoutineBytecodeGenerator.getRealVariable;

public interface VariableDelegator extends Variable {
  Variable getDelegate();

  @Override
  default Variable set(Object value) {
    return getDelegate().set(value);
  }

  @Override
  default Class<?> classType() {
    return getDelegate().classType();
  }

  @Override
  default ClassMaker makerType() {
    return getDelegate().makerType();
  }

  @Override
  default String name() {
    return getDelegate().name();
  }

  @Override
  default Variable name(String name) {
    return getDelegate().name(name);
  }

  @Override
  default Variable signature(Object... components) {
    return getDelegate().signature(components);
  }

  @Override
  default AnnotationMaker addAnnotation(Object annotationType, boolean visible) {
    return getDelegate().addAnnotation(annotationType, visible);
  }

  @Override
  default Variable clear() {
    return getDelegate().clear();
  }

  @Override
  default Variable setExact(Object value) {
    return getDelegate().setExact(value);
  }

  @Override
  default Variable get() {
    return getDelegate();
  }

  @Override
  default void ifTrue(Label label) {
    getDelegate().ifTrue(label);
  }

  @Override
  default void ifTrue(Runnable then) {
    getDelegate().ifTrue(then);
  }

  @Override
  default void ifTrue(Runnable then, Runnable else_) {
    getDelegate().ifTrue(then, else_);
  }

  @Override
  default void ifFalse(Label label) {
    getDelegate().ifFalse(label);
  }

  @Override
  default void ifFalse(Runnable then) {
    getDelegate().ifFalse(then);
  }

  @Override
  default void ifFalse(Runnable then, Runnable else_) {
    getDelegate().ifFalse(then, else_);
  }

  @Override
  default void ifEq(Object value, Label label) {
    getDelegate().ifEq(value, label);
  }

  @Override
  default void ifEq(Object value, Runnable then) {
    getDelegate().ifEq(value, then);
  }

  @Override
  default void ifEq(Object value, Runnable then, Runnable else_) {
    getDelegate().ifEq(value, then, else_);
  }

  @Override
  default void ifNe(Object value, Label label) {
    getDelegate().ifNe(value, label);
  }

  @Override
  default void ifNe(Object value, Runnable then) {
    getDelegate().ifNe(value, then);
  }

  @Override
  default void ifNe(Object value, Runnable then, Runnable else_) {
    getDelegate().ifNe(value, then, else_);
  }

  @Override
  default void ifLt(Object value, Label label) {
    getDelegate().ifLt(value, label);
  }

  @Override
  default void ifLt(Object value, Runnable then) {
    getDelegate().ifLt(value, then);
  }

  @Override
  default void ifLt(Object value, Runnable then, Runnable else_) {
    getDelegate().ifLt(value, then, else_);
  }

  @Override
  default void ifGe(Object value, Label label) {
    getDelegate().ifGe(value, label);
  }

  @Override
  default void ifGe(Object value, Runnable then) {
    getDelegate().ifGe(value, then);
  }

  @Override
  default void ifGe(Object value, Runnable then, Runnable else_) {
    getDelegate().ifGe(value, then, else_);
  }

  @Override
  default void ifGt(Object value, Label label) {
    getDelegate().ifGt(value, label);
  }

  @Override
  default void ifGt(Object value, Runnable then) {
    getDelegate().ifGt(value, then);
  }

  @Override
  default void ifGt(Object value, Runnable then, Runnable else_) {
    getDelegate().ifGt(value, then, else_);
  }

  @Override
  default void ifLe(Object value, Label label) {
    getDelegate().ifLe(value, label);
  }

  @Override
  default void ifLe(Object value, Runnable then) {
    getDelegate().ifLe(value, then);
  }

  @Override
  default void ifLe(Object value, Runnable then, Runnable else_) {
    getDelegate().ifLe(value, then, else_);
  }

  @Override
  default void switch_(Label defaultLabel, int[] cases, Label... labels) {
    getDelegate().switch_(defaultLabel, cases, labels);
  }

  @Override
  default void switch_(Label defaultLabel, String[] cases, Label... labels) {
    getDelegate().switch_(defaultLabel, cases, labels);
  }

  @Override
  default void switch_(Label defaultLabel, Enum<?>[] cases, Label... labels) {
    getDelegate().switch_(defaultLabel, cases, labels);
  }

  @Override
  default void switch_(Label defaultLabel, Object[] cases, Label... labels) {
    getDelegate().switch_(defaultLabel, cases, labels);
  }

  @Override
  default void inc(Object value) {
    set(getDelegate().add(1));
  }

  @Override
  default Variable add(Object value) {
    return getDelegate().add(getRealVariable(value));
  }

  @Override
  default Variable sub(Object value) {
    return getDelegate().sub(getRealVariable(value));
  }

  @Override
  default Variable mul(Object value) {
    return getDelegate().mul(getRealVariable(value));
  }

  @Override
  default Variable div(Object value) {
    return getDelegate().div(value);
  }

  @Override
  default Variable rem(Object value) {
    return getDelegate().rem(value);
  }

  @Override
  default Variable eq(Object value) {
    return getDelegate().eq(value);
  }

  @Override
  default Variable ne(Object value) {
    return getDelegate().ne(value);
  }

  @Override
  default Variable lt(Object value) {
    return getDelegate().lt(value);
  }

  @Override
  default Variable ge(Object value) {
    return getDelegate().ge(value);
  }

  @Override
  default Variable gt(Object value) {
    return getDelegate().gt(value);
  }

  @Override
  default Variable le(Object value) {
    return getDelegate().le(value);
  }

  @Override
  default Variable instanceOf(Object type) {
    return getDelegate().instanceOf(type);
  }

  @Override
  default Variable cast(Object type) {
    return getDelegate().cast(type);
  }

  @Override
  default Variable not() {
    return getDelegate().not();
  }

  @Override
  default Variable and(Object value) {
    return getDelegate().and(value);
  }

  @Override
  default Variable or(Object value) {
    return getDelegate().or(value);
  }

  @Override
  default Variable xor(Object value) {
    return getDelegate().xor(value);
  }

  @Override
  default Variable shl(Object value) {
    return getDelegate().shl(value);
  }

  @Override
  default Variable shr(Object value) {
    return getDelegate().shr(value);
  }

  @Override
  default Variable ushr(Object value) {
    return getDelegate().ushr(value);
  }

  @Override
  default Variable neg() {
    return getDelegate().neg();
  }

  @Override
  default Variable com() {
    return getDelegate().com();
  }

  @Override
  default Variable box() {
    return getDelegate().box();
  }

  @Override
  default Variable unbox() {
    return getDelegate().unbox();
  }

  @Override
  default Variable alength() {
    return getDelegate().alength();
  }

  @Override
  default Variable aget(Object index) {
    return getDelegate().aget(index);
  }

  @Override
  default void aset(Object index, Object value) {
    getDelegate().aset(index, value);
  }

  @Override
  default Field field(String name) {
    return getDelegate().field(name);
  }

  @Override
  default Variable invoke(String name, Object... values) {
    return getDelegate().invoke(name, values);
  }

  @Override
  default Variable invoke(String name) {
    return getDelegate().invoke(name);
  }

  @Override
  default Variable invoke(Object returnType, String name, Object[] types, Object... values) {
    return getDelegate().invoke(returnType, name, types, values);
  }

  @Override
  default Variable invoke(Object returnType, String name, Object[] types) {
    return getDelegate().invoke(returnType, name, types);
  }

  @Override
  default Variable methodHandle(Object returnType, String name, Object... types) {
    return getDelegate().methodHandle(returnType, name, types);
  }

  @Override
  default Variable methodHandle(Object returnType, String name) {
    return getDelegate().methodHandle(returnType, name);
  }

  @Override
  default Bootstrap indy(String name, Object... args) {
    return getDelegate().indy(name, args);
  }

  @Override
  default Bootstrap indy(String name) {
    return getDelegate().indy(name);
  }

  @Override
  default Bootstrap condy(String name, Object... args) {
    return getDelegate().condy(name, args);
  }

  @Override
  default Bootstrap condy(String name) {
    return getDelegate().condy(name);
  }

  @Override
  default void throw_() {
    getDelegate().throw_();
  }

  @Override
  default void monitorEnter() {
    getDelegate().monitorEnter();
  }

  @Override
  default void monitorExit() {
    getDelegate().monitorExit();
  }

  @Override
  default void synchronized_(Runnable body) {
    getDelegate().synchronized_(body);
  }

  @Override
  default MethodMaker methodMaker() {
    return getDelegate().methodMaker();
  }

  void setRegister(VirtualRegister<?> register);
}
