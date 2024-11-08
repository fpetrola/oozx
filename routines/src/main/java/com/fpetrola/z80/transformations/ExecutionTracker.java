/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.blocks.*;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class ExecutionTracker implements BlockRoleVisitor {
  private final Instruction instruction;
  private final int pcValue;

  public ExecutionTracker(Instruction instruction, int pcValue) {
    this.instruction = instruction;
    this.pcValue = pcValue;
  }

  @Override
  public void visiting(CodeBlockType codeBlockType) {
    Block block = codeBlockType.getBlock();
    BlocksManager blocksManager = block.getBlocksManager();

    if (!codeBlockType.getBlock().isCompleted()) {
      int instructionLength = getInstructionLength();

      if (block.contains(pcValue)) {
        int lastAddress = pcValue + instructionLength - 1;
        if (!block.contains(lastAddress)) {
          block.growBlockTo(lastAddress + 1);
        }
      } else if (block.canTake(pcValue)) {
        block.growBlockTo(pcValue + instructionLength);
      }

      if (instruction instanceof ConditionalInstruction conditionalInstruction) {
        Block block1 = codeBlockType.getBlock();
        block1.setCompleted(true);
        WordNumber jumpAddress = conditionalInstruction.calculateJumpAddress();
      }

      codeBlockType.addInstruction(instruction);
    }
  }

  @Override
  public void visiting(UnknownBlockType unknownBlockType) {

    Block block = unknownBlockType.getBlock();
    if (block.canTake(pcValue)) {

    }
    Block split1 = block.split(pcValue-1, "", UnknownBlockType.class);
    int instructionLength = getInstructionLength();
    Block split = split1.split(pcValue + instructionLength - 1, "", UnknownBlockType.class);

    split1.setType(new CodeBlockType());
    split1.accept(this);
  }

  private int getInstructionLength() {
    return instruction.getLength();
  }

}
