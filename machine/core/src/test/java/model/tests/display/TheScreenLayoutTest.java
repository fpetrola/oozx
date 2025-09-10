package model.tests.display;

import com.fpetrola.oozx.speccy.modules.display.ScreenLayout;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the picture is in the bank that holds it. The bitmap is not in the order it is read in: a
 * line's offset carries the third first, then which line of a character row, then which character
 * row, so the eight lines of one row are 256 bytes apart and the row below starts 32 bytes on.
 * <p>
 * These are offsets and not addresses: the beam reads the bank that is the screen, which on a 128
 * can be bank 7 while bank 5 is what sits at 0x4000. Said in numbers rather than by repeating the
 * formula, which would only agree with itself. From prototypes/tdd/display, and asked of the
 * layout on its own - it needs no machine, which is the point of it being its own object.
 */
class TheScreenLayoutTest {
  private final ScreenLayout layout = new ScreenLayout();

  @Test
  void aPixelLineIsWhereTheThirdAndTheCharacterRowPutIt() {
    assertEquals(0x0000, layout.lineStart[0], "the first line is the start of the bank");
    assertEquals(0x0100, layout.lineStart[1], "the next line down is 256 bytes on, not 32");
    assertEquals(0x0700, layout.lineStart[7], "the last line of the first character row");
    assertEquals(0x0020, layout.lineStart[8], "and the second character row starts 32 bytes in");
    assertEquals(0x0800, layout.lineStart[64], "the second third");
    assertEquals(0x1000, layout.lineStart[128], "and the third");
    assertEquals(0x17e0, layout.lineStart[191], "the very last line");
  }

  @Test
  void oneAttributeCoversACellOfEightByEight() {
    assertEquals(0x1800, layout.attrStart[0], "the attributes follow the 6144 bytes of bitmap");
    assertEquals(0x1800, layout.attrStart[7], "the eight lines of a character row share them");
    assertEquals(0x1820, layout.attrStart[8], "and the row below starts a new lot");
    assertEquals(0x1ae0, layout.attrStart[191], "the last row of cells");
  }

  /** 192 lines of 32 bytes with none left over and none shared: the layout is a permutation. */
  @Test
  void theLinesFillTheBitmapExactlyOnce() {
    Set<Integer> seen = new HashSet<>();
    for (int y = 0; y < 192; y++) {
      assertTrue(seen.add(layout.lineStart[y]), "line " + y + " starts where another one does");
      assertTrue(layout.lineStart[y] >= 0 && layout.lineStart[y] + 32 <= 0x1800,
          "line " + y + " falls outside the bitmap");
      assertEquals(0, layout.lineStart[y] % 32, "line " + y + " does not start on a line boundary");
    }
    assertEquals(192, seen.size());
  }

  /**
   * Going the other way: a write to the screen arrives as an offset and has to become the cell it
   * touched. That is the layout read backwards, and this is what says the two agree - for all 6144
   * bytes of the bitmap, not for a sample.
   */
  @Test
  void everyOffsetGoesBackToTheLineAndColumnItCameFrom() {
    for (int y = 0; y < 192; y++) {
      for (int column = 0; column < 32; column++) {
        int offset = layout.lineStart[y] + column;
        assertEquals(y, layout.lineOf(offset), "line of offset " + offset);
        assertEquals(column, layout.columnOf(offset), "column of offset " + offset);
      }
    }
  }

  @Test
  void everyAttributeOffsetGoesBackToItsCharacterRow() {
    for (int y = 0; y < 192; y++) {
      for (int column = 0; column < 32; column++) {
        int offset = layout.attrStart[y] + column;
        assertEquals(y / 8, layout.attributeRowOf(offset), "character row of offset " + offset);
        assertEquals(column, layout.columnOf(offset), "column of offset " + offset);
      }
    }
  }
}
