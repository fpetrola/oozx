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

package com.fpetrola.z80.helpers;

import java.util.List;

public class SingleItemInvoker<T> implements ItemHandler<T> {
  private final T item;

  public SingleItemInvoker(T item) {
    this.item = item;
  }

  public void execute(ItemInvoker<T> invoker) {
    invoker.invoke(item);
  }

  public ItemHandler<T> add(T item) {
    return new MultipleItemsInvoker<T>(List.of(this.item, item));
  }

  public List<T> getList() {
    return List.of(item);
  }

  public ItemHandler<T> remove(T item) {
    return new EmptyItemHandler<T>();
  }
}
