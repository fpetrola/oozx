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

package model.tests;

import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.fuse.LibretroCore;
import com.fpetrola.oozx.fuse.bridge.EmulatorCommand;

public class GetMemoryMapWrite implements EmulatorCommand {
  private int i;

  public GetMemoryMapWrite(int i) {
    this.i = i;
  }

  public Object execute(LibretroCore core) {
    MemoryPage memoryPage = new MemoryPage();
    memoryPage.pageNum= core.retro_get_memory_map_write_page_num(i);
    memoryPage.source= core.retro_get_memory_map_write_source(i);
    return memoryPage;
  }
}
