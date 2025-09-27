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

package com.fpetrola.oozx.fuse;

import java.util.Objects;

public class TStateUpdate {
  public final int key;
  public final int value;
  public final String description;
  public final int pc;

  public TStateUpdate(int key, int value, String description, int pc) {
    this.key = key;
    this.value = value;
    this.description = description;
    this.pc = pc;
  }

  public String toString() {
    return "TStateUpdate{" +
        "key=" + key +
        ", value=" + value +
        ", description='" + description + '\'' +
//        ", pc='" + pc + '\'' +
        "}\n";
  }

  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TStateUpdate that = (TStateUpdate) o;
    return key == that.key && value == that.value && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value, description);
  }
}
