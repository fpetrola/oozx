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

package com.fpetrola.z80.graph;

import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxImageCanvas;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Map;

public class GraphFrame extends JFrame {
//  private final class mxCoordinateAssignmentExtension extends mxCoordinateAssignment {
//    private mxCoordinateAssignmentExtension(mxHierarchicalLayout layout, double intraCellSpacing, double interRankCellSpacing, int orientation, double initialX, double parallelEdgeSpacing) {
//      super(layout, intraCellSpacing, interRankCellSpacing, orientation, initialX, parallelEdgeSpacing);
//    }
//
//    protected void localEdgeProcessing(mxGraphHierarchyModel model) {
//    }
//
//    protected void setEdgePosition(mxGraphAbstractHierarchyCell cell) {
//    }
//  }

  public mxGraph graph = new mxGraph() {

    public void drawState(mxICanvas canvas, mxCellState state, boolean drawLabel) {
      String label = (drawLabel) ? state.getLabel() : "";

      // Indirection for wrapped swing canvas inside image canvas (used for creating
      // the preview image when cells are dragged)
      Object cell = state.getCell();
      if (getModel().isVertex(cell) && canvas instanceof mxImageCanvas && ((mxImageCanvas) canvas).getGraphicsCanvas() instanceof SwingCanvas) {
        ((SwingCanvas) ((mxImageCanvas) canvas).getGraphicsCanvas()).drawVertex(state, label);
      }
      // Redirection of drawing vertices in SwingCanvas
      else if (getModel().isVertex(cell) && canvas instanceof SwingCanvas) {
        ((SwingCanvas) canvas).drawVertex(state, label);
      } else {
        super.drawState(canvas, state, drawLabel);
      }
    }
  };
  private final mxGraphComponent graphComponent;
  private int id;
  public boolean ready;

  public static void main(String[] args) {
    JFrame frame = new GraphFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1000, 700);
    frame.setVisible(true);
  }

  public GraphFrame() {
    Map<String, Object> style = graph.getStylesheet().getDefaultEdgeStyle();
    style.put(mxConstants.STYLE_ROUNDED, true);
    style.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ELBOW);

    graphComponent = new mxGraphComponent(graph) {
      public mxInteractiveCanvas createCanvas() {
        return new SwingCanvas(this);
      }
    };

    MouseAdapter mouseAdapter = new MouseAdapter() {
      private mxPoint start;

      public void mouseClicked(MouseEvent e) {
        ready = true;
        if (e.isControlDown())
          morphGraph();
      }

      public void mousePressed(MouseEvent e) {
        if (!e.isConsumed()) {
          mxPoint translate = graph.getView().getTranslate();

          double pX = (e.getX() / graph.getView().getScale()) - translate.getX();
          double pY = (e.getY() / graph.getView().getScale()) - translate.getY();

          start = new mxPoint(pX, pY);
        }
      }

      public void mouseDragged(MouseEvent e) {

        if (start != null) {
          double pX = (e.getX() / graph.getView().getScale()) - start.getX();
          double pY = (e.getY() / graph.getView().getScale()) - start.getY();

//          System.out.println(start.getX() + " - " + start.getY());

          graph.getView().setTranslate(new mxPoint(pX, pY));
          e.consume();
        }
      }

      public void mouseReleased(MouseEvent e) {
        start = null;
      }
    };
    graphComponent.getGraphControl().addMouseListener(mouseAdapter);
    graphComponent.getGraphControl().addMouseMotionListener(mouseAdapter);

    graphComponent.getGraphControl().addMouseWheelListener(new MouseAdapter() {
      public void zoom(double factor) {
        mxGraphView view = graph.getView();
        double newScale = (double) ((int) (view.getScale() * 100 * factor)) / 100;

        if (newScale != view.getScale() && newScale > 0.04) {
          mxPoint translate = new mxPoint();
          graph.getView().scaleAndTranslate(newScale, translate.getX(), translate.getY());

        }
      }

      public void mouseWheelMoved(MouseWheelEvent evt) {
        if (evt.isConsumed()) {
          return;
        }

        boolean gridEnabled = graph.isGridEnabled();

        // disable snapping
        graph.setGridEnabled(false);

        mxPoint p1 = graphComponent.getPointForEvent(evt, false);

        if (evt.getWheelRotation() < 0) {
          zoom(1.2);
        } else {
          zoom(1 / 1.2);
        }

        mxPoint p2 = graphComponent.getPointForEvent(evt, false);
        double deltaX = p2.getX() - p1.getX();
        double deltaY = p2.getY() - p1.getY();

        mxGraphView view = graph.getView();

        mxPoint translate = view.getTranslate();
        view.setTranslate(new mxPoint(translate.getX() + deltaX, translate.getY() + deltaY));

//        graph.setGridEnabled(gridEnabled);

        evt.consume();
//        zoomWithWheel(evt);
      }
    });
    Object defaultParent = graph.getDefaultParent();
    JScrollPane jScrollPane = new JScrollPane(graphComponent);
    jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    getContentPane().add(graphComponent);
    morphGraph();
    graphComponent.zoomOut();
  }

  public void morphGraph() {
//    mxIGraphLayout layout = new mxOrganicLayout(graph);
    mxHierarchicalLayout layout = new mxHierarchicalLayout(graph) {
//      public double placementStage(double initialX, Object parent) {
//        mxCoordinateAssignment placementStage = new mxCoordinateAssignmentExtension(this, intraCellSpacing, interRankCellSpacing, orientation, initialX, parallelEdgeSpacing);
//        placementStage.setFineTuning(fineTuning);
//        placementStage.execute(parent);
//
//        return placementStage.getLimitX() + interHierarchySpacing;
//      }
    };
    layout.setIntraCellSpacing(130.0);
    layout.setInterRankCellSpacing(130.0);
//    layout.setDisableEdgeStyle(false);
    // mxIGraphLayout layout = new mxCircleLayout(graph);
    mxIGraphLayout layout2 = new mxOrthogonalLayout(graph);

//    mxParallelEdgeLayout layout2 = new mxParallelEdgeLayout(graph);

    graph.getModel().beginUpdate();
    layout.execute(graph.getDefaultParent());
//      layout2.execute(graph.getDefaultParent());
    graph.getModel().endUpdate();

//    graphComponent.zoomAndCenter();
  }

}
