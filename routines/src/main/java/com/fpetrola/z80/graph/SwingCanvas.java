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

  public SwingCanvas(mxGraphComponent graphComponent) {
    this.graphComponent = graphComponent;

    vertexRenderer.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    vertexRenderer.setHorizontalAlignment(JLabel.CENTER);
    vertexRenderer.setBackground(graphComponent.getBackground().darker());
    vertexRenderer.setOpaque(true);
  }

  public void drawVertex(mxCellState state, String label) {
    vertexRenderer.setText(label);
    // TODO: Configure other properties..

//    RSyntaxTextAreaHighlighter rSyntaxTextAreaHighlighter = new RSyntaxTextAreaHighlighter();
//    rSyntaxTextAreaHighlighter.install(new RTextArea(label));

    TextEditorPane textArea = new TextEditorPane() {

      public Graphics getGraphics() {
        return g;
      }

      private double zoomFactor = 0.3;
      private double prevZoomFactor = 1;
      private boolean zoomer = true;

      public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        zoomFactor = (width / 300f)+ 0.1f;
      }

      public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (zoomer) {
          AffineTransform at = new AffineTransform();
          at.scale(zoomFactor, zoomFactor);
          prevZoomFactor = zoomFactor;
          g2.transform(at);
          zoomer = false;
        }
        super.paint(g);
      }
    };
    textArea.setText(label);
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
    textArea.setCodeFoldingEnabled(true);
//    RTextScrollPane sp = new RTextScrollPane(textArea);
//    sp.setOpaque(true);

    int x = (int) (state.getX() + translate.getX());
    int y = (int) (state.getY() + translate.getY());
    int width = (int) state.getWidth();

    rendererPane.paintComponent(g, textArea, graphComponent, x, y, width, (int) state.getHeight(), true);
  }

}