/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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
 */package com.fpetrola.oozx.speccy.devices.sound;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.modules.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.OptionalBinder;

/** The platform's audio as the card the mix goes to. */
public class SoundDevices extends AbstractModule implements Extension {
  protected void configure() {
    OptionalBinder.newOptionalBinder(binder(), SoundCard.class).setBinding().to(JavaSoundDevice.class);
  }
}
