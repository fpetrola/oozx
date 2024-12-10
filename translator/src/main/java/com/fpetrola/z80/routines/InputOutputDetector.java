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

package com.fpetrola.z80.routines;import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.transformations.Virtual8BitsRegister;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;
import com.fpetrola.z80.transformations.VirtualRegister;

public class InputOutputDetector {
  public static void detectInputAndOutput(final Routine routine, Instruction instruction) {
    instruction.accept(new RegisterFinderInstructionVisitor() {
      public boolean visitRegister(Register register) {
        if (register instanceof VirtualRegister<?> virtualRegister) {
          addParameter(routine, virtualRegister);
          addReturnValue(routine, virtualRegister);
//        addReturnValues(virtualRegister);
//        addParameters(virtualRegister);
        }
        return super.visitRegister(register);
      }

      private void addParameters(VirtualRegister<?> virtualRegister) {
        boolean isParameter = virtualRegister.getPreviousVersions().stream().anyMatch(previous -> routine.routineManager.findRoutineAt(previous.getRegisterLine()) != routine);
        if (isParameter)
          addParameter(routine, virtualRegister);
      }

      private void addReturnValues(VirtualRegister<?> virtualRegister) {
        boolean isReturnValue = virtualRegister.getDependants().stream().anyMatch(dependantRegister -> {
          boolean[] isReturnValue2 = new boolean[]{false};
          if (routine.routineManager.findRoutineAt(dependantRegister.getRegisterLine()) != routine.routineManager.findRoutineAt(virtualRegister.getRegisterLine())) {
            if (dependantRegister instanceof Virtual8BitsRegister<?> dependantVirtual8BitsRegister) {
              checkReturn(routine, virtualRegister, dependantVirtual8BitsRegister, isReturnValue2);
            } else if (dependantRegister instanceof VirtualComposed16BitRegister<?> virtualComposed16BitRegister) {
              checkReturn(routine, (VirtualRegister<?>) virtualRegister, (Virtual8BitsRegister<?>) virtualComposed16BitRegister.getHigh(), isReturnValue2);
              checkReturn(routine, (VirtualRegister<?>) virtualRegister, (Virtual8BitsRegister<?>) virtualComposed16BitRegister.getLow(), isReturnValue2);
            } else
              System.out.println();
          }
          return isReturnValue2[0];
        });
        if (isReturnValue)
          addReturnValue(routine, virtualRegister);
      }
    });
  }

  private static void checkReturn(final Routine routine, VirtualRegister<?> virtualRegister, Virtual8BitsRegister<?> dependantVirtual8BitsRegister, boolean[] isReturnValue2) {
    Instruction<?> instruction = dependantVirtual8BitsRegister.instruction;
    instruction.accept(new RegisterFinderInstructionVisitor() {
      public boolean visitRegister(Register register) {
        boolean sameInitial = false;
        if (isSource) {
          sameInitial = getFirstVersion(virtualRegister) == getFirstVersion((VirtualRegister) register);
          if (sameInitial) {
            isReturnValue2[0] = sameInitial;
          }
        }
        return sameInitial;
      }
    });
  }

  private static VirtualRegister getFirstVersion(VirtualRegister virtualRegister) {
    return (VirtualRegister) virtualRegister.getVersionHandler().versions.getFirst();
  }

  private static void addParameter(Routine routine, VirtualRegister register) {
    routine.parameters.add(getFirstVersion(register).getName());
  }

  private static void addReturnValue(Routine routine, VirtualRegister register) {
    routine.returnValues.add(getFirstVersion(register).getName());
  }
}
