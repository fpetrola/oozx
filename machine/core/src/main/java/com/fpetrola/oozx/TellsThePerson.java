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
package com.fpetrola.oozx;

/** @deprecated plugdesk's, {@link dev.plugdesk.app.TellsThePerson}. Stays while a published plugin says something through it. */
@Deprecated
public final class TellsThePerson {
  private TellsThePerson() {
  }

  public static void that(String what) {
    dev.plugdesk.app.TellsThePerson.that(what);
  }

  public static void thisBuildCannot(String what) {
    dev.plugdesk.app.TellsThePerson.thisBuildCannot(what);
  }

  public static void thisBuildCannot(String what, String thatNeeds) {
    dev.plugdesk.app.TellsThePerson.thisBuildCannot(what, thatNeeds);
  }
}
