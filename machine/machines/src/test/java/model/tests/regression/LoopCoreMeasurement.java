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
package model.tests.regression;

import model.harness.MachineTest;
import com.fpetrola.oozx.Speccy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Frames per second of the machine's own loop on whichever core it has: mvn test -pl machine/core -Doozx.measure=true -Dtest=LoopCoreMeasurement. */
@EnabledIfSystemProperty(named = "oozx.measure", matches = "true")
class LoopCoreMeasurement extends MachineTest {
  @Test
  void framesPerSecondOfTheOwnLoop() {
    Speccy speccy = silentMachine();
    runFrames(speccy, 500);
    for (int round = 0; round < 3; round++) {
      long start = System.nanoTime();
      runFrames(speccy, 3000);
      double seconds = (System.nanoTime() - start) / 1e9;
      System.out.printf("loop core=%s frames=3000 seconds=%.2f fps=%.0f speed=%.0f%%%n", speccy.cpu.getOoz80().getClass().getSimpleName(), seconds, 3000 / seconds, 3000 / seconds / 50 * 100);
    }
  }

}
