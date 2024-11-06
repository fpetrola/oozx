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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.instructions.Ld;
import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Composed16BitRegister;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class VirtualComposed16BitRegister<T extends WordNumber> extends Composed16BitRegister<T, IVirtual8BitsRegister<T>> implements VirtualRegister<T> {
  private final int currentAddress;
  private Scope scope = new Scope();
  private final VirtualRegisterVersionHandler versionHandler;

  public VirtualComposed16BitRegister(int currentAddress, String virtualRegisterName, IVirtual8BitsRegister<T> virtualH, IVirtual8BitsRegister<T> virtualL, VirtualRegisterVersionHandler versionHandler, boolean composed) {
    super(virtualRegisterName, virtualH, virtualL);
    this.currentAddress = currentAddress;
    this.versionHandler = versionHandler;
    virtualL.set16BitsRegister(this);
    virtualH.set16BitsRegister(this);
    if (composed) {
      virtualL.setComposed(composed);
      virtualH.setComposed(composed);
    }
    scope.include(this);
  }

  @Override
  public List<VirtualRegister<T>> getPreviousVersions() {
    return getVirtualRegisters(low.getPreviousVersions(), high.getPreviousVersions());
  }

  private List<VirtualRegister<T>> getVirtualRegisters(List<VirtualRegister<T>> previousVersionsL, List<VirtualRegister<T>> previousVersionsH) {
    List<VirtualRegister<T>> list = new ArrayList<>();
    for (int i = 0, previousVersionsLSize = previousVersionsL.size(); i < previousVersionsLSize; i++) {
      VirtualRegister<T> pL = previousVersionsL.get(i);
      VirtualRegister<T> pH = previousVersionsH.isEmpty() ? high : previousVersionsH.get(Math.min(i, previousVersionsH.size() - 1));

      String nameL = pL.getName();
      String nameH = pH.getName();
      String lineL = getLineNumber(nameL);
      String lineH = getLineNumber(nameH);

      String finalName = nameH + "," + nameL;
      if (lineL.equals(lineH))
        finalName = nameH.substring(0, nameH.indexOf("_")) + nameL.substring(0, nameL.indexOf("_")) + "_" + lineL;

      finalName = fixIndexNames(finalName);

      list.add(new VirtualComposed16BitRegister<T>(Math.min(pL.getAddress(), pH.getAddress()), finalName, (IVirtual8BitsRegister<T>) pH, (IVirtual8BitsRegister<T>) pL, versionHandler, false));
    }
    return list;
  }

  public static String fixIndexNames(String finalName) {
    return finalName.replace("IXHIXL", "IX").replace("IYHIYL", "IY"); //FIXME
  }

  public static String getLineNumber(String name) {
    return name.substring(name.indexOf("_") + 1);
  }

  @Override
  public boolean usesMultipleVersions() {
    return high.usesMultipleVersions() && low.usesMultipleVersions();
  }

  public void reset() {
    low.reset();
    high.reset();
  }

  public void saveData() {
    low.saveData();
    high.saveData();
  }

  public boolean hasNoPrevious() {
    return getPreviousVersions().isEmpty() || high.hasNoPrevious() && low.hasNoPrevious();
  }

  @Override
  public int getAddress() {
    return Math.min(high.getAddress(), low.getAddress());
  }

  @Override
  public Scope getScope() {
    return scope;
  }

  @Override
  public List<VirtualRegister<T>> getDependants() {
    return getVirtualRegisters(low.getDependants(), high.getDependants());
  }

  @Override
  public void accept(InstructionVisitor instructionVisitor) {
    if (!instructionVisitor.visitVirtualComposed16BitRegister(this)) {
      if (!instructionVisitor.visitRegister(this)) {
        instructionVisitor.visitRegister(getHigh());
        instructionVisitor.visitRegister(getLow());
      }
    }
  }

  public int getRegisterLine() {
    if (getName().contains(","))
      return Math.min(low.getRegisterLine(), high.getRegisterLine());
    else
      return VirtualRegister.super.getRegisterLine();
  }

  @Override
  public VirtualRegisterVersionHandler getVersionHandler() {
    return versionHandler;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    VirtualComposed16BitRegister<?> that = (VirtualComposed16BitRegister<?>) o;

    return new EqualsBuilder().append(toString(), that.toString()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(toString()).toHashCode();
  }

  @Override
  public boolean isInitialized() {
    return isLdTarget(high) || isLdTarget(low);
  }

  private boolean isLdTarget(IVirtual8BitsRegister<T> high1) {
    Instruction instruction = ((Virtual8BitsRegister) high1).instruction;

    if (instruction instanceof Ld ld) {
      if (ld.getTarget().equals(this))
        return true;
    }
    return false;
  }

  public boolean isMixRegister() {
    return low.getRegisterLine() != high.getRegisterLine();
  }
}
