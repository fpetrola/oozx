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

package com.fpetrola.z80.ide;

import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.spy.RoutineCustomGraph;
import com.fpetrola.z80.blocks.spy.RoutineGrouperSpy;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.graph.GraphFrame;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.minizx.emulation.Z80EmulatorBridge;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineFinder;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.DataflowService;
import com.fpetrola.z80.spy.ObservableRegister;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;

import javax.swing.*;

public class ZXIde {
  public static void main(String[] args) {
    LafManager.install(new DarculaTheme());

    Helper.hex = true;

    GraphFrame frame = new GraphFrame();
//    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//    frame.setSize(1000, 700);
//    frame.setVisible(true);
    DataflowService dataflowService = new DataflowService() {
    };

    RoutineCustomGraph.graph = frame.graph;

    RoutineCustomGraph.GraphBlockChangesListener blockChangesListener = new RoutineCustomGraph.GraphBlockChangesListener();
    BlocksManager blocksManager = new BlocksManager(blockChangesListener, true);
    Z80Debugger.blockManager = blocksManager;

    State state = EmulatedMiniZX.createState();
    RoutineManager routineManager = new RoutineManager(blocksManager);
    RoutineFinder routineFinder = new RoutineFinder(routineManager, new StackAnalyzer<>(state), state);
    RoutineGrouperSpy spy = new RoutineGrouperSpy<>(frame.graph, dataflowService, routineFinder);

    spy.enable(true);

    String url = "file:///home/fernando/dynamitedan1.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/tge.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/rickdangerous";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/wally.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/dynamitedan";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/jsw.z80";


    new EmulatedMiniZX((ooz80, emulateUntil, pause) -> {
      ObservableRegister<?> pc = (ObservableRegister<?>) ooz80.getState().getPc();
      Z80EmulatorBridge emulator1 = new Z80EmulatorBridge(pc, ooz80, emulateUntil, pause, routineManager);
      routineManager.setRoutineHandlingListener(emulator1.getRoutineHandlingListener());
      SwingUtilities.invokeLater(() -> Z80Debugger.createAndShowGUI(emulator1, emulator1.getTreeListener()));
      ooz80.getInstructionFetcher().addFetchListener(emulator1.getRegisterWriteListener());
    }, url, 10, true, -1, true, spy, state).start();
  }
}
