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

package com.fpetrola.oozx.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Screen {
  private static final ObjectMapper MAPPER = new ObjectMapper();

    public int entry_id;
    public String release_seq;
    public String filename;
    public String url;
    public String scrUrl;
    public int size;
    public String type;
    public String format;
    public String title;

  /**
   * One of these out of whatever the API handed back, or null if it is not one.
   * <p>
   * It lived as a static on the game browser's window, so a class that talks to a web service
   * imported a window to read its own answers.
   */
  public static Screen from(Object fromTheApi) {
    try {
      return MAPPER.convertValue(fromTheApi, Screen.class);
    } catch (IllegalArgumentException notOne) {
      return null;
    }
  }
}
