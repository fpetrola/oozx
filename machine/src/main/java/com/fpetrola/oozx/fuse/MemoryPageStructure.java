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

import com.sun.jna.Structure;

import java.util.List;

public class MemoryPageStructure extends Structure {
  public int writable; // Can we write to this data?
  public int contended; // Are reads/writes to this page contended?
  public int source; // Where did this page come from?
  public int save_to_snapshot; // Should this page be saved to snapshots?
  public int page_num; // Which page from the source
  public int offset; // How far into the page this chunk starts

  @Override
  protected List<String> getFieldOrder() {
    return List.of("writable", "contended", "source", "save_to_snapshot", "page_num", "offset");
  }
}
