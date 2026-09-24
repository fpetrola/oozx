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

package com.fpetrola.oozx.speccy.pokes;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.devices.Desk;
import com.fpetrola.oozx.speccy.devices.EmulatorWindow;
import com.fpetrola.oozx.speccy.devices.MachineTool;
import com.fpetrola.oozx.speccy.devices.WhatGameThisIs;
import com.fpetrola.oozx.speccy.windows.Widgets;
import com.google.inject.Inject;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Los pokes de un juego: el boton, el dialogo, y lo que quedo aplicado en cada ventana.
 * <p>
 * Antes vivia en la ventana de maquina, que para ofrecer pokes tenia que saber leer listados
 * .pok. Ahora es una herramienta: sacar este plugin saca el boton.
 */
public class PokesTool implements MachineTool {

  private final PokesManager pokes = new PokesManager();
  /** Quien sabe que juego es: puesto por quien arma las herramientas, o ninguno. */
  @Inject
  private Set<WhatGameThisIs> aboutGames = Set.of();

  /** Lo aplicado en cada ventana, que se olvida sola cuando la ventana se cierra. */
  private final Map<EmulatorWindow, List<PokFile.PokeMod>> applied = new WeakHashMap<>();

  public javax.swing.Icon icon() {
    return Widgets.loadIcon("1F513.svg");
  }

  public String tooltip() {
    return "Cheats/Pokes";
  }

  public void use(EmulatorWindow window) {
    Speccy machine = window.machine();
    String file = machine == null || machine.control == null ? null : machine.control.getFilename();
    Desk.Game identified = file == null || file.isEmpty() ? null
        : aboutGames.stream().map(one -> one.gameIn(file)).filter(java.util.Objects::nonNull)
        .findFirst().orElse(null);
    String gameName = identified != null ? identified.title() : file;
    if (identified == null && gameName != null) {
      gameName = new java.io.File(gameName).getName().replaceAll("\\.(tap|tzx|z80|sna|szx)$", "");
    }
    if (gameName == null || gameName.isEmpty()) {
      JOptionPane.showMessageDialog(window.picture(), "No game loaded", "Info",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    // Por el numero cuando el catalogo conoce el juego, que es como ZXDB une una entrada con su
    // .pok. El nombre es lo que queda cuando no, y es una adivinanza.
    List<PokFile> found = pokes.findPokesForEntry(identified == null ? null : identified.id());
    if (found.isEmpty()) {
      found = pokes.findPokesForGame(gameName);
    }
    if (found.isEmpty()) {
      JOptionPane.showMessageDialog(window.picture(), "No pokes found for: " + gameName,
          "Pokes Not Found", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    List<PokFile.PokeMod> here = applied.computeIfAbsent(window, nobody -> new ArrayList<>());
    PokesDialog dialog = new PokesDialog((Frame) SwingUtilities.getWindowAncestor(window.picture()),
        gameName, found, pokes, new ArrayList<>(here));
    dialog.setOnPokesAppliedListener(chosen -> apply(machine, here, chosen));
    dialog.setOnPokesChangedListener(removed -> revert(machine, here, removed));
    dialog.setVisible(true);
  }

  private static void apply(Speccy machine, List<PokFile.PokeMod> here, List<PokFile.PokeMod> chosen) {
    List<PokFile.PokeMod> fresh = chosen.stream().filter(mod -> here.stream().noneMatch(
        was -> was.getName().equals(mod.getName())
            && was.getRawInstruction().equals(mod.getRawInstruction()))).toList();
    here.clear();
    here.addAll(chosen);
    fresh.forEach(mod -> mod.getParsedInstruction().apply(on(machine)));
  }

  private static void revert(Speccy machine, List<PokFile.PokeMod> here, List<PokFile.PokeMod> removed) {
    for (PokFile.PokeMod mod : removed) {
      here.removeIf(was -> was.getName().equals(mod.getName())
          && was.getRawInstruction().equals(mod.getRawInstruction()));
      // Lo que habia antes quedo guardado en la instruccion cuando se aplico.
      mod.getParsedInstruction().revert(on(machine));
    }
  }

  private static PokInstruction.EmulatorMemoryWriter on(Speccy machine) {
    return new PokInstruction.EmulatorMemoryWriter() {
      public void writeMemory(int bank, int address, int value) {
        machine.cpu.getOoz80().getState().getMemory().write(address, value);
      }

      public int readMemory(int bank, int address) {
        return machine.cpu.getOoz80().getState().getMemory().read(address);
      }
    };
  }

  public void remember(EmulatorWindow window, OOZxConfiguration.WindowState into) {
    List<OOZxConfiguration.PokModState> kept = new ArrayList<>();
    for (PokFile.PokeMod mod : applied.getOrDefault(window, List.of())) {
      PokInstruction instruction = mod.getParsedInstruction();
      kept.add(new OOZxConfiguration.PokModState(mod.getName(), mod.getRawInstruction(),
          mod.getPokFileName(), mod.getGameName(), mod.getInstructionType(), mod.getDescription(),
          instruction.getPreviousValue(), instruction.getPreviousBank(), instruction.getPreviousAddress()));
    }
    into.setAppliedPokes(kept);
  }

  /**
   * Lo que estaba aplicado, anotado de vuelta. No se escribe en la memoria: la ventana se reabre
   * con su snapshot, que ya los trae puestos.
   */
  public void restore(EmulatorWindow window, OOZxConfiguration.WindowState from) {
    if (from.getAppliedPokes() == null || from.getAppliedPokes().isEmpty()) {
      return;
    }
    List<PokFile.PokeMod> here = applied.computeIfAbsent(window, nobody -> new ArrayList<>());
    here.clear();
    for (OOZxConfiguration.PokModState kept : from.getAppliedPokes()) {
      PokFile.PokeMod mod = new PokFile.PokeMod(kept.getName(), kept.getRawInstruction(),
          kept.getPokFileName(), kept.getGameName());
      PokInstruction instruction = mod.getParsedInstruction();
      if (instruction != null) {
        instruction.setPreviousValue(kept.getPreviousValue());
        instruction.setPreviousBank(kept.getPreviousBank());
        instruction.setPreviousAddress(kept.getPreviousAddress());
      }
      here.add(mod);
    }
  }
}
