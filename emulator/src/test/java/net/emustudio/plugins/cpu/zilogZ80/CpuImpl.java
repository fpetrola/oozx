/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.emustudio.plugins.cpu.zilogZ80;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static com.fpetrola.z80.cpu.State.InterruptionMode.IM0;
import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.*;

@PluginRoot(
    type = PLUGIN_TYPE.CPU,
    title = "Zilog Z80 CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
  private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);
  public final OOZ80<WordNumber> ooz80;

  private JPanel statusPanel;
  private Disassembler disassembler;
  private EmulatorEngine engine;
  private final ContextZ80Impl context = new ContextZ80Impl();

  public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings, OOZ80<WordNumber> ooz80) {
    super(pluginID, applicationApi, settings);
    this.ooz80 = ooz80;
    try {
      applicationApi.getContextPool().register(pluginID, context, Context8080.class);
      applicationApi.getContextPool().register(pluginID, context, ContextZ80.class);
    } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
      LOGGER.error("Could not register Z80 CPU context", e);
      applicationApi.getDialogs().showError(
          "Could not register Z80 CPU context. Please see log file for more details.", getTitle()
      );
    }
    context.setCPUFrequency(settings.getInt("frequency_khz", ContextZ80Impl.DEFAULT_FREQUENCY_KHZ));
  }

  @Override
  public int getInstructionLocation() {
    return engine.PC;
  }

  @Override
  public boolean setInstructionLocation(int location) {
    if (location < 0) {
      return false;
    }
    engine.PC = location & 0xFFFF;
    return true;
  }

  @Override
  public void initialize() throws PluginInitializationException {
    reset2();
  }

  private void reset2() {
    State<WordNumber> state = ooz80.getState();
    Stream.of(RegisterName.values()).forEach(r -> state.r(r).write(createValue(0)));
    state.getRegister(IR).write(createValue(0));
    state.getRegister(AF).write(createValue(0));
    state.getRegister(SP).write(createValue(0xFFFF));
    state.setIntMode(IM0);
    state.setActiveNMI(false);
    state.setHalted(false);
    state.setIff1(false);
    state.setIff2(false);
    state.setPinReset(false);
    state.setINTLine(false);
    state.setPendingEI(false);
  }

  public EmulatorEngine getEngine() {
    return engine;
  }

  @Override
  public JPanel getStatusPanel() {
    return statusPanel;
  }

  @Override
  public RunState call() {
    return engine.run(this);
  }

  @Override
  protected void resetInternal(int startPos) {
    reset2();
    //engine.reset(startPos);
  }

  @Override
  public void pause() {
    super.pause();
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  protected RunState stepInternal() throws Exception {
    ooz80.getState().setRunState(State.RunState.STATE_STOPPED_BREAK);
    ooz80.execute();
    RunState currentRunState = RunState.values()[ooz80.getState().getRunState().ordinal()];
    return currentRunState;
  }

  @Override
  public Disassembler getDisassembler() {
    return disassembler;
  }

  @Override
  protected void destroyInternal() {
  }

  @Override
  public String getVersion() {
    return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
  }

  @Override
  public String getCopyright() {
    return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
  }

  @Override
  public String getDescription() {
    return "Emulator of Zilog Z80 CPU";
  }


  private Optional<ResourceBundle> getResourceBundle() {
    try {
      return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.zilogZ80.version"));
    } catch (MissingResourceException e) {
      return Optional.empty();
    }
  }
}
