package model.tests.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fpetrola.oozx.speccy.windows.Edge;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Every border of a frame measures, lays out and meets a window the same, however the borders are written. */
class EveryEdgeMeasuresAsBeforeTest {
  private static final Rectangle FRAME = new Rectangle(100, 50, 400, 300);
  private static final Rectangle WINDOW = new Rectangle(120, 380, 200, 90);

  @Test
  void everyEdgeAnswersAsBefore() {
    List<String> said = new ArrayList<>();
    for (Edge edge : Edge.values()) {
      said.add(edge + " " + edge.flat() + " " + edge.facing() + " " + edge.along(WINDOW) + " " + edge.run(WINDOW)
          + " " + edge.across(WINDOW) + " " + edge.thickness(WINDOW) + " " + edge.rest(FRAME, 90, 3) + " "
          + edge.line(FRAME, 2) + " " + edge.laid(10, 20, 30, 40) + " " + edge.alongside(FRAME, WINDOW));
    }
    assertEquals(List.of(
        "BOTTOM true TOP 120 200 380 90 347 347 java.awt.Rectangle[x=10,y=20,width=30,height=40] true",
        "TOP true BOTTOM 120 200 380 90 -37 52 java.awt.Rectangle[x=10,y=20,width=30,height=40] true",
        "LEFT false RIGHT 380 90 120 200 13 102 java.awt.Rectangle[x=20,y=10,width=40,height=30] false",
        "RIGHT false LEFT 380 90 120 200 497 497 java.awt.Rectangle[x=20,y=10,width=40,height=30] false"), said);
  }
}
