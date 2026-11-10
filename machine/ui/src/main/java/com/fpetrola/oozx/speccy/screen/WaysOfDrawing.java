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

package com.fpetrola.oozx.speccy.screen;

import java.util.ArrayList;
import java.util.List;

/**
 * The ways of drawing that arrived, each where it goes: the scalers, one of which is picked to make a windowful, and
 * the steps, which are stacked on their side of it. Each says which it is by arriving.
 */
public final class WaysOfDrawing {
  private final List<Scaler> scalers = new ArrayList<>();
  private final List<ScreenEffect> steps = new ArrayList<>();

  public WaysOfDrawing(List<? extends ScreenEffect> arrived) {
    arrived.forEach(way -> way.arriveAt(this));
  }

  void scaler(Scaler scaler) {
    scalers.add(scaler);
  }

  void step(ScreenEffect step) {
    steps.add(step);
  }

  public List<Scaler> scalers() {
    return List.copyOf(scalers);
  }

  public List<ScreenEffect> steps() {
    return List.copyOf(steps);
  }
}
