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

package com.fpetrola.z80.metadata;

import java.util.HashMap;
import java.util.Map;

public class DataStructure {
  public Map<Integer, DataStructureInstance> instances = new HashMap<>();

  public DataStructure() {
  }

  public DataStructureInstance getInstance(int instance) {
    DataStructureInstance dataStructureInstance = instances.get(instance);
    if (dataStructureInstance == null)
      instances.put(instance, dataStructureInstance = new DataStructureInstance());
    return dataStructureInstance;
  }
}
