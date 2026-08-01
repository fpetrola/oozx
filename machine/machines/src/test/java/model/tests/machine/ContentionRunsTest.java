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
package model.tests.machine;

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A batched contention-run lookup (replacing five per-cycle lookups with one table lookup for
 * indexed instructions) must sum to the same total as summing individual per-cycle lookups, for
 * every start T-state on every model - an off-by-one table would silently skew frame timing.
 */
class ContentionRunsTest {

  private final Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));

  @Test
  void aRunLookedUpOnceIsTheRunLookedUpOneCycleAtATime() {
    Ula ula = injector.getInstance(Ula.class);
    // Asked for before there is a machine, the way the phase processor asks, so the refill is
    // what is tested and not just the first build.
    byte[] askedEarly = ula.contention.run(5);
    for (Class<? extends Spectrum> model : List.of(Spec48.class, Spec128.class,
        SpecPlus2A.class, SpecPlus3.class)) {
      Spectrum machine = injector.getInstance(model);
      ula.contention.forMachine(machine);
      int frame = machine.getTimings().tstatesPerFrame();
      for (int times = 2; times <= 7; times++) {
        byte[] run = ula.contention.run(times);
        for (int start = 0; start < frame + 200; start++) {
          int t = start;
          for (int i = 0; i < times; i++) {
            t += ula.contention.delayNoMreq[t] + 1;
          }
          assertEquals(t - start, run[start],
              model.getSimpleName() + ": a run of " + times + " from " + start);
        }
      }
    }
    assertEquals(ula.contention.run(5), askedEarly, "the table asked for early is the one refilled, not another");
  }
}
