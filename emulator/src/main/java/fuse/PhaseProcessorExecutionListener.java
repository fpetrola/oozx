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

package fuse;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.ExecutionListener;
import fuse.tstates.PhaseProcessor;
import fuse.tstates.phases.AfterExecution;
import fuse.tstates.phases.BeforeExecution;

public class PhaseProcessorExecutionListener<T extends WordNumber> implements ExecutionListener<T> {
  private final PhaseProcessor<T> phaseProcessor;
  private AfterExecution afterExecution = new AfterExecution();
  private BeforeExecution beforeExecution = new BeforeExecution();

  public PhaseProcessorExecutionListener(PhaseProcessor<T> phaseProcessor) {
    this.phaseProcessor = phaseProcessor;
  }

  public void beforeExecution(Instruction<T> instruction) {
    phaseProcessor.processPhase(beforeExecution);
  }

  public void afterExecution(Instruction<T> instruction) {
    phaseProcessor.processPhase(afterExecution);
  }
}
