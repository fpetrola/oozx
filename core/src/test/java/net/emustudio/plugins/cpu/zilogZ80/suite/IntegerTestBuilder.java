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
package net.emustudio.plugins.cpu.zilogZ80.suite;

import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.cpu.testsuite.TestBuilder;
import net.emustudio.cpu.testsuite.injectors.MemoryByte;
import net.emustudio.cpu.testsuite.injectors.MemoryExpand;
import net.emustudio.plugins.cpu.zilogZ80.suite.injectors.RegisterPair;
import net.emustudio.plugins.cpu.zilogZ80.suite.injectors.RegisterPair2;
import net.emustudio.plugins.cpu.zilogZ80.suite.injectors.RegisterPairPSW;

import java.util.Objects;
import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_A;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.get8MSBplus8LSB;

public class IntegerTestBuilder extends TestBuilder<Integer, IntegerTestBuilder, CpuRunnerImpl, CpuVerifierImpl> {

    public IntegerTestBuilder(CpuRunnerImpl cpuRunner, CpuVerifierImpl cpuVerifier) {
        super(cpuRunner, cpuVerifier);
    }

    public IntegerTestBuilder firstIsPair(int registerPair) {
        runner.injectFirst(new MemoryExpand<>(), new RegisterPair(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsPair(int registerPair) {
        runner.injectSecond(new MemoryExpand<>(), new RegisterPair(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsPair2(int registerPair) {
        runner.injectSecond(new MemoryExpand<>(), new RegisterPair2(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsIX() {
        runner.injectSecond((tmpRunner, second) -> cpuRunner.setIX(second));
        return this;
    }

    public IntegerTestBuilder secondIsIY() {
        runner.injectSecond((tmpRunner, second) -> cpuRunner.setIY(second));
        return this;
    }

    public IntegerTestBuilder firstIsPSW() {
        runner.injectFirst(new MemoryExpand<>(), new RegisterPairPSW(3));
        return this;
    }

    public IntegerTestBuilder firstIsIX() {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.setIX(first));
        return this;
    }

    public IntegerTestBuilder firstIsIY() {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.setIY(first));
        return this;
    }

    public IntegerTestBuilder first8MSBisIX() {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.setIX(first & 0xFF00));
        return this;
    }

    public IntegerTestBuilder first8LSBisRegister(int register) {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.setRegister(register, first & 0xFF));
        return this;
    }

    public IntegerTestBuilder first8MSBisRegister(int register) {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.setRegister(register, (first >>> 8) & 0xFF));
        return this;
    }

    public IntegerTestBuilder first8MSBisIY() {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.setIY(first & 0xFF00));
        return this;
    }

    public IntegerTestBuilder first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte() {
        runner.injectTwoOperands((tmpRunner, first, second) ->
                new MemoryByte<>(get8MSBplus8LSB(first)).accept(tmpRunner, second.byteValue())
        );
        return this;
    }

    public IntegerTestBuilder first8MSBplus8LSBisMemoryByte(int value) {
        runner.injectTwoOperands((tmpRunner, first, second) ->
                new MemoryByte<>(get8MSBplus8LSB(first)).accept(tmpRunner, (byte) value)
        );
        return this;
    }

    public IntegerTestBuilder secondIsPSW() {
        runner.injectSecond(new MemoryExpand<>(), new RegisterPairPSW(3));
        return this;
    }

    public IntegerTestBuilder firstIsAF() {
        runner.injectFirst((tmpRunner, first) -> {
            cpuRunner.resetFlags();
            cpuRunner.setFlags(first & 0xFF);
            cpuRunner.setRegister(REG_A, (first >>> 8) & 0xFF);
        });
        return this;
    }

    public IntegerTestBuilder secondIsAF2() {
        runner.injectSecond((tmpRunner, second) -> {
            cpuRunner.resetFlags2();
            cpuRunner.setFlags2(second & 0xFF);
            cpuRunner.setRegister2(REG_A, (second >>> 8) & 0xFF);
        });
        return this;
    }

    public IntegerTestBuilder first8MSBisDeviceAndFirst8LSBIsPort() {
        runner.injectFirst((tmpRunner, first) ->
                cpuRunner.getDevice(first & 0xFF).setValue((byte) ((first >>> 8) & 0xFF)));
        return this;
    }

    public IntegerTestBuilder disableIFF1() {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.disableIFF1());
        return this;
    }

    public IntegerTestBuilder enableIFF2() {
        runner.injectFirst((tmpRunner, first) -> cpuRunner.enableIFF2());
        return this;
    }

    public IntegerTestBuilder verifyPSW(Function<RunnerContext<Integer>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        runner.verifyAfterTest(context -> cpuVerifier.checkRegisterPairPSW(3, operation.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyRegister(int register, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        return verifyRegister(register);
    }

    public IntegerTestBuilder verifyRegister(int register) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        Function<RunnerContext<Integer>, Integer> operation = lastOperation;
        runner.verifyAfterTest(context -> cpuVerifier.checkRegister(register, operation.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyPair(int registerPair, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkRegisterPair(registerPair, operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyPair2(int registerPair, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkRegisterPair2(registerPair, operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyIX(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkIX(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyIY(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkIY(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyPC(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkPC(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyR(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkR(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyIFF1isEnabled() {
        runner.verifyAfterTest(context -> cpuVerifier.checkInterruptsAreEnabled(0));
        return this;
    }

    public IntegerTestBuilder verifyAF(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkAF(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyAF2(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkAF2(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyDeviceWhenFirst8LSBisPort(Function<RunnerContext<Integer>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        runner.verifyAfterTest(context -> cpuVerifier.checkDeviceValue(context.first & 0xFF, operation.apply(context)));
        return this;
    }

}
