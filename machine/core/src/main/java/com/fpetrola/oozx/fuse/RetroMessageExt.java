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

package com.fpetrola.oozx.fuse;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class RetroMessageExt extends Structure {

  public RetroMessageExt(Pointer data) {
    super(data);
  }

  // campos en el mismo orden que en C
  public String msg;          // const char* (JNA lo convierte automáticamente en String UTF-8)
  public int frames;          // unsigned -> int alcanza
  public int priority;        // unsigned -> int
  public int level;           // enum retro_log_level (usar int)
  public int target;          // enum retro_message_target
  public int type;            // enum retro_message_type
  public int id;

  @Override
  protected List<String> getFieldOrder() {
    return Arrays.asList("msg", "frames", "priority", "level", "target", "type", "id");
  }
}
