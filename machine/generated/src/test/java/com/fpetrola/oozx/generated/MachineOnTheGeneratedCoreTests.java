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
