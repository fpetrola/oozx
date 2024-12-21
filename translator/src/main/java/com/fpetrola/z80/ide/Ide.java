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
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.routines.RoutineFinder;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.DataflowService;

import javax.swing.*;

public class Ide {
  public static void main(String[] args) {

    GraphFrame frame = new GraphFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1000, 700);
    frame.setVisible(true);
    State state = EmulatedMiniZX.createState();
    DataflowService dataflowService = new DataflowService() {
    };

    RoutineCustomGraph.graph = frame.graph;

    RoutineCustomGraph.GraphBlockChangesListener blockChangesListener = new RoutineCustomGraph.GraphBlockChangesListener();
    BlocksManager blocksManager = new BlocksManager(blockChangesListener, true);
    RoutineFinder routineFinder = new RoutineFinder(new RoutineManager(blocksManager));
    RoutineGrouperSpy spy = new RoutineGrouperSpy<>(frame, dataflowService, routineFinder);

    spy.enable(true);

    String url = "file:///home/fernando/dynamitedan1.z80";
    url= "file:///home/fernando/detodo/desarrollo/m/zx/roms/dynamitedan";
    new EmulatedMiniZX(url, 1000, true, -1, true, spy, state).start();
  }
}
