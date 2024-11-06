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
package net.emustudio.plugins.cpu.zilogZ80.suite.injectors;

import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;

import java.util.function.BiConsumer;

public class RegisterPairPSW implements BiConsumer<CpuRunnerImpl, Integer> {
    private final int registerPairPSW;

    public RegisterPairPSW(int registerPairPSW) {
        this.registerPairPSW = registerPairPSW;
    }

    @Override
    public void accept(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPairPSW(registerPairPSW, value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("registerPairPSW[%x]", registerPairPSW);
    }

}
