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

import com.google.inject.Singleton;

/**
 * Whether the emulator should keep running.
 * <p>
 * The flag is written from one thread and read from another: closing the window finishes the
 * session on the event dispatch thread, while the emulation loop reads it on its own. It is
 * volatile for that reason — without it the loop is free to hoist the read and never observe
 * the change, and the emulator keeps running after its window is gone.
 */
@Singleton
public class EmulationSession {

  private volatile boolean alive = true;

  public boolean isAlive() {
    return alive;
  }

  public void finish() {
    alive = false;
  }

  private final java.util.concurrent.CountDownLatch out = new java.util.concurrent.CountDownLatch(1);
  private volatile boolean looping;

  /** Dicho por quien la va a correr en un bucle, antes de arrancarlo. */
  public void looping() {
    looping = true;
  }

  /** Dicho por el bucle al salir: ya no queda nada suyo corriendo en ese hilo. */
  public void outOfTheLoop() {
    out.countDown();
  }

  /**
   * Espera a que el bucle que la corre salga, si hay uno. Hasta entonces ese hilo tiene la maquina
   * en su pila, y lo que la armo no se puede soltar.
   */
  public void awaitOutOfTheLoop() {
    if (!looping) return;
    try {
      out.await(1, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
