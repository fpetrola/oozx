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

package com.fpetrola.z80.minizx.emulation;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.io.FilenameUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class GameData {
  public SetMultimap<Integer, Integer> memoryAccesses = TreeMultimap.create();
  public SetMultimap<Integer, Integer> invertedMemoryAccesses = TreeMultimap.create();
  public List<LocalMemory> localMemoryList = new ArrayList<>();

  public TreeSet<Integer> spriteAddresses = new TreeSet<>();
  public TreeSet<Integer> attributesAddresses = new TreeSet<>();
  TreeSet<Integer> borderAddresses = new TreeSet<>();
  public TreeSet<Integer> soundAddresses = new TreeSet<>();
  public String name;

  public GameData() {
  }

  public GameData(String url) {
    try {
      url = url.replace(" ", "%20");
      if (!url.contains(":"))
        url = "file://" + url;

      URI uri = new URI(url);
      name = FilenameUtils.getBaseName(uri.getPath()).replace(" ", "_");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}