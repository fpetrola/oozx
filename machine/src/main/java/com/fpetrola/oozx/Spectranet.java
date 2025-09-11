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

public class Spectranet {
  public static boolean paged;
  public static boolean w5100PagedA;
  public static boolean w5100PagedB;

  public static byte w5100Read(MemoryPage mapping, int address) {
    return 0;
  }

  public static void flashRomWrite(int address, byte b) {

  }

  public static void w5100Write(MemoryPage mapping, int address, byte b) {

  }
}
