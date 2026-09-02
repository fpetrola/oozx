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

import com.github.javaparser.ast.expr.*;

import java.util.ArrayList;

/** Arithmetic and logic on literals, done at generation time so the generated code says what it means. */
public class Folder {
  /** Works on a copy: JavaParser re-parents a child that is put under a new node, and the original tree would lose it. */
  public static Expression fold(Expression e) {
    return foldInPlace(e.clone());
  }

  private static Expression foldInPlace(Expression e) {
    if (e instanceof AssignExpr || e instanceof MethodCallExpr || e instanceof ArrayAccessExpr || e instanceof VariableDeclarationExpr || e instanceof FieldAccessExpr || e instanceof ObjectCreationExpr || e instanceof InstanceOfExpr) {
      for (var child : new ArrayList<>(e.getChildNodes()))
        if (child instanceof Expression c && !(c instanceof NameExpr && e instanceof AssignExpr a && a.getTarget() == c)) {
          Expression folded = foldInPlace(c.clone());
          if (!folded.toString().equals(c.toString()))
            c.replace(folded);
        } else if (child instanceof com.github.javaparser.ast.body.VariableDeclarator d && d.getInitializer().isPresent()) {
          Expression folded = foldInPlace(d.getInitializer().get().clone());
          if (!folded.toString().equals(d.getInitializer().get().toString()))
            d.setInitializer(folded);
        }
      return e;
    }
    if (e instanceof EnclosedExpr en) {
      Expression inner = foldInPlace(en.getInner());
      if (inner instanceof LiteralExpr || inner instanceof NameExpr || inner instanceof MethodCallExpr || inner instanceof ArrayAccessExpr || inner instanceof EnclosedExpr)
        return inner;
      return new EnclosedExpr(inner);
    }
    if (e instanceof UnaryExpr u) {
      Expression operand = foldInPlace(u.getExpression());
      if (operand instanceof BooleanLiteralExpr b && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT)
        return new BooleanLiteralExpr(!b.getValue());
      if (operand instanceof IntegerLiteralExpr n) {
        int v = n.asNumber().intValue();
        switch (u.getOperator()) {
          case MINUS: return Specializer.intLiteral(-v);
          case BITWISE_COMPLEMENT: return Specializer.intLiteral(~v);
          case PLUS: return n;
          default: break;
        }
      }
      if (operand instanceof EnclosedExpr en && en.getInner() instanceof BooleanLiteralExpr b && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT)
        return new BooleanLiteralExpr(!b.getValue());
      return new UnaryExpr(operand, u.getOperator());
    }
    if (e instanceof CastExpr c) {
      Expression inner = foldInPlace(c.getExpression());
      if (inner instanceof IntegerLiteralExpr n && c.getType().isPrimitiveType()) {
        int v = n.asNumber().intValue();
        switch (c.getType().asPrimitiveType().getType()) {
          case BYTE: return Specializer.intLiteral((byte) v);
          case INT: return n;
          default: break;
        }
      }
      return new CastExpr(c.getType(), inner);
    }
    if (e instanceof ConditionalExpr c) {
      Expression cond = foldInPlace(c.getCondition());
      if (cond instanceof BooleanLiteralExpr b)
        return foldInPlace(b.getValue() ? c.getThenExpr() : c.getElseExpr());
      return new ConditionalExpr(cond, foldInPlace(c.getThenExpr()), foldInPlace(c.getElseExpr()));
    }
    if (e instanceof MethodCallExpr m) {
      if (m.getNameAsString().equals("equals") && m.getScope().isPresent() && m.getScope().get() instanceof StringLiteralExpr a && m.getArgument(0) instanceof StringLiteralExpr b)
        return new BooleanLiteralExpr(a.getValue().equals(b.getValue()));
      return m;
    }
    if (!(e instanceof BinaryExpr b))
      return e;
    Expression l = foldInPlace(b.getLeft()), r = foldInPlace(b.getRight());
    BinaryExpr.Operator op = b.getOperator();
    if (l instanceof BooleanLiteralExpr lb && r instanceof BooleanLiteralExpr rb) {
      switch (op) {
        case AND: return new BooleanLiteralExpr(lb.getValue() && rb.getValue());
        case OR: return new BooleanLiteralExpr(lb.getValue() || rb.getValue());
        case EQUALS: return new BooleanLiteralExpr(lb.getValue() == rb.getValue());
        case NOT_EQUALS: return new BooleanLiteralExpr(lb.getValue() != rb.getValue());
        default: break;
      }
    }
    if (l instanceof BooleanLiteralExpr lb && isBoolean(r)) {
      switch (op) {
        case AND: return lb.getValue() ? r : lb;
        case OR: return lb.getValue() ? lb : r;
        case EQUALS: return lb.getValue() ? r : not(r);
        case NOT_EQUALS: return lb.getValue() ? not(r) : r;
        default: break;
      }
    }
    if (r instanceof BooleanLiteralExpr rb && isBoolean(l)) {
      switch (op) {
        case AND: return rb.getValue() ? l : rb;
        case OR: return rb.getValue() ? rb : l;
        case EQUALS: return rb.getValue() ? l : not(l);
        case NOT_EQUALS: return rb.getValue() ? not(l) : l;
        default: break;
      }
    }
    if (l instanceof IntegerLiteralExpr ln && r instanceof IntegerLiteralExpr rn) {
      int x = ln.asNumber().intValue(), y = rn.asNumber().intValue();
      switch (op) {
        case PLUS: return Specializer.intLiteral(x + y);
        case MINUS: return Specializer.intLiteral(x - y);
        case MULTIPLY: return Specializer.intLiteral(x * y);
        case DIVIDE: if (y != 0) return Specializer.intLiteral(x / y); break;
        case REMAINDER: if (y != 0) return Specializer.intLiteral(x % y); break;
        case BINARY_AND: return Specializer.intLiteral(x & y);
        case BINARY_OR: return Specializer.intLiteral(x | y);
        case XOR: return Specializer.intLiteral(x ^ y);
        case LEFT_SHIFT: return Specializer.intLiteral(x << y);
        case SIGNED_RIGHT_SHIFT: return Specializer.intLiteral(x >> y);
        case UNSIGNED_RIGHT_SHIFT: return Specializer.intLiteral(x >>> y);
        case EQUALS: return new BooleanLiteralExpr(x == y);
        case NOT_EQUALS: return new BooleanLiteralExpr(x != y);
        case LESS: return new BooleanLiteralExpr(x < y);
        case GREATER: return new BooleanLiteralExpr(x > y);
        case LESS_EQUALS: return new BooleanLiteralExpr(x <= y);
        case GREATER_EQUALS: return new BooleanLiteralExpr(x >= y);
        default: break;
      }
    }
    if ((op == BinaryExpr.Operator.PLUS || op == BinaryExpr.Operator.MINUS) && r instanceof IntegerLiteralExpr rn && l instanceof BinaryExpr lb
        && (lb.getOperator() == BinaryExpr.Operator.PLUS || lb.getOperator() == BinaryExpr.Operator.MINUS) && lb.getRight() instanceof IntegerLiteralExpr lrn) {
      int inner = lb.getOperator() == BinaryExpr.Operator.PLUS ? lrn.asNumber().intValue() : -lrn.asNumber().intValue();
      int outer = op == BinaryExpr.Operator.PLUS ? rn.asNumber().intValue() : -rn.asNumber().intValue();
      int sum = inner + outer;
      if (sum == 0)
        return lb.getLeft();
      return new BinaryExpr(lb.getLeft(), Specializer.intLiteral(Math.abs(sum)), sum > 0 ? BinaryExpr.Operator.PLUS : BinaryExpr.Operator.MINUS);
    }
    if (r instanceof IntegerLiteralExpr rn && rn.asNumber().intValue() == 0 && (op == BinaryExpr.Operator.PLUS || op == BinaryExpr.Operator.MINUS || op == BinaryExpr.Operator.BINARY_OR))
      return l;
    if (l instanceof IntegerLiteralExpr ln && ln.asNumber().intValue() == 0 && (op == BinaryExpr.Operator.PLUS || op == BinaryExpr.Operator.BINARY_OR))
      return r;
    return new BinaryExpr(l, r, op);
  }

  private static boolean isBoolean(Expression e) {
    return e instanceof BinaryExpr b && switch (b.getOperator()) {
      case AND, OR, EQUALS, NOT_EQUALS, LESS, GREATER, LESS_EQUALS, GREATER_EQUALS -> true;
      default -> false;
    } || e instanceof BooleanLiteralExpr || e instanceof InstanceOfExpr
        || e instanceof UnaryExpr u && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT
        || e instanceof EnclosedExpr en && isBoolean(en.getInner())
        || e instanceof NameExpr;
  }

  private static Expression not(Expression e) {
    if (e instanceof UnaryExpr u && u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT)
      return u.getExpression();
    if (e instanceof BinaryExpr b) {
      switch (b.getOperator()) {
        case EQUALS: return new BinaryExpr(b.getLeft(), b.getRight(), BinaryExpr.Operator.NOT_EQUALS);
        case NOT_EQUALS: return new BinaryExpr(b.getLeft(), b.getRight(), BinaryExpr.Operator.EQUALS);
        default: break;
      }
    }
    return new UnaryExpr(e instanceof BinaryExpr ? new EnclosedExpr(e) : e, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
  }
}
