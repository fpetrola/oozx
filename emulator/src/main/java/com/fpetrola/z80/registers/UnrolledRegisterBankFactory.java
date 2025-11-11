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

package com.fpetrola.z80.registers;

public class UnrolledRegisterBankFactory {
  public RegisterBank createBank() {
    UnrolledRegisterBank registerBank = new UnrolledRegisterBank();

    registerBank.registerAf = registerBank.new AFRegister(registerBank.new ARegister(), registerBank.new FRegister());
    registerBank.registerBc = registerBank.new BCRegister(registerBank.new BRegister(), registerBank.new CRegister());
    registerBank.registerDe = registerBank.new DERegister(registerBank.new DRegister(), registerBank.new ERegister());
    registerBank.registerHl = registerBank.new HLRegister(registerBank.new HRegister(), registerBank.new LRegister());
    registerBank.register_af = registerBank.new AFxRegister(registerBank.new AxRegister(), registerBank.new FxRegister());
    registerBank.register_bc = registerBank.new BCxRegister(registerBank.new BxRegister(), registerBank.new CxRegister());
    registerBank.register_de = registerBank.new DExRegister(registerBank.new DxRegister(), registerBank.new ExRegister());
    registerBank.register_hl = registerBank.new HLxRegister(registerBank.new HxRegister(), registerBank.new LxRegister());
    registerBank.registerIx = registerBank.new IXRegister(registerBank.new IXHRegister(), registerBank.new IXLRegister());
    registerBank.registerIy = registerBank.new IYRegister(registerBank.new IYHRegister(), registerBank.new IYLRegister());
    registerBank.registerIr = registerBank.new IRRegister(registerBank.new IRegister(), registerBank.new RRegister());
    registerBank.registerPc = registerBank.new PCRegister();
    registerBank.registerSp = registerBank.new SPRegister();
    registerBank.registerMemptr = registerBank.new MEMPTRRegister();
    registerBank.registerVirtual = registerBank.new VirtualRegister();

    return registerBank;
  }
}
