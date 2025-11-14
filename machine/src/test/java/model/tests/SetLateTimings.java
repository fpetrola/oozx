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

package model.tests;

import com.fpetrola.oozx.fuse.LibretroCore;
import com.fpetrola.oozx.fuse.bridge.EmulatorCommand;

public class SetLateTimings implements EmulatorCommand {
  private final boolean b;

  public SetLateTimings(boolean b) {
    this.b = b;
  }

  public Object execute(LibretroCore core) {
    core.retro_set_late_timings(b ? 0 : 1);
    return null;
  }
}
