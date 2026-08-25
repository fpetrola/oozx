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

package model.tests.api;

import com.fpetrola.oozx.api.Screen;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A screenshot arrives inside a game as whatever the JSON said, so it reaches us as a map and has
 * to be asked whether it is one of these. Every caller reads the answer as "or null", which is the
 * half worth pinning down: the fields are only ever looked at once null has been ruled out.
 */
public class ScreenFromTheApiTest {

  private static Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) map.put((String) keysAndValues[i], keysAndValues[i + 1]);
    return map;
  }

  @Test
  public void aScreenshotComesBackWithTheFieldsTheApiSent() {
    Screen screen = Screen.from(map("entry_id", 1234, "url", "http://x/y.scr", "size", 6912, "filename", "y.scr"));

    assertNotNull(screen);
    assertEquals(1234, screen.entry_id);
    assertEquals("http://x/y.scr", screen.url);
    assertEquals(6912, screen.size);
    assertEquals("y.scr", screen.filename);
  }

  @Test
  public void aFieldNobodyHereKnowsAboutIsIgnoredRatherThanRefused() {
    Screen screen = Screen.from(map("url", "http://a", "someFieldAddedLaterByTheApi", "z"));

    assertNotNull(screen);
    assertEquals("http://a", screen.url);
  }

  @Test
  public void nothingInItIsStillOneOfThese() {
    assertNotNull(Screen.from(map()));
  }

  @Test
  public void whatIsNotAScreenshotIsNull() {
    assertNull(Screen.from(null));
    assertNull(Screen.from("a line of text"));
    assertNull(Screen.from(List.of(1, 2, 3)));
    assertNull(Screen.from(42));
  }
}
