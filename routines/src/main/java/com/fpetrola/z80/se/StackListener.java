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

import java.util.List;
import java.util.Set;

public interface StackListener {
  default boolean returnAddressPopped(int pcValue, int returnAddress, int callAddress) {
    System.out.println("returnAddressPopped: %s %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(returnAddress), Helper.formatAddress(callAddress)));
    return false;
  }

  default boolean jumpUsingRet(int pcValue, Set<Integer> jumpAddresses) {
    System.out.println("jumpUsingRet: %s %s".formatted(Helper.formatAddress(pcValue), jumpAddresses));
    return false;
  }

  default boolean simulatedCall(int pcValue, int jumpAddress, Set<Integer> jumpAddresses, int returnAddress) {
    System.out.println("simulatedCall: %s %s %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(jumpAddress), formatHex(jumpAddresses), Helper.formatAddress(returnAddress)));
    return false;
  }

  private List<String> formatHex(Set<Integer> jumpAddresses) {
    return jumpAddresses.stream().map(i-> Helper.formatAddress(i)).toList();
  }

  default boolean beginUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
    System.out.println("beginUsingStackAsRepository: %s %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(newSpAddress), Helper.formatAddress(oldSpAddress)));
    return false;
  }

  default boolean endUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
    System.out.println("endUsingStackAsRepository: %s %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(newSpAddress), Helper.formatAddress(oldSpAddress)));
    return false;
  }

  default boolean droppingReturnValues(int pcValue, int newSpAddress, int oldSpAddress, ReturnAddressWordNumber lastReturnAddress) {
    System.out.println("droppingReturnValues: %s %s %s %s".formatted(Helper.formatAddress(pcValue), Helper.formatAddress(newSpAddress), Helper.formatAddress(oldSpAddress), lastReturnAddress));
    return false;
  }
}
