/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
