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

package com.fpetrola.oozx;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Utils {
  public static int openFile(String dock, int autoload, Object o) {
    return 0;
  }

  public static int readAuxiliaryFile(String filename, File rom, AuxiliaryType auxiliaryType) {
    try {
      URL resource = Utils.class.getResource("/" + filename);
      rom.buffer = FileUtils.readFileToByteArray(new java.io.File(resource.getFile()));
      rom.length = rom.buffer.length;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return 0;
  }

  public static void closeFile(File rom) {

  }

  public static int readFile(String filename, File file) {
    return 0;
  }

  public enum AuxiliaryType {ROM}

  public static class File {
    public int length;
    public byte[] buffer;
  }
}
