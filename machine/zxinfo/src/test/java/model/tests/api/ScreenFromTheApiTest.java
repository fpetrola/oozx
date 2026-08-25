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
