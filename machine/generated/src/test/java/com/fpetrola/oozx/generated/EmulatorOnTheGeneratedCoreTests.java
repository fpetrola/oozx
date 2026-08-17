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
