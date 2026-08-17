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
package com.fpetrola.oozx.generated;

import com.fpetrola.z80.AluReferenceTest;
import fuse.FuseTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * The emulator's batteries - the 1355 Fuse tests, the ALU reference, the emustudio instruction
 * tests - run again on the generated core. They do not know: this module registers the core
 * they find on the classpath.
 */
@Suite
@SuiteDisplayName("the emulator's batteries on the generated core")
@SelectClasses({FuseTests.class, AluReferenceTest.class})
@SelectPackages("net.emustudio.plugins.cpu.zilogZ80")
public class EmulatorOnTheGeneratedCoreTests {
}
