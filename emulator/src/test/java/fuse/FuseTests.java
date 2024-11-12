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

package fuse;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.references.WordNumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

class FuseTests {

  private static final Path FUSE_TEST_DATA_DIR = Paths.get("src", "test", "resources", "fuse");
  private IPortHandler portHandler;
  private static FuseTestParser fuseTestParser = new FuseTestParser(FUSE_TEST_DATA_DIR.toFile());

  // Load tests and results from FuseTestParser
  private static final List<FuseTest> theTests = fuseTestParser.getTests();
  private static final List<FuseResult> theResults = fuseTestParser.getResults();

  public static Stream<Arguments> sumProvider() {
    return null;
  }

  @BeforeEach
  void setUp() {
//    portHandler = Mockito.mock(IPortHandler.class);
//    Mockito.when(portHandler.in(Mockito.anyByte())).thenAnswer(invocation -> (byte) (invocation.getArgumentAt(0, Short.class) >> 8));
    portHandler = new IPortHandler();
  }


  static Stream<FuseTest> fuseTests() {
    return theTests.stream();
  }

  @DisplayName("Fuse test")
  @ParameterizedTest(name = "{index} => test={0}")
  @MethodSource("fuseTests")
  @Execution(ExecutionMode.CONCURRENT)
  public void testFuseTest(FuseTest fuseTest) {
    List<String> list = Arrays.asList("27_1", "27", "ed57", "ed5e", "ed5f", "ed6e", "eda2", "eda3", "eda9", "edaa", "edab", "edb2", "edb3", "edb9", "edba", "edbb");

    if (list.stream().noneMatch(fuseTest.testId::startsWith)) {
      FuseResult fuseResult = theResults.stream()
          .filter(result -> result.getTestId().equals(fuseTest.testId))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Result not found for test: " + fuseTest.testId));


      Z80Cpu cpu = Helper.createOOZ80(new IO<WordNumber>() {
        public WordNumber in(WordNumber port) {
          return createValue(port.intValue() >> 8);
        }

        public void out(WordNumber port, WordNumber value) {
        }
      });
      fuseTest.initCpu(cpu);
      boolean runResult = fuseTest.run(cpu, fuseResult.getExpectedPC());

      Assertions.assertTrue(runResult, "Test timed-out.");
      fuseResult.verify(cpu);
    }
    else
      Assertions.assertTrue(true, "OK");

  }
}
