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

import com.fpetrola.z80.opcodes.references.WordNumber;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Helper {
  public static boolean hex = false;

  private static String convertToHex(int routineAddress) {
    return Long.toHexString(routineAddress).toUpperCase();
  }

  public static String formatAddress(int routineAddress) {
    return hex ? Long.toHexString(routineAddress).toUpperCase() : routineAddress + "";
  }

  public static <T extends WordNumber> String convertToHex(T value) {
    return value.toString();
  }

  public static <T> T createInstance(Class<T> type) {
    T instance;
    try {
      Constructor<T> constructor = type.getConstructor(null);
      instance = constructor.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return (T) instance;
  }

  public static String getSnapshotFile(String url) {
    try {
      String s = url;
      String first = "/tmp/game.z80";
      Files.copy(new URL(s).openStream(), Paths.get(first), StandardCopyOption.REPLACE_EXISTING);
      return first;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
