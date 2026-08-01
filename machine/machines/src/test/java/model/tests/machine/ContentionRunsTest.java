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
