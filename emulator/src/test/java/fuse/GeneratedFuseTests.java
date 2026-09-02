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

package fuse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/** The 1355 Fuse tests, event by event, against the generated core. */
public class GeneratedFuseTests {
  private static final FuseTestParser parser = new FuseTestParser(Paths.get("fuse"), true);
  private static final List<FuseTest> tests = parser.getTests();
  private static final List<FuseResult> results = parser.getResults();

  static Stream<FuseTest> fuseTests() {
    return tests.stream();
  }

  @DisplayName("Fuse test on the generated core")
  @ParameterizedTest(name = "{index} => {0}")
  @MethodSource("fuseTests")
  public void testFuseTest(FuseTest fuseTest) {
    FuseResult fuseResult = results.stream().filter(result -> result.getTestId().equals(fuseTest.testId)).findFirst()
        .orElseThrow(() -> new AssertionError("Result not found for test: " + fuseTest.testId));
    fuseTest.initCpu();
    Assertions.assertTrue(fuseTest.run(fuseResult.getExpectedPC()), "Test timed-out.");
    fuseResult.verify(fuseTest.cpu, fuseTest.events.list());
  }
}
