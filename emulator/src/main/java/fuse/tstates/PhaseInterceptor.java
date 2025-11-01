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

public class PhaseInterceptor implements Phase {
  private final PhaseVisitor visitor;
  private BeforeExecutionPhaseVisitor beforeExecutionPhaseVisitors = (e) -> {
  };
  private AfterMRPhaseVisitor afterMRPhaseVisitors = (e) -> {
  };
  private BeforeWritePhaseVisitor beforeWritePhaseVisitors = (e) -> {
  };
  private AfterExecutionPhaseVisitor afterExecutionPhaseVisitors = (e) -> {
  };
  private boolean ready= false;

  public PhaseInterceptor() {
    this.visitor = getVisitor();
  }

  public void accept(PhaseVisitor visitor) {
  }

  public void acceptAfterExecution(AfterExecutionPhaseVisitor visitor) {
    afterExecutionPhaseVisitors = visitor;
  }

  public void acceptAfterMR(AfterMRPhaseVisitor visitor) {
    afterMRPhaseVisitors = visitor;
  }

  public void acceptBeforeExecution(BeforeExecutionPhaseVisitor visitor) {
    beforeExecutionPhaseVisitors = visitor;
  }

  public void acceptBeforeWrite(BeforeWritePhaseVisitor visitor) {
    beforeWritePhaseVisitors = visitor;
  }

  public PhaseVisitor getVisitor() {
    return new PhaseVisitor() {
      public void visit(BeforeExecution beforeExecution) {
        beforeExecutionPhaseVisitors.visit(beforeExecution);
      }

      public void visit(AfterExecution afterExecution) {
        afterExecutionPhaseVisitors.visit(afterExecution);
      }

      public void visit(AfterMR afterMR) {
        afterMRPhaseVisitors.visit(afterMR);
      }

      public void visit(BeforeWrite beforeWrite) {
        beforeWritePhaseVisitors.visit(beforeWrite);
      }
    };
  }

  public void execute(Phase phase) {
    phase.accept(visitor);
  }

  public void ready() {
    ready= true;
  }

  public boolean isReady() {
    return ready;
  }
}
