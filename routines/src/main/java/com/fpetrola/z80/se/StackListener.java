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

package com.fpetrola.z80.se;

import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.opcodes.references.WordNumber;

public interface StackListener {
  boolean returnAddressPopped(int pcValue, int returnAddress, int callAddress);

  default boolean jumpUsingRet(int pcValue, int jumpAddress) {
    System.out.println("jumpUsingRet: %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(jumpAddress)));
    return false;
  }

  default boolean simulatedCall(int pcValue, int i) {
    System.out.println("simulatedCall: %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(i)));
    return false;
  }

  default boolean beginUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
    System.out.println("beginUsingStackAsRepository: %s %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(newSpAddress), Helper.formatAddress(oldSpAddress)));
    return false;
  }

  default boolean endUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
    System.out.println("endUsingStackAsRepository: %s %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(newSpAddress), Helper.formatAddress(oldSpAddress)));
    return false;
  }
}
