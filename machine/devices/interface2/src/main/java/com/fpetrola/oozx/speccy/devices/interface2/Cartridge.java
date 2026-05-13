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
package com.fpetrola.oozx.speccy.devices.interface2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** A ROM cartridge: 16K, and the name on its label. */
public record Cartridge(String name, byte[] image, String path) {

  public static final int SIZE = 0x4000;

  public static Cartridge read(File file) throws IOException {
    byte[] image = Files.readAllBytes(file.toPath());
    if (image.length != SIZE) {
      throw new IOException(file.getName() + " is " + image.length + " bytes; a cartridge is 16K");
    }
    return new Cartridge(file.getName().replaceFirst("\\.[^.]*$", ""), image, file.getPath());
  }
}
