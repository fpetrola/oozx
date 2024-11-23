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

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;

@BuildParseTree
public class TestFileParser extends BaseParser<Object> {

  public Rule File() {
    return Sequence(
        ZeroOrMore(TestDefinition()),
        EOI
    );
  }

  public Rule TestDefinition() {
    return Sequence(
        Description(),
        CPUState(),
        MemorySetup(),
        TStatesToRun()
    );
  }

  public Rule Description() {
    return Sequence(
        TextLine(),
        Optional(NewLine())
    );
  }

  public Rule CPUState() {
    return Sequence(
        Integer(), Integer(), "DE", Integer(), "HL", Integer(),
        "AF'", Integer(), "BC'", Integer(), "DE'", Integer(), "HL'", Integer(),
        "IX", Integer(), "IY", Integer(), "SP", Integer(), "PC", Integer(),
        "MEMPTR", Integer(),
        "I", Integer(), "R", Integer(), "IFF1", Integer(), "IFF2", Integer(),
        "IM", Integer(), Halted()
    );
  }

  public Rule MemorySetup() {
    return ZeroOrMore(MemoryLine());
  }

  public Rule MemoryLine() {
    return Sequence(
        Integer(), ZeroOrMore(Integer()), "-1", Optional(NewLine())
    );
  }

  public Rule TStatesToRun() {
    return Sequence(
        Integer(),
        Optional(NewLine())
    );
  }

  public Rule Halted() {
    return FirstOf("true", "false");
  }

  public Rule TextLine() {
    return OneOrMore(FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), Digit(), AnyOf(" _")));
  }

  public Rule Integer() {
    return OneOrMore(Digit());
  }

  public Rule Digit() {
    return CharRange('0', '9');
  }

  public Rule NewLine() {
    return AnyOf("\r\n");
  }
}
