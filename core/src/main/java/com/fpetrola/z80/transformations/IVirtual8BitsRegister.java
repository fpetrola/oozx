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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.opcodes.references.WordNumber;

public interface IVirtual8BitsRegister<T extends WordNumber> extends VirtualRegister<T> {
  T readPrevious();

  VirtualComposed16BitRegister<T> getVirtualComposed16BitRegister();

  public IVirtual8BitsRegister<T> getCurrentPreviousVersion();

  public void addPreviousVersion(IVirtual8BitsRegister previousVersion);

  void set16BitsRegister(VirtualComposed16BitRegister<T> virtualComposed16BitRegister);

  void addDependant(VirtualRegister virtualRegister);
}
