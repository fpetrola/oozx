package model.tests.display;

import com.fpetrola.oozx.speccy.modules.display.Colouring;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What colours an attribute byte asks for. Said in numbers: 0x47 is white ink on black paper,
 * bright, which is what a freshly cleared Spectrum screen is not - it is 0x38, black on white.
 */
class ColouringTest {
  private final Colouring colouring = new Colouring();

  @Test
  void theBottomThreeBitsAreInkAndTheNextThreeArePaper() {
    assertEquals(2, colouring.ink((byte) 0x0a), "ink 2");
    assertEquals(1, colouring.paper((byte) 0x0a), "paper 1");
    assertEquals(0, colouring.ink((byte) 0x38), "black ink");
    assertEquals(7, colouring.paper((byte) 0x38), "on white paper, which is what a cleared screen is");
  }

  /** Bright lifts the ink into the top eight colours; the paper's own bit already sits above it. */
  @Test
  void brightLiftsTheInkIntoTheTopEightColours() {
    assertEquals(7, colouring.ink((byte) 0x07), "white without bright");
    assertEquals(15, colouring.ink((byte) 0x47), "and bright white above it");
    assertEquals(8, colouring.paper((byte) 0x40), "the paper carries bright in the same byte");
  }

  @Test
  void aFlashingCellReadsTheOtherWayRoundWhileTheFlashIsTurnedOver() {
    byte flashing = (byte) (0x80 | 0x0a);

    assertEquals(2, colouring.ink(flashing));
    assertEquals(1, colouring.paper(flashing));

    colouring.reversed = true;
    assertEquals(1, colouring.ink(flashing), "ink and paper change places");
    assertEquals(2, colouring.paper(flashing));

    assertEquals(2, colouring.ink((byte) 0x0a), "a cell without the bit is left alone");
    assertEquals(1, colouring.paper((byte) 0x0a));
  }

  @Test
  void theTopBitIsWhatSaysACellFlashes() {
    assertTrue(Colouring.flashes((byte) 0x80));
    assertFalse(Colouring.flashes((byte) 0x7f));
  }
}
