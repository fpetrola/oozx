package model.tests.display;

import com.fpetrola.oozx.speccy.modules.display.Picture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The canvas, and the one thing that goes on it: eight pixels of a cell, with the highest bit of
 * the byte on the left. From prototypes/tdd/display, where it is the third fact.
 */
class PictureTest {
  private final Picture canvas = new Picture();

  private int at(int x, int y) {
    return canvas.pixels[y * Picture.WIDTH + x];
  }

  /**
   * The byte has to be one that is not the same backwards, or reversing the bits would agree with
   * itself: 0b11000000 is the two leftmost pixels and nothing else.
   */
  @Test
  void theHighestBitOfAByteIsTheLeftmostPixel() {
    canvas.plot8(3, 40, (byte) 0b11000000, (byte) 7, (byte) 0);
    int left = 3 * 8;

    assertEquals(Picture.PALETTE[7], at(left, 40), "bit 7 is set, so the leftmost pixel takes the ink");
    assertEquals(Picture.PALETTE[7], at(left + 1, 40), "and bit 6 the one beside it");
    assertEquals(Picture.PALETTE[0], at(left + 2, 40), "the rest is paper");
    assertEquals(Picture.PALETTE[0], at(left + 7, 40), "including the rightmost, which is bit 0");
  }

  @Test
  void aCellIsEightPixelsWideAndTouchesNoOther() {
    canvas.plot8(3, 40, (byte) 0xff, (byte) 7, (byte) 0);

    assertEquals(0, at(3 * 8 - 1, 40), "the cell to the left is untouched");
    assertEquals(0, at(4 * 8, 40), "and the one to the right");
    assertEquals(0, at(3 * 8, 41), "and the line below");
  }

  /** Bright is the top half of the palette, and the dim colours are not the same as the bright ones. */
  @Test
  void thePaletteHasEachColourDimAndBright() {
    assertNotEquals(Picture.PALETTE[7], Picture.PALETTE[15], "white and bright white differ");
    assertEquals(0, Picture.PALETTE[0], "black is black either way");
    assertEquals(0, Picture.PALETTE[8]);
  }

  @Test
  void anInactiveCanvasIsNotPlottedOn() {
    canvas.active = false;
    canvas.plot8(3, 40, (byte) 0xff, (byte) 7, (byte) 0);
    assertEquals(0, at(3 * 8, 40));
  }
}
