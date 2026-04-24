/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.devices.debugger;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * The machine's memory read as sprites: a byte is eight pixels, a sprite is so many bytes across
 * and so many rows down, and they are laid out from an address onwards.
 * <p>
 * The panning, the zoom that keeps what is under the pointer under the pointer, and the pixel
 * grid come from a sprite viewer written in a branch, which drew sprites of random colours
 * because it had no machine to read. This one has one.
 */
public class SpriteViewerInternalFrame extends MachineFrame {

  private static final int REFRESH_MILLIS = 200;

  private final JTextField address = new JTextField("4000", 5);
  private final JSpinner across = new JSpinner(new SpinnerNumberModel(2, 1, 32, 1));
  private final JSpinner down = new JSpinner(new SpinnerNumberModel(16, 1, 192, 1));
  private final JSpinner many = new JSpinner(new SpinnerNumberModel(16, 1, 256, 1));
  private final JToggleButton grid = new JToggleButton("Grid");
  private final Canvas canvas = new Canvas();

  public SpriteViewerInternalFrame() {
    super("Sprites");

    address.setToolTipText("Where the first sprite starts, in hex. 4000 is the screen.");
    across.setToolTipText("Bytes across: eight pixels each");
    down.setToolTipText("Rows down");
    many.setToolTipText("How many, one after another");
    grid.addActionListener(e -> canvas.repaint());
    JButton screen = new JButton("Screen");
    screen.setToolTipText("Back to the screen's own memory");
    screen.addActionListener(e -> address.setText("4000"));

    controls.add(new JLabel("At:"));
    controls.add(address);
    controls.add(new JLabel("across:"));
    controls.add(across);
    controls.add(new JLabel("down:"));
    controls.add(down);
    controls.add(new JLabel("of them:"));
    controls.add(many);
    controls.add(grid);
    controls.add(screen);

    JPanel body = new JPanel(new BorderLayout());
    body.add(canvas, BorderLayout.CENTER);
    assemble(body);
    setCompact(false);
    setSize(640, 480);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> canvas.repaint());
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
  }

  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    canvas.repaint();
  }

  private int at() {
    try {
      return Integer.parseInt(address.getText().trim(), 16) & 0xffff;
    } catch (NumberFormatException notAnAddress) {
      return 0x4000;
    }
  }

  private int number(JSpinner spinner) {
    return (Integer) spinner.getValue();
  }

  /** Ported from the branch: drag to pan, wheel to zoom about the pointer, a grid of pixels. */
  private class Canvas extends JPanel {

    private double zoom = 4;
    private double offsetX;
    private double offsetY;

    Canvas() {
      setPreferredSize(new Dimension(600, 400));
      setBackground(Color.DARK_GRAY);
      MouseAdapter hand = new MouseAdapter() {
        private int lastX;
        private int lastY;

        public void mousePressed(MouseEvent e) {
          lastX = e.getX();
          lastY = e.getY();
        }

        public void mouseDragged(MouseEvent e) {
          offsetX += (e.getX() - lastX) / zoom;
          offsetY += (e.getY() - lastY) / zoom;
          lastX = e.getX();
          lastY = e.getY();
          repaint();
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
          double was = zoom;
          zoom = Math.max(1, Math.min(32, zoom * (e.getPreciseWheelRotation() < 0 ? 1.1 : 0.9)));
          // What is under the pointer stays under the pointer.
          offsetX = e.getX() / zoom - (e.getX() / was - offsetX);
          offsetY = e.getY() / zoom - (e.getY() / was - offsetY);
          repaint();
        }
      };
      addMouseListener(hand);
      addMouseMotionListener(hand);
      addMouseWheelListener(hand);
    }

    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Speccy machine = machine();
      if (machine == null) {
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("clip me to a machine", 12, 20);
        return;
      }
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.translate(offsetX * zoom, offsetY * zoom);
      g2d.scale(zoom, zoom);

      int wide = number(across);
      int tall = number(down);
      int count = number(many);
      int perRow = Math.max(1, (int) (getWidth() / zoom) / (wide * 8 + 2));
      int from = at();

      g2d.setColor(Color.WHITE);
      for (int sprite = 0; sprite < count; sprite++) {
        int left = sprite % perRow * (wide * 8 + 2);
        int top = sprite / perRow * (tall + 2);
        for (int row = 0; row < tall; row++) {
          for (int column = 0; column < wide; column++) {
            int data = machine.memory.peek(from + sprite * wide * tall + row * wide + column) & 0xff;
            for (int bit = 0; bit < 8; bit++) {
              if ((data & 0x80 >> bit) != 0) {
                g2d.fillRect(left + column * 8 + bit, top + row, 1, 1);
              }
            }
          }
        }
      }

      if (grid.isSelected() && zoom >= 4) {
        g2d.setColor(new Color(0x40, 0x40, 0x40));
        int width = (int) (getWidth() / zoom);
        int height = (int) (getHeight() / zoom);
        for (int x = 0; x <= width; x++) {
          g2d.drawLine(x, 0, x, height);
        }
        for (int y = 0; y <= height; y++) {
          g2d.drawLine(0, y, width, y);
        }
      }
      g2d.dispose();
    }
  }

  @Override
  protected String expandTip() {
    return "Show the whole sheet of sprites";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto a machine's window, which is the memory it reads";
  }
}
