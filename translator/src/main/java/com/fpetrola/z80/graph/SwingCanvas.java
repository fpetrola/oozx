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

import com.fpetrola.z80.ide.Z80Debugger;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.view.mxCellState;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class SwingCanvas extends mxInteractiveCanvas {
  protected CellRendererPane rendererPane = new CellRendererPane();

  protected JLabel vertexRenderer = new JLabel();

  protected mxGraphComponent graphComponent;
  private int i;
  private double lastScale;

  public SwingCanvas(mxGraphComponent graphComponent) {
    this.graphComponent = graphComponent;

    initComponent(vertexRenderer);
  }

  private void initComponent(JComponent vertexRenderer1) {
    vertexRenderer1.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
//    vertexRenderer1.setHorizontalAlignment(JLabel.CENTER);
    vertexRenderer1.setBackground(graphComponent.getBackground().brighter());
    vertexRenderer1.setOpaque(true);
  }

  public void drawVertex(mxCellState state, String label, double scale) {
    vertexRenderer.setText(label);

    int x = (int) (state.getX() + translate.getX());
    int y = (int) (state.getY() + translate.getY());
    int width = (int) state.getWidth();

    mxCell cell = (mxCell) state.getCell();
    String value = (String) cell.getValue();
    JComponent mainPanel = Z80Debugger.addBlock(value);
    JPanel component1 = getjPanel(scale);
    component1.add(mainPanel);

    rendererPane.paintComponent(g, component1, graphComponent, x, y, width, (int) state.getHeight(), false);
  }

  private void getTextArea(String label) {
    TextEditorPane textArea = new TextEditorPane();
    textArea.setText(label);
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
    textArea.setCodeFoldingEnabled(true);
  }

  private JPanel getjPanel(double scale) {
    JPanel component1 = new JPanel() {
      public Graphics getGraphics() {
        return g;
      }

      private double zoomFactor = 0.3;
      private boolean zoomer = true;

      public void setBounds(int x, int y, int width, int height) {
        getComponent(0).setBounds(x, y, width, height);
        zoomFactor = (width / 64f + 0.1f) / 5f;
        invalidate2(getjComponent());
      }

      public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (zoomer) {
          AffineTransform at = new AffineTransform();
          double zoomFactor = scale + 0.1f;
          at.scale(zoomFactor, zoomFactor);
          g2.transform(at);
          zoomer = false;
        }

        JComponent component = getjComponent();
        validateTree2(component, scale);
        component.paint(g);

//        getComponent(0).paintAll(g);
      }

      private JComponent getjComponent() {
        JComponent component = (JComponent) getComponent(0);
        return component;
      }

      private void invalidate2(JComponent component) {
        component.putClientProperty("validated2", "false");
      }

      protected static void validateTree2(final JComponent jPanel, double scale) {
        Object validated2 = jPanel.getClientProperty("validated2");
        if (validated2 == null || validated2.equals(scale)) {
          jPanel.putClientProperty("validated2", scale);
          jPanel.doLayout();
          Component[] components = jPanel.getComponents();
          for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component instanceof JComponent) {
              JComponent comp = (JComponent) component;
              validateTree2(comp, scale);
            }
          }
        }
      }

      public boolean isShowing() {
        return true;
      }
    };

    initComponent(component1);
    return component1;
  }

}