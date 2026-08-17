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

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * The machine's own tests, run again with the generated core plugged in. They do not know: this
 * module is on the classpath, so every machine they build gets it, and the ones that put the
 * machine under the harness get the OOP core as they would anywhere. The slow ones stay in the
 * machine's own slow suite.
 */
@Suite
@SuiteDisplayName("the machine's tests on the generated core")
@SelectPackages("model.tests")
@ExcludeTags("slow")
public class MachineOnTheGeneratedCoreTests {
}
