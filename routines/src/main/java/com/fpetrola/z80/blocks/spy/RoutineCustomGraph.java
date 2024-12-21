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

package com.fpetrola.z80.blocks.spy;

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.blocks.BlockChangesListener;
import com.fpetrola.z80.blocks.ParentChildChangesListener;
import com.fpetrola.z80.graph.CustomGraph;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RoutineCustomGraph<T> extends CustomGraph {
  public static mxGraph graph;
  private static final Map<Nameable, mxCell> routinesVertices = new HashMap<>();

  public RoutineCustomGraph(mxGraph graph) {
    RoutineCustomGraph.graph = graph;
  }

  public static class GraphBlockChangesListener<T extends Nameable> implements ParentChildChangesListener<T> {

    private int id;

    public void removingKnownBlock(T block, T calledBlock) {
      runOnSwing(() -> {
        log("graph: removing block references: " + block.getName() + " -> " + calledBlock.getName());

        mxCell routineVertex = routinesVertices.get(block);
        mxCell calledRoutineVertex = routinesVertices.get(calledBlock);

        Object[] edgesBetween = graph.getEdgesBetween(routineVertex, calledRoutineVertex);

        for (Object object : edgesBetween) {
          routineVertex.removeEdge((mxICell) object, true);
          routineVertex.removeEdge((mxICell) object, false);
          if (calledRoutineVertex != null) {
            calledRoutineVertex.removeEdge((mxICell) object, true);
            calledRoutineVertex.removeEdge((mxICell) object, false);
          } else {
            log("why?");
          }
        }

        graph.removeCells(edgesBetween);

        verifyVertex(routineVertex);
        verifyVertex(calledRoutineVertex);
      });
    }

    private void verifyVertex(mxCell vertex) {
//      int edgeCount = vertex.getEdgeCount();
//      for (int i = 0; i < edgeCount; i++) {
//        mxICell edgeAt = vertex.getEdgeAt(i);
//
//        mxICell sourceTerminal = edgeAt.getTerminal(true);
//        mxICell targetTerminal = edgeAt.getTerminal(false);
//        if (sourceTerminal == null || targetTerminal == null)
//          System.out.println("guau");
//      }
    }

    public void removingBlock(T block) {
      runOnSwing(() -> {

        log("graph: removing block: " + block.getName());

        mxCell routineVertex = routinesVertices.get(block);

        Object[] edges1 = graph.getEdges(routineVertex);

        for (Object object : edges1) {

          mxICell mxICell = (mxICell) object;
          mxICell terminal1 = mxICell.getTerminal(true);
          mxICell terminal2 = mxICell.getTerminal(false);
          terminal1.removeEdge(mxICell, true);
          terminal1.removeEdge(mxICell, false);
          terminal2.removeEdge(mxICell, true);
          terminal2.removeEdge(mxICell, false);
        }
        graph.removeCells(edges1);
        graph.removeCells(new mxCell[]{routineVertex});
        routinesVertices.remove(block);
      });
    }

    private static void runOnSwing(Runnable runnable) {
      try {
//        runnable.run();
        SwingUtilities.invokeLater(runnable);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void addingKnownBLock(T block, T calledBlock, int from) {
      runOnSwing(() -> {
        log("graph: adding block references: " + block.getName() + " -> " + calledBlock.getName());
        mxCell routineVertex = routinesVertices.get(block);
        mxCell calledRoutineVertex = routinesVertices.get(calledBlock);
//        String callType = executionStepData.instructionToString.contains("Call") ? "CALL" : "JUMP";

        Object[] edgesBetween = graph.getEdgesBetween(routineVertex, calledRoutineVertex);
        if (edgesBetween.length == 0) {
          String style = "edgeStyle=sideToSideEdgeStyle;elbow=vertical;orthogonal=0;";
          graph.insertEdge(graph.getDefaultParent(), id++ + "", calledBlock.getName(), routineVertex, calledRoutineVertex, style);
          if (calledRoutineVertex == null) log("why?");
        }

        verifyVertex(routineVertex);
        verifyVertex(calledRoutineVertex);
      });
    }

    public void addingBlock(T block) {
      runOnSwing(() -> {
        mxCell routineVertex = routinesVertices.get(block);
        if (routineVertex == null) {
          log("graph: adding block: " + block.getName());
          mxCell newRoutineVertex = (mxCell) graph.insertVertex(graph.getDefaultParent(), id++ + "", block.getName(), 50, 50, 200, 50);
          routinesVertices.put(block, newRoutineVertex);
        } else
          log("trying to add a block twice: " + block.getName());
      });
    }

    private static void log(String block) {
      // System.out.println(block);
    }

    public void blockChanged(T block) {
      runOnSwing(() -> {
        log("graph: changed block: " + block.getName());
        mxCell routineVertex = routinesVertices.get(block);
        StringBuffer stringBuffer = new StringBuffer();
        if (routineVertex != null)
          routineVertex.setValue(block.getName());
      });
    }

    @Override
    public void replaceBlock(T oldBlock, T newBlock) {
      runOnSwing(() -> {
        log("graph: replace block: " + oldBlock.getName() + " by: " + newBlock.getName());
        mxCell mxCell = routinesVertices.get(oldBlock);
        routinesVertices.put(newBlock, mxCell);
//      removingBlock(oldBlock);
//      routinesVertices.remove(oldBlock);
      });

    }
  }

  protected String getVertexLabel(Object object) {
    if (object instanceof mxCell mxCell) {
      return mxCell.getValue().toString();
    } else return object + "";
  }

  protected String getVertexId(Object object) {
    if (object instanceof mxCell mxCell) {
      return mxCell.getId();
    } else return getVertexLabel(object);
  }

  public CustomGraph convertGraph() {

    Set<Map.Entry<Nameable, mxCell>> entrySet = routinesVertices.entrySet();

    for (Map.Entry<Nameable, mxCell> entry : entrySet) {
      mxCell vertex = entry.getValue();

      System.out.println(vertex.getValue());
      String name = entry.getKey().getName();
      addVertex(vertex.getId(), name);
      int edgeCount = vertex.getEdgeCount();
      for (int i = 0; i < edgeCount; i++) {
        mxICell edgeAt = vertex.getEdgeAt(i);

        mxICell sourceTerminal = edgeAt.getTerminal(true);
        mxICell targetTerminal = edgeAt.getTerminal(false);
        if (sourceTerminal == null || targetTerminal == null) System.out.println("guau");
        addEdge(sourceTerminal, targetTerminal, entry.getKey().getName());
      }
    }

    return this;
  }
}
