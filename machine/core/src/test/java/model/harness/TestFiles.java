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

package model.harness;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * A test file as a File, wherever it is kept.
 * <p>
 * These used to be read with Path.of(getResource(...).toURI()), which works only while the
 * resource is a loose file in a directory. The recordings and tapes live in the emulator's test
 * jar now, so the URL is a jar: one and that call throws - the file is copied out instead, which
 * works either way.
 */
public class TestFiles {
  public static File testFile(String resource) {
    try (InputStream from = TestFiles.class.getResourceAsStream(resource)) {
      if (from == null) {
        throw new IllegalStateException("the " + resource + " test resource is missing");
      }
      Path copy = Files.createTempFile("test", resource.substring(resource.lastIndexOf('.')));
      Files.copy(from, copy, StandardCopyOption.REPLACE_EXISTING);
      copy.toFile().deleteOnExit();
      return copy.toFile();
    } catch (IOException couldNotRead) {
      throw new IllegalStateException("could not read the test resource " + resource, couldNotRead);
    }
  }
}
