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
import java.util.Arrays;
import java.util.List;

public class CollectionHandler<T>
{
	protected T singleItem;
	protected List<T> multipleItems;

	public CollectionHandler()
	{
	}

	public CollectionHandler(List<T> elements)
	{
		setMultipleItems(elements);
	}

	public void add(T item)
	{
		if (singleItem == null && multipleItems == null)
			singleItem= item;
		else
		{
			if (multipleItems == null)
			{
				multipleItems= new ArrayList<T>();
				multipleItems.add(singleItem);
				singleItem= null;
			}
			multipleItems.add(item);
		}
	}

	public void forAll(ItemInvoker<T> invoker)
	{
		if (singleItem != null)
			invoker.invoke(singleItem);
		else if (multipleItems != null)
      for (T multipleItem : multipleItems) invoker.invoke(multipleItem);
	}

	public List<T> getList()
	{
		if (singleItem != null)
			return List.of(singleItem);
		else
			return multipleItems != null ? new ArrayList<T>(multipleItems) : new ArrayList<T>();
	}

	public List<T> getMultipleItems()
	{
		return multipleItems;
	}

	public T getSingleItem()
	{
		return singleItem;
	}

	public boolean isEmpty()
	{
		return singleItem == null && multipleItems == null;
	}

	public void remove(T item)
	{
		if (multipleItems == null)
			singleItem= null;
		else
		{
			multipleItems.remove(item);
			if (multipleItems.isEmpty())
				multipleItems= null;
		}
	}

	public void removeAll()
	{
		singleItem= null;
		multipleItems= null;
	}

	public void setMultipleItems(List<T> multipleItems)
	{
		this.multipleItems= multipleItems;
	}
	public void setSingleItem(T singleItem)
	{
		this.singleItem= singleItem;
	}
}