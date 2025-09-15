/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.screen;

import com.sun.jna.NativeLong;

import javax.swing.*;

public class FuseLibretroSwing {

    public static void main(String[] args) {
        args = new String[]{"/home/fernando/detodo/desarrollo/m/zx/roms/jsw.z80"};

        String gamePath = args[0];

        FuseLibretroExample.LibretroCore core = FuseLibretroExample.LibretroCore.INSTANCE;
        core.retro_init();

        SpectrumPanel panel = new SpectrumPanel(320, 240); // tamaño inicial, se ajustará después
        JFrame frame = new JFrame("ZX Spectrum via Libretro");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);

        core.retro_set_video_refresh((data, w, h, pitch) -> {
            panel.updateFrame(data, w, h, pitch);
        });

        core.retro_set_environment((cmd, p) -> false);
        core.retro_set_audio_sample((l, r) -> {});
        core.retro_set_audio_sample_batch((p, f) -> f);
        core.retro_set_input_poll(() -> {});
        core.retro_set_input_state((port, device, index, id) -> (short) 0);

        // Cargar snapshot
        FuseLibretroExample.retro_game_info game = new FuseLibretroExample.retro_game_info();
        game.path = gamePath;
        game.data = null;
        game.size = new NativeLong(0);
        game.meta = null;

        if (!core.retro_load_game(game)) {
            throw new RuntimeException("No se pudo cargar el snapshot " + gamePath);
        }

        // Loop de ejecución (ejemplo básico)
        new Timer(16, e -> core.retro_run()).start();
    }
}
