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
import com.fpetrola.oozx.Spectrum;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.modules.Ula;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A run of contended internal cycles looked up once has to come to exactly what it comes to
 * looked up one cycle at a time, from every T-state a run can start at, on every machine.
 * <p>
 * The core takes the run tables for its indexed instructions - five lookups became one - and
 * the only thing that can go wrong is quietly: a table one short somewhere puts every frame's
 * timing off by a T-state and nothing crashes.
 */
class ContentionRunsTest {

  private final Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));

  @Test
  void aRunLookedUpOnceIsTheRunLookedUpOneCycleAtATime() {
    Ula ula = injector.getInstance(Ula.class);
    // Asked for before there is a machine, the way the phase processor asks, so the refill is
    // what is tested and not just the first build.
    byte[] askedEarly = ula.noMreqRun(5);
    for (Class<? extends Spectrum> model : List.of(Spec48.class, Spec48Ntsc.class, Spec128.class,
        SpecPlus2A.class, SpecPlus3.class, Pentagon.class)) {
      Spectrum machine = injector.getInstance(model);
      ula.tablesFor(machine);
      int frame = machine.getTimings().tstatesPerFrame;
      for (int times = 2; times <= 7; times++) {
        byte[] run = ula.noMreqRun(times);
        for (int start = 0; start < frame + 200; start++) {
          int t = start;
          for (int i = 0; i < times; i++) {
            t += ula.contentionNoMreq[t] + 1;
          }
          assertEquals(t - start, run[start],
              model.getSimpleName() + ": a run of " + times + " from " + start);
        }
      }
    }
    assertEquals(ula.noMreqRun(5), askedEarly, "the table asked for early is the one refilled, not another");
  }
}
