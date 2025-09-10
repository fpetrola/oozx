package model.tests.display;

import com.fpetrola.oozx.speccy.modules.display.DirtyCells;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What still has to be put on the canvas. A frame plots what changed and not the whole screen, so
 * the marks a write leaves have to survive until the plotting takes them, and taking one must not
 * take its neighbours.
 */
class DirtyCellsTest {
  private final DirtyCells dirty = new DirtyCells(192);

  @Test
  void everythingIsToBePlottedToStartWith() {
    assertEquals(0xffffffff, dirty.between(0, 0, 32), "a screen with nothing on it yet");
  }

  @Test
  void aMarkedCellStaysMarkedUntilItIsPlotted() {
    dirty.plotted(10, 0xffffffff);
    assertEquals(0, dirty.between(10, 0, 32));

    dirty.cell(5, 10);
    assertEquals(1 << 5, dirty.between(10, 0, 32));
    assertEquals(1 << 5, dirty.between(10, 0, 32), "asking does not take it");

    dirty.plotted(10, 1 << 5);
    assertEquals(0, dirty.between(10, 0, 32));
  }

  @Test
  void plottingPartOfARowLeavesTheRest() {
    dirty.plotted(3, 0xffffffff);
    dirty.cell(2, 3);
    dirty.cell(20, 3);

    assertEquals(1 << 2, dirty.between(3, 0, 8), "only what falls between the columns asked for");
    dirty.plotted(3, 1 << 2);
    assertEquals(1 << 20, dirty.between(3, 0, 32), "and the other one is still waiting");
  }

  @Test
  void rowsDoNotSpillIntoEachOther() {
    dirty.plotted(7, 0xffffffff);
    dirty.plotted(8, 0xffffffff);
    dirty.cell(1, 7);

    assertEquals(1 << 1, dirty.between(7, 0, 32));
    assertEquals(0, dirty.between(8, 0, 32));
  }

  /**
   * The last column of a row, and a range that reaches past it.
   * <p>
   * Both ends of the row are where a mask built by shifting goes wrong: the last column is the
   * sign bit of the int a row is kept in, and a range of the whole width asks for a shift of
   * thirty-two, which in int arithmetic is a shift of nothing and would leave the mask empty.
   */
  @Test
  void theLastColumnOfARowIsAColumnLikeTheRest() {
    dirty.plotted(4, 0xffffffff);
    dirty.cell(31, 4);

    assertEquals(1 << 31, dirty.between(4, 0, 32), "the whole width should reach the last column");
    assertEquals(1 << 31, dirty.between(4, 31, 32), "and so should a range of just that column");
    assertEquals(0, dirty.between(4, 0, 31), "a range stopping short of it should not");

    dirty.cell(30, 4);
    assertEquals(1 << 30, dirty.between(4, 0, 31), "its neighbour is its own cell");

    dirty.plotted(4, 1 << 31);
    assertEquals(1 << 30, dirty.between(4, 0, 32), "plotting the last one leaves the one below it");
  }
}
