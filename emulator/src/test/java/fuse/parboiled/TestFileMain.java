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

package fuse.parboiled;

import fuse.FuseTests;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestFileMain {
    public static void main(String[] args) throws Exception {
        URI inFile1 = TestFileParser.class.getResource("/fuse/tests.in").toURI();
        File inFile = new File(inFile1.toURL().toURI());

        String input = new String(Files.readAllBytes(inFile.toPath()));

        TestFileParser parser = new TestFileParser();
        ParsingResult<?> result = new ReportingParseRunner<>(parser.File()).run(input);

        if (result.hasErrors()) {
            System.err.println("Parsing failed:");
            System.err.println(result.parseErrors);
        } else {
            System.out.println("Parsing succeeded:");
            System.out.println(result.parseTreeRoot.getValue());
        }
    }
}
