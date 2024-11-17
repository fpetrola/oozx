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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestFileParser {

    // Parses the .in file and returns a list of TestInput objects
    public List<TestInput> parseInputFile(File file) throws IOException {
        List<TestInput> tests = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse the test description
                String description = line.trim();
                
                // Parse the register state
                line = reader.readLine();
                RegisterState registerState = parseRegisterState(line);

                // Parse the memory setups
                List<MemorySetup> memorySetups = new ArrayList<>();
                while ((line = reader.readLine()) != null && !line.equals("-1")) {
                    memorySetups.add(parseMemorySetup(line));
                }

                // Add the TestInput object to the list
                tests.add(new TestInput(description, registerState, memorySetups));
            }
        }
        return tests;
    }

    // Parses the .expected file and returns a list of TestOutput objects
    public List<TestOutput> parseExpectedFile(File file) throws IOException {
        List<TestOutput> tests = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse the test description
                String description = line.trim();

                // Parse events
                List<Event> events = new ArrayList<>();
                while ((line = reader.readLine()) != null && !line.matches("\\d{4}")) {
                    events.add(parseEvent(line));
                }

                // Parse final register state
                RegisterState finalRegisterState = parseRegisterState(line);

                // Parse memory changes
                List<MemoryChange> memoryChanges = new ArrayList<>();
                while ((line = reader.readLine()) != null && !line.equals("-1")) {
                    memoryChanges.add(parseMemoryChange(line));
                }

                // Add the TestOutput object to the list
                tests.add(new TestOutput(description, events, finalRegisterState, memoryChanges));
            }
        }
        return tests;
    }

    // Helper to parse register states
    private RegisterState parseRegisterState(String line) {
        String[] parts = line.trim().split(" ");
        return new RegisterState(
            Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1], 16), Integer.parseInt(parts[2], 16), Integer.parseInt(parts[3], 16),
            Integer.parseInt(parts[4], 16), Integer.parseInt(parts[5], 16), Integer.parseInt(parts[6], 16), Integer.parseInt(parts[7], 16),
            Integer.parseInt(parts[8], 16), Integer.parseInt(parts[9], 16), Integer.parseInt(parts[10], 16), Integer.parseInt(parts[11], 16), Integer.parseInt(parts[12], 16),
            Integer.parseInt(parts[13], 16), Integer.parseInt(parts[14], 16), Integer.parseInt(parts[15], 16), Integer.parseInt(parts[16], 16), 1, parts[17].equals("1"),
            Integer.parseInt(parts[18])
        );
    }

    // Helper to parse memory setup in .in file
    private MemorySetup parseMemorySetup(String line) {
        String[] parts = line.split(" ");
        int startAddress = Integer.parseInt(parts[0], 16);
        List<Integer> bytes = new ArrayList<>();
        for (int i = 1; i < parts.length - 1; i++) {
            bytes.add(Integer.parseInt(parts[i], 16));
        }
        return new MemorySetup(startAddress, bytes);
    }

    // Helper to parse event in .expected file
    private Event parseEvent(String line) {
        String[] parts = line.trim().split(" ");
        int time = Integer.parseInt(parts[0]);
        String type = parts[1];
        int address = Integer.parseInt(parts[2], 16);
        Integer data = parts.length > 3 ? Integer.parseInt(parts[3], 16) : null;
        return new Event(time, type, address, data);
    }

    // Helper to parse memory change in .expected file
    private MemoryChange parseMemoryChange(String line) {
        String[] parts = line.split(" ");
        int address = Integer.parseInt(parts[0], 16);
        List<Integer> bytes = new ArrayList<>();
        for (int i = 1; i < parts.length - 1; i++) {
            bytes.add(Integer.parseInt(parts[i], 16));
        }
        return new MemoryChange(address, bytes);
    }
}
