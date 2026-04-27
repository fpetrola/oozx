/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package model.tests.devices;

import com.fpetrola.oozx.speccy.devices.printer.PrinterPaper;
import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinter;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The window follows the paper as it comes out, the way you watch a printer print.
 * <p>
 * It scrolled to the bottom of the panel as the panel was before the row arrived, so it stayed one
 * row short and then stopped following at all: a scroll asked for before the panel has been laid
 * out at its new height is quietly clamped to where the bottom used to be.
 */
class PaperFollowsThePrintoutTest {

  private static final int TICKS_PER_DOT = 220;

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
