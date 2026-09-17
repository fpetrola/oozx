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

package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * What repainting the whole screen costs, which is all a painter does, with nothing else running:
 * a frame from the top with every cell dirty, over and over, and the best of several blocks.
 * <p>
 * The picture is checksummed every block, and that is the point of it: the same painter on the
 * same game gives the same number, so a block that suddenly costs a tenth is not a painter that
 * got faster but a session that ended and left the machine's own three rules painting instead.
 * <p>
 * Run with {@code -Doozx.measure=true}, and give it a Spec256 game with
 * {@code -Doozx.spec256=/path/to/GAME.SNA} to measure the heaviest painter there is. Whatever it
 * prints while the machine is busy is worth nothing: this moves by ten times against a build
 * running beside it, and the frequency governor alone accounts for much of that.
 */
@EnabledIfSystemProperty(named = "oozx.measure", matches = "true")
@Timeout(value = 30, unit = TimeUnit.MINUTES)
class PaintingCostMeasurement extends MachineTest {
  private static final int FRAMES = 600, BLOCKS = 6;

  private Speccy started(String snapshot) {
    Speccy speccy = silentMachine();
    select(speccy, speccy.machine.model(Spec48.class));
    speccy.sound.output.enabled = false;
    speccy.picture.active = true;
    if (snapshot != null) {
      Snapshots.of(speccy).load(snapshot);
      speccy.loop.applyWhatWasDeferred();
    }
    return speccy;
  }

  private void measure(String what, Speccy speccy) {
    double best = Double.MAX_VALUE;
    for (int block = 0; block < BLOCKS; block++) {
      long start = System.nanoTime();
      for (int frame = 0; frame < FRAMES; frame++) {
        speccy.display.refreshAll();
        speccy.zxClock.setTStates(0);
        speccy.display.frame();
      }
      double us = (System.nanoTime() - start) / 1e3 / FRAMES;
      best = Math.min(best, us);
      System.out.printf("   %s bloque %d: %.1f us por cuadro, imagen %08x%n",
          what, block, us, Arrays.hashCode(speccy.picture.pixels));
    }
    System.out.printf("== %s MEJOR %.1f us por cuadro%n", what, best);
  }

  @Test
  void whatRepaintingCosts() {
    measure("sinclair", started(null));
    String game = System.getProperty("oozx.spec256", "");
    if (game.isEmpty()) System.out.println("== spec256 no medido: falta -Doozx.spec256=<juego.sna>");
    else if (!new File(game).exists()) System.out.println("== spec256 no medido: no existe " + game);
    else measure("spec256", started(game));
  }
}
