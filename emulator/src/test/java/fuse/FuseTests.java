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

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.NullInstructionSpy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.B;

@SuppressWarnings("ALL")
public class FuseTests {
  private static final Path FUSE_TEST_DATA_DIR = Paths.get("src", "test", "resources", "fuse");
  private static FuseTestParser fuseTestParser = new FuseTestParser(FUSE_TEST_DATA_DIR.toFile());

  private static final List<FuseTest> theTests = fuseTestParser.getTests();
  private static final List<FuseResult> theResults = fuseTestParser.getResults();

  static Stream<FuseTest> fuseTests() {
    return theTests.stream();
  }

  @DisplayName("Fuse test")
  @ParameterizedTest(name = "{index} => {0}")
  @MethodSource("fuseTests")
  @Execution(ExecutionMode.CONCURRENT)
  public void testFuseTest(FuseTest fuseTest) {
    FuseResult fuseResult = theResults.stream()
        .filter(result -> result.getTestId().equals(fuseTest.testId))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Result not found for test: " + fuseTest.testId));

    fuseTest.initCpu();
    boolean runResult = fuseTest.run(fuseResult.getExpectedPC());

    Assertions.assertTrue(runResult, "Test timed-out.");
    fuseResult.verify(fuseTest.cpu);
  }
}
