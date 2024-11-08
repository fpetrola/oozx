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

package z80core;

//import com.fpetrola.z80.spy.ComplexInstructionSpy;
//import snapshots.Z80State;

import snapshots.Z80State;

import java.io.File;

public interface IZ80 {

  void update();

  void enableSpy(boolean b);

  void setINTLine(boolean intLine);

  void execute(int statesLimit);

  void setZ80State(Z80State state);

  Z80State getZ80State();

  void setBreakpoint(int address, boolean state);

  void setPinReset();

  void reset();

  void triggerNMI();

  int getRegPC();

  void xor(int oper8);

  void cp(int oper8);

  void setRegDE(int word);

  int getRegDE();

  void setCarryFlag(boolean b);

  void setFlags(int regF);

  int getFlags();

  int getRegA();

  void setRegA(int b);

  void setRegIX(int word);

  int getRegIX();

  int getRegI();

  void setSpritesArray(boolean[] bitsWritten);
  
  public boolean isExecuting();

  void setLoadedFile(File fileSnapshot);

  Object getSpy();
}