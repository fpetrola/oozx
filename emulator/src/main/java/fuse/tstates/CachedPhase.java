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

package fuse.tstates;

import fuse.tstates.phases.*;

public class CachedPhase implements Phase {
  private PhaseVisitor visitor;
  private BeforeExecutionPhaseVisitor beforeExecutionPhaseVisitors;
  private AfterMRPhaseVisitor afterMRPhaseVisitors;
  private BeforeWritePhaseVisitor beforeWritePhaseVisitors;
  private AfterExecutionPhaseVisitor afterExecutionPhaseVisitors;

  private boolean ready = false;
  private boolean skippable = true;

  public CachedPhase() {
    this.visitor = getVisitor();
  }

  public void accept(PhaseVisitor visitor) {
  }

  public void acceptAfterExecution(AfterExecutionPhaseVisitor visitor) {
    skippable= false;
    afterExecutionPhaseVisitors = visitor;
  }

  public void acceptAfterMR(AfterMRPhaseVisitor visitor) {
    skippable= false;
    afterMRPhaseVisitors = visitor;
  }

  public void acceptBeforeExecution(BeforeExecutionPhaseVisitor visitor) {
    skippable= false;
    beforeExecutionPhaseVisitors = visitor;
  }

  public void acceptBeforeWrite(BeforeWritePhaseVisitor visitor) {
    skippable= false;
    beforeWritePhaseVisitors = visitor;
  }

  public PhaseVisitor getVisitor() {
    return new ConfigurablePhaseVisitor();
  }

  public void execute(Phase phase) {
    phase.accept(visitor);
  }

  public void ready() {
    ready = true;
  }

  public boolean isReady() {
    return ready;
  }

  public boolean isSkippable() {
    return skippable;
  }

  private class ConfigurablePhaseVisitor implements PhaseVisitor {
    public void visit(BeforeExecution beforeExecution) {
      if (beforeExecutionPhaseVisitors != null)
        beforeExecutionPhaseVisitors.visit(beforeExecution);
    }

    public void visit(AfterExecution afterExecution) {
      if (afterExecutionPhaseVisitors != null)
        afterExecutionPhaseVisitors.visit(afterExecution);
    }

    public void visit(AfterMR afterMR) {
      if (afterMRPhaseVisitors != null)
        afterMRPhaseVisitors.visit(afterMR);
    }

    public void visit(BeforeWrite beforeWrite) {
      if (beforeWritePhaseVisitors != null)
        beforeWritePhaseVisitors.visit(beforeWrite);
    }
  }
}
