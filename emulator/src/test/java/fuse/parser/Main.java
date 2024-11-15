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

package fuse.parser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
  private static final Path FUSE_TEST_DATA_DIR = Paths.get("src", "test", "resources", "fuse");

  public static void main(String[] args) throws URISyntaxException {
    try {
      File testDataDir = FUSE_TEST_DATA_DIR.toFile();
      File inFile = getFile("tests.in");
      File expectedFile = getFile("tests.expected");

      TestFileParser parser = new TestFileParser();

      // Parse the .in file
      List<TestInput> inputs = parser.parseInputFile(inFile);

      // Parse the .expected file
      List<TestOutput> outputs = parser.parseExpectedFile(expectedFile);

      // Example of accessing parsed data
      inputs.forEach(System.out::println);
      outputs.forEach(System.out::println);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static File getFile(String s) throws URISyntaxException {
    URL resource = Main.class.getResource("/fuse/" + s);
    File path = Paths.get(resource.toURI()).toFile();
    return path;
  }
}
