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

package com.fpetrola.oozx.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A way in: every implementation of an interface marked with this is something that was plugged
 * in, and {@link Plugins#found} is how whoever cares about them asks for the ones there are.
 * <p>
 * It goes on the interface rather than on each implementation, because being a way in is a fact
 * about the interface: a board, a tool, a window of the desk, a way of drawing the picture. What
 * implements one needs no ceremony - a jar that brings one says so in its META-INF/services, and
 * nothing else has to be told.
 * <p>
 * A plugin can declare one of its own, and what turns up for it is found the same way: there is
 * one loader over the whole plugin folder, so a jar can implement an interface another jar
 * brought.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

  /** What these are called where someone is told what a jar brings: "device", "tool", "effect". */
  String value();
}
