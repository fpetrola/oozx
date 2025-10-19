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

package com.fpetrola.z80.helpers;

import java.util.ArrayList;
import java.util.List;

public class MultipleItemsInvoker<T> implements ItemHandler<T> {
  protected List<T> multipleItems = new ArrayList<>();

  public MultipleItemsInvoker(List<T> items) {
    multipleItems.addAll(items);
  }

  public void execute(ItemInvoker<T> invoker) {
    for (T multipleItem : multipleItems) invoker.invoke(multipleItem);
  }

  public ItemHandler<T> add(T item) {
    multipleItems.add(item);
    return this;
  }

  public List<T> getList() {
    return multipleItems;
  }

  public ItemHandler<T> remove(T item) {
    multipleItems.remove(item);
    if (multipleItems.size() == 1) {
      return new SingleItemInvoker<>(multipleItems.get(0));
    } else
      return this;
  }
}
