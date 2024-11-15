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

import java.util.List;

// Represents a single test in the input file
public class TestInput {
    private String description;
    private RegisterState registerState;
    private List<MemorySetup> memorySetups;

    public TestInput(String description, RegisterState registerState, List<MemorySetup> memorySetups) {
        this.description = description;
        this.registerState = registerState;
        this.memorySetups = memorySetups;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RegisterState getRegisterState() {
        return registerState;
    }

    public void setRegisterState(RegisterState registerState) {
        this.registerState = registerState;
    }

    public List<MemorySetup> getMemorySetups() {
        return memorySetups;
    }

    public void setMemorySetups(List<MemorySetup> memorySetups) {
        this.memorySetups = memorySetups;
    }
}

