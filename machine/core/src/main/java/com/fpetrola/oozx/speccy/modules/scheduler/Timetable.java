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

package com.fpetrola.oozx.speccy.modules.scheduler;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * The plan: which tasks are due and when, soonest first. Two due together go in the order they
 * were registered. A task has one row, so asking for it again moves it rather than adding another.
 */
final class Timetable {
  /** A task and when it is due. Kept for as long as the task is registered, and reused. */
  static final class Row {
    final Task task;
    private final int order;
    long due;

    private Row(Task task, int order) {
      this.task = task;
      this.order = order;
    }
  }

  /** What {@link #nextDue()} answers with nothing pending: no T-state is before it. */
  static final long NOTHING_DUE = Long.MIN_VALUE;

  private final SortedSet<Row> pending = new ObjectAVLTreeSet<>(Timetable::soonerFirst);
  private final Map<Task, Row> rowOf = new IdentityHashMap<>();
  private long nextDue = NOTHING_DUE;

  private static int soonerFirst(Row a, Row b) {
    return a.due != b.due ? Long.compare(a.due, b.due) : Integer.compare(a.order, b.order);
  }

  void add(Task task) {
    if (rowOf.putIfAbsent(task, new Row(task, rowOf.size())) != null) {
      throw new IllegalStateException(task.name() + " is registered already");
    }
  }

  void put(Task task, long due) {
    // Out before its due T-state changes, since that is what the rows are sorted on.
    Row row = rowOf(task);
    pending.remove(row);
    row.due = due;
    pending.add(row);
    nextDue = pending.first().due;
  }

  void remove(Task task) {
    if (pending.remove(rowOf(task))) {
      whenIsNext();
    }
  }

  boolean isEmpty() {
    return pending.isEmpty();
  }

  long nextDue() {
    return nextDue;
  }

  Row takeFirst() {
    Row first = pending.first();
    pending.remove(first);
    whenIsNext();
    return first;
  }

  void shiftAll(long delta) {
    // Every row moves by the same amount, which leaves them in the order they were in, so they
    // are changed where they lie instead of being sorted again fifty times a second.
    for (Row row : pending) {
      row.due -= delta;
    }
    whenIsNext();
  }

  void clear() {
    pending.clear();
    nextDue = NOTHING_DUE;
  }

  private void whenIsNext() {
    nextDue = pending.isEmpty() ? NOTHING_DUE : pending.first().due;
  }

  private Row rowOf(Task task) {
    Row row = rowOf.get(task);
    if (row == null) {
      throw new IllegalStateException(task.name() + " was never registered");
    }
    return row;
  }
}
