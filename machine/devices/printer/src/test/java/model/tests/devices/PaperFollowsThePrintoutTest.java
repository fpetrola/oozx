/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package model.tests.devices;

import com.fpetrola.oozx.speccy.devices.printer.PrinterPaper;
import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The window follows the paper as it comes out, the way you watch a printer print.
 * <p>
 * It scrolled to the bottom of the panel as the panel was before the row arrived, so it stayed one
 * row short and then stopped following at all: a scroll asked for before the panel has been laid
 * out at its new height is quietly clamped to where the bottom used to be.
 */
class PaperFollowsThePrintoutTest {

  private static final int TICKS_PER_DOT = 220;

  @BeforeEach
  void aWindowNeedsSomewhereToOpen() {
    assumeFalse(GraphicsEnvironment.isHeadless(), "the paper is shown in a window, and there is no screen here");
  }

  private void print(ZxPrinter printer, long[] now, int lines) {
    for (int line = 0; line < lines; line++) {
      printer.write((byte) 0x80);
      now[0] += (long) (64 + 256) * TICKS_PER_DOT;
      printer.write((byte) 0x80);
    }
  }

  @Test
  void theViewFollowsThePaperOut() throws Exception {
    Printout paper = new Printout();
    long[] now = {0};
    ZxPrinter printer = new ZxPrinter(paper, () -> now[0], () -> 69888);

    PrinterPaper view = new PrinterPaper(paper);
    JScrollPane scroll = new JScrollPane(view);
    scroll.setPreferredSize(new Dimension(300, 240));
    JFrame window = new JFrame();
    window.getContentPane().add(scroll);
    window.pack();

    print(printer, now, 120);
    SwingUtilities.invokeAndWait(() -> { });

    Rectangle shown = view.getVisibleRect();
    int paperBottom = view.getPreferredSize().height;
    assertTrue(shown.y + shown.height >= paperBottom - 40,
        "the view stayed at " + (shown.y + shown.height) + " while the paper reached " + paperBottom);
    window.dispose();
  }

  /** Unless somebody is reading further up, in which case dragging the view about is rude. */
  @Test
  void itLeavesTheViewAloneWhenSomebodyHasScrolledUp() throws Exception {
    Printout paper = new Printout();
    long[] now = {0};
    ZxPrinter printer = new ZxPrinter(paper, () -> now[0], () -> 69888);

    PrinterPaper view = new PrinterPaper(paper);
    JScrollPane scroll = new JScrollPane(view);
    scroll.setPreferredSize(new Dimension(300, 240));
    JFrame window = new JFrame();
    window.getContentPane().add(scroll);
    window.pack();

    print(printer, now, 120);
    SwingUtilities.invokeAndWait(() -> { });

    view.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    SwingUtilities.invokeAndWait(() -> { });
    int readingAt = view.getVisibleRect().y;

    print(printer, now, 20);
    SwingUtilities.invokeAndWait(() -> { });

    assertTrue(view.getVisibleRect().y - readingAt < 40,
        "the view was dragged down to " + view.getVisibleRect().y + " while somebody was reading at " + readingAt);

    // And scrolling back to the end takes up watching again, which is the other half of the rule.
    view.scrollRectToVisible(new Rectangle(0, view.getPreferredSize().height - 1, 1, 1));
    SwingUtilities.invokeAndWait(() -> { });
    int wasAt = view.getVisibleRect().y;

    print(printer, now, 20);
    SwingUtilities.invokeAndWait(() -> { });

    assertTrue(view.getVisibleRect().y > wasAt,
        "back at the end and it stopped following: still at " + view.getVisibleRect().y);
    window.dispose();
  }
}
