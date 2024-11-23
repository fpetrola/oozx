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

import com.fpetrola.z80.cpu.Event;

import java.util.List;

// Represents a single test in the output file
public class TestOutput {
    private String description;
    private List<Event> events;
    private RegisterState finalRegisterState;
    private List<MemoryChange> memoryChanges;

    public TestOutput(String description, List<Event> events, RegisterState finalRegisterState, List<MemoryChange> memoryChanges) {
        this.description = description;
        this.events = events;
        this.finalRegisterState = finalRegisterState;
        this.memoryChanges = memoryChanges;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public RegisterState getFinalRegisterState() {
        return finalRegisterState;
    }

    public void setFinalRegisterState(RegisterState finalRegisterState) {
        this.finalRegisterState = finalRegisterState;
    }

    public List<MemoryChange> getMemoryChanges() {
        return memoryChanges;
    }

    public void setMemoryChanges(List<MemoryChange> memoryChanges) {
        this.memoryChanges = memoryChanges;
    }
}
