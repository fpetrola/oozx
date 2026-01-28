package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The picture is what the beam saw: a byte written after the beam passed its cell is seen next
 * frame, one written before is seen this frame. That is what gives a cell more than two colours
 * when a game changes its attribute mid-frame, and it is decided at the moment of each write,
 * so it is asked here with the beam parked at known places rather than by running a game.
 * <p>
 * Positions are in the machine's own terms: a column is eight pixels, and rows and columns
 * count from the top left of the border. The cell watched is column 5 of character row 12,
 * which is pixel rows 96 to 103 of the picture proper.
 */
class ScreenFollowsTheBeamTest {
  private static final int CELL_COLUMN = 5, CELL_ROW = 12, FIRST_PIXEL_ROW = CELL_ROW * 8;
  private static final int PAPER_RED_INK_BLUE = 0x11, PAPER_GREEN_INK_BLUE = 0x21;
  private static final int RED = 2, GREEN = 4, BLUE = 1, WHITE = 7;

  private final Speccy speccy = speccy();
  private final int borderColumns, borderRows;

  ScreenFollowsTheBeamTest() {
    borderColumns = speccy.display.BORDER_WIDTH_COLS;
    borderRows = speccy.display.BORDER_HEIGHT;
  }

  private static Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    return speccy;
  }

  private void beamAt(int column, int row) {
    speccy.zxClock.setTStates((int) (speccy.machine.current.lineStart(row) + column * 4L));
  }

  /** The beam over the picture proper, past the border. */
  private void beamOverPixel(int column, int row) {
    beamAt(borderColumns + column, borderRows + row);
  }

  private void write(int address, int value) {
    speccy.memory.write(address, (byte) value);
  }

  private static int attributeOf(int column, int row) {
    return 0x5800 + row * 32 + column;
  }

  private static int pixelsOf(int column, int row) {
    return 0x4000 | ((row & 0xC0) << 5) | ((row & 7) << 8) | ((row & 0x38) << 2) | column;
  }

  /** The frame ends with the clock back at the top, the way the machine leaves it. */
  private void endFrame() {
    speccy.zxClock.setTStates(0);
    speccy.display.frame();
  }

  private int colourAt(int x, int y) {
    int rgb = speccy.picture.pixels[y * Picture.WIDTH + x];
    for (int colour = 0; colour < Picture.PALETTE.length; colour++) {
      if (Picture.PALETTE[colour] == rgb) return colour;
    }
    return -1;
  }

  /** The colour of the first pixel of the watched cell on one of its rows. */
  private int cellRow(int row) {
    return colourAt((borderColumns + CELL_COLUMN) * 8, borderRows + row);
  }

  private int cellRowLastPixel(int row) {
    return colourAt((borderColumns + CELL_COLUMN) * 8 + 7, borderRows + row);
  }

  @Test
  void whatIsWrittenBehindTheBeamIsSeenNextFrame() {
    write(attributeOf(CELL_COLUMN, CELL_ROW), PAPER_RED_INK_BLUE);
    endFrame();

    beamOverPixel(16, FIRST_PIXEL_ROW + 4);
    write(attributeOf(CELL_COLUMN, CELL_ROW), PAPER_GREEN_INK_BLUE);
    write(pixelsOf(CELL_COLUMN, FIRST_PIXEL_ROW + 2), 0xFF);
    write(pixelsOf(CELL_COLUMN, FIRST_PIXEL_ROW + 6), 0xFF);
    endFrame();

    assertEquals(RED, cellRow(FIRST_PIXEL_ROW), "the row the beam passed keeps the old attribute");
    assertEquals(RED, cellRow(FIRST_PIXEL_ROW + 2), "and the old pixels");
    assertEquals(RED, cellRow(FIRST_PIXEL_ROW + 4), "the beam's own row was already past the cell");
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW + 5), "the row still to come shows the new attribute");
    assertEquals(BLUE, cellRow(FIRST_PIXEL_ROW + 6), "and the new pixels");
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW + 7));

    endFrame();
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW), "next frame the beam sees the write");
    assertEquals(BLUE, cellRow(FIRST_PIXEL_ROW + 2));
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW + 4));
  }

  @Test
  void whatIsWrittenAheadOfTheBeamIsSeenThisFrame() {
    write(attributeOf(CELL_COLUMN, CELL_ROW), PAPER_RED_INK_BLUE);
    endFrame();

    beamOverPixel(16, FIRST_PIXEL_ROW - 50);
    write(attributeOf(CELL_COLUMN, CELL_ROW), PAPER_GREEN_INK_BLUE);
    endFrame();

    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW));
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW + 7));
  }

  @Test
  void theBorderChangesColourWhereTheBeamWas() {
    speccy.display.border.becomes(WHITE);
    endFrame();
    assertEquals(WHITE, colourAt(30 * 8, 5));
    assertEquals(WHITE, colourAt(8, 230));

    beamAt(20, 10);
    speccy.display.border.becomes(RED);
    endFrame();

    assertEquals(WHITE, colourAt(30 * 8, 5), "above the beam the border keeps its colour");
    assertEquals(WHITE, colourAt(10 * 8, 10), "so does the beam's row up to where it was");
    assertEquals(RED, colourAt(30 * 8, 10), "and changes from there");
    assertEquals(RED, colourAt(8, 100), "down the left of the picture");
    assertEquals(RED, colourAt(30 * 8, 230), "to the bottom");
  }

  @Test
  void flashSwapsInkAndPaperEverySixteenFrames() {
    write(pixelsOf(CELL_COLUMN, FIRST_PIXEL_ROW), 0xF0);
    write(attributeOf(CELL_COLUMN, CELL_ROW), 0x80 | PAPER_RED_INK_BLUE);

    for (int frame = 1; frame <= 16; frame++) {
      endFrame();
      assertEquals(BLUE, cellRow(FIRST_PIXEL_ROW), "frame " + frame);
      assertEquals(RED, cellRowLastPixel(FIRST_PIXEL_ROW), "frame " + frame);
    }
    endFrame();
    assertEquals(RED, cellRow(FIRST_PIXEL_ROW), "swapped from the seventeenth frame");
    assertEquals(BLUE, cellRowLastPixel(FIRST_PIXEL_ROW));

    for (int frame = 18; frame <= 33; frame++) endFrame();
    assertEquals(BLUE, cellRow(FIRST_PIXEL_ROW), "and back from the thirty-third");
    assertEquals(RED, cellRowLastPixel(FIRST_PIXEL_ROW));
  }

  @Test
  void switchingTheScreenPageMidFrameSplitsThePicture() {
    speccy.machine.select(speccy.machine.model(Spec128.class));
    speccy.machine.model(Spec128.class).memoryPortWrite(0x7ffd, (byte) 7);
    write(attributeOf(CELL_COLUMN, CELL_ROW), PAPER_RED_INK_BLUE);
    write(0x8000 + attributeOf(CELL_COLUMN, CELL_ROW), PAPER_GREEN_INK_BLUE);
    endFrame();
    assertEquals(RED, cellRow(FIRST_PIXEL_ROW + 7));

    beamOverPixel(16, FIRST_PIXEL_ROW + 4);
    speccy.machine.model(Spec128.class).memoryPortWrite(0x7ffd, (byte) (0x08 | 7));
    endFrame();

    assertEquals(RED, cellRow(FIRST_PIXEL_ROW), "the rows the beam passed came from the old page");
    assertEquals(RED, cellRow(FIRST_PIXEL_ROW + 4));
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW + 5), "the rest from the new one");
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW + 7));

    endFrame();
    assertEquals(GREEN, cellRow(FIRST_PIXEL_ROW));
  }
}
