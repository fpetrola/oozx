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

public class FieldForTest implements Field {
  @Override
  public Class<?> classType() {
    return null;
  }

  @Override
  public ClassMaker makerType() {
    return null;
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public AnnotationMaker addAnnotation(Object o, boolean b) {
    return null;
  }

  @Override
  public Variable clear() {
    return null;
  }

  @Override
  public Field set(Object o) {
    return null;
  }

  @Override
  public Variable setExact(Object o) {
    return null;
  }

  @Override
  public Variable get() {
    return null;
  }

  @Override
  public void ifTrue(Label label) {

  }

  @Override
  public void ifFalse(Label label) {

  }

  @Override
  public void ifEq(Object o, Label label) {

  }

  @Override
  public void ifNe(Object o, Label label) {

  }

  @Override
  public void ifLt(Object o, Label label) {

  }

  @Override
  public void ifGe(Object o, Label label) {

  }

  @Override
  public void ifGt(Object o, Label label) {

  }

  @Override
  public void ifLe(Object o, Label label) {

  }

  @Override
  public void switch_(Label label, int[] ints, Label... labels) {

  }

  @Override
  public void switch_(Label label, String[] strings, Label... labels) {

  }

  @Override
  public void switch_(Label label, Enum<?>[] enums, Label... labels) {

  }

  @Override
  public void switch_(Label label, Object[] objects, Label... labels) {

  }

  @Override
  public void inc(Object o) {

  }

  @Override
  public Variable add(Object o) {
    return null;
  }

  @Override
  public Variable sub(Object o) {
    return null;
  }

  @Override
  public Variable mul(Object o) {
    return null;
  }

  @Override
  public Variable div(Object o) {
    return null;
  }

  @Override
  public Variable rem(Object o) {
    return null;
  }

  @Override
  public Variable eq(Object o) {
    return null;
  }

  @Override
  public Variable ne(Object o) {
    return null;
  }

  @Override
  public Variable lt(Object o) {
    return null;
  }

  @Override
  public Variable ge(Object o) {
    return null;
  }

  @Override
  public Variable gt(Object o) {
    return null;
  }

  @Override
  public Variable le(Object o) {
    return null;
  }

  @Override
  public Variable instanceOf(Object o) {
    return null;
  }

  @Override
  public Variable cast(Object o) {
    return null;
  }

  @Override
  public Variable not() {
    return null;
  }

  @Override
  public Variable and(Object o) {
    return null;
  }

  @Override
  public Variable or(Object o) {
    return this;
  }

  @Override
  public Variable xor(Object o) {
    return null;
  }

  @Override
  public Variable shl(Object o) {
    return this;
  }

  @Override
  public Variable shr(Object o) {
    return null;
  }

  @Override
  public Variable ushr(Object o) {
    return null;
  }

  @Override
  public Variable neg() {
    return null;
  }

  @Override
  public Variable com() {
    return null;
  }

  @Override
  public Variable box() {
    return null;
  }

  @Override
  public Variable unbox() {
    return null;
  }

  @Override
  public Variable alength() {
    return null;
  }

  @Override
  public Variable aget(Object o) {
    Variable a= (Variable) o;
    return new VariableForTest(a.classType());
  }

  @Override
  public void aset(Object o, Object o1) {

  }

  @Override
  public Field field(String s) {
    return null;
  }

  @Override
  public Variable invoke(String s, Object... objects) {
    return null;
  }

  @Override
  public Variable invoke(Object o, String s, Object[] objects, Object... objects1) {
    return new VariableForTest(objects1);
  }

  @Override
  public Variable methodHandle(Object o, String s, Object... objects) {
    return null;
  }

  @Override
  public Bootstrap indy(String s, Object... objects) {
    return null;
  }

  @Override
  public Bootstrap condy(String s, Object... objects) {
    return null;
  }

  @Override
  public void throw_() {

  }

  @Override
  public void monitorEnter() {

  }

  @Override
  public void monitorExit() {

  }

  @Override
  public void synchronized_(Runnable runnable) {

  }

  @Override
  public MethodMaker methodMaker() {
    return null;
  }

  @Override
  public Variable getPlain() {
    return null;
  }

  @Override
  public void setPlain(Object o) {

  }

  @Override
  public Variable getOpaque() {
    return null;
  }

  @Override
  public void setOpaque(Object o) {

  }

  @Override
  public Variable getAcquire() {
    return null;
  }

  @Override
  public void setRelease(Object o) {

  }

  @Override
  public Variable getVolatile() {
    return null;
  }

  @Override
  public void setVolatile(Object o) {

  }

  @Override
  public Variable compareAndSet(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable compareAndExchange(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable compareAndExchangeAcquire(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable compareAndExchangeRelease(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable weakCompareAndSetPlain(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable weakCompareAndSet(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable weakCompareAndSetAcquire(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable weakCompareAndSetRelease(Object o, Object o1) {
    return null;
  }

  @Override
  public Variable getAndSet(Object o) {
    return null;
  }

  @Override
  public Variable getAndSetAcquire(Object o) {
    return null;
  }

  @Override
  public Variable getAndSetRelease(Object o) {
    return null;
  }

  @Override
  public Variable getAndAdd(Object o) {
    return null;
  }

  @Override
  public Variable getAndAddAcquire(Object o) {
    return null;
  }

  @Override
  public Variable getAndAddRelease(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseOr(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseOrAcquire(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseOrRelease(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseAnd(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseAndAcquire(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseAndRelease(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseXor(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseXorAcquire(Object o) {
    return null;
  }

  @Override
  public Variable getAndBitwiseXorRelease(Object o) {
    return null;
  }

  @Override
  public Variable varHandle() {
    return null;
  }

  @Override
  public Variable methodHandleSet() {
    return null;
  }

  @Override
  public Variable methodHandleGet() {
    return null;
  }
}
