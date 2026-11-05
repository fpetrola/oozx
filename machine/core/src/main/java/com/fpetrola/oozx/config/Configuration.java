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
package com.fpetrola.oozx.config;

/**
 * @deprecated the settings are plugdesk's, {@link dev.plugdesk.app.Configuration}. This stays while a published plugin
 * names it: tool-games asks {@code shared().of}, tool-catalogue {@code home}, tool-mappings {@code asText/fromText}.
 */
@Deprecated
public final class Configuration {
  private static final Configuration SHARED = new Configuration();

  private Configuration() {
  }

  public static Configuration shared() {
    return SHARED;
  }

  public <T> T of(Class<T> type) {
    return dev.plugdesk.app.Configuration.shared().of(type);
  }

  public static java.io.File home() {
    return dev.plugdesk.app.Configuration.home();
  }

  public static String asText(Object value) {
    return dev.plugdesk.app.Configuration.asText(value);
  }

  public static Object fromText(String text, Class<?> type) {
    return dev.plugdesk.app.Configuration.fromText(text, type);
  }
}
