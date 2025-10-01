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

package com.fpetrola.oozx.fuse;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.ShortByReference;

import java.util.Arrays;
import java.util.List;

/**
 * Multiplexor que reenvía todas las llamadas a múltiples instancias de LibretroCore.
 */
public class LibretroCoreMultiplexor implements LibretroCore {

    private final List<LibretroCore> delegates;

    public LibretroCoreMultiplexor(List<LibretroCore> delegates) {
        this.delegates = delegates;
    }

    public LibretroCoreMultiplexor(LibretroCore... delegates) {
        this.delegates = Arrays.asList(delegates);
    }

    @Override
    public int retro_get_beam_x() {
        return delegates.get(0).retro_get_beam_x();
    }

    @Override
    public int retro_get_beam_y() {
        return delegates.get(0).retro_get_beam_y();
    }

    @Override
    public void retro_init() {
        delegates.forEach(LibretroCore::retro_init);
    }

    @Override
    public void retro_deinit() {
        delegates.forEach(LibretroCore::retro_deinit);
    }

    @Override
    public int retro_api_version() {
        return delegates.get(0).retro_api_version();
    }

    @Override
    public void retro_set_bridge_command(bridge_command cb) {
        delegates.forEach(d -> d.retro_set_bridge_command(cb));
    }

    @Override
    public void retro_set_environment(retro_environment_t cb) {
        delegates.forEach(d -> d.retro_set_environment(cb));
    }

    @Override
    public void retro_set_video_refresh(retro_video_refresh_t cb) {
        delegates.forEach(d -> d.retro_set_video_refresh(cb));
    }

    @Override
    public void retro_set_audio_sample(retro_audio_sample_t cb) {
        delegates.forEach(d -> d.retro_set_audio_sample(cb));
    }

    @Override
    public void retro_set_audio_sample_batch(retro_audio_sample_batch_t cb) {
        delegates.forEach(d -> d.retro_set_audio_sample_batch(cb));
    }

    @Override
    public void retro_set_input_poll(retro_input_poll_t cb) {
        delegates.forEach(d -> d.retro_set_input_poll(cb));
    }

    @Override
    public void retro_set_input_state(retro_input_state_t cb) {
        delegates.forEach(d -> d.retro_set_input_state(cb));
    }

    @Override
    public void retro_get_system_info(Pointer info) {
        delegates.forEach(d -> d.retro_get_system_info(info));
    }

    @Override
    public void retro_get_system_av_info(Pointer avInfo) {
        delegates.forEach(d -> d.retro_get_system_av_info(avInfo));
    }

    @Override
    public boolean retro_load_game(retro_game_info game) {
        boolean result = true;
        for (LibretroCore d : delegates) {
            result &= d.retro_load_game(game);
        }
        return result;
    }

    @Override
    public void retro_unload_game() {
        delegates.forEach(LibretroCore::retro_unload_game);
    }

    @Override
    public void retro_run() {
        delegates.forEach(LibretroCore::retro_run);
    }

    @Override
    public void retro_reset() {
        delegates.forEach(LibretroCore::retro_reset);
    }

    @Override
    public int retro_get_memory_data(int id) {
        return delegates.get(0).retro_get_memory_data(id);
    }

    @Override
    public int retro_get_memory_data_contended(int id) {
        return delegates.get(0).retro_get_memory_data_contended(id);
    }

    @Override
    public void retro_set_memory_data(int address, int id) {
        delegates.forEach(d -> d.retro_set_memory_data(address, id));
    }

    @Override
    public void retro_set_memory_data_contended(int address, int id) {
        delegates.forEach(d -> d.retro_set_memory_data_contended(address, id));
    }

    @Override
    public long retro_get_memory_size(int id) {
        return delegates.get(0).retro_get_memory_size(id);
    }

    @Override
    public void retro_set_register_data(String register, int value) {
        delegates.forEach(d -> d.retro_set_register_data(register, value));
    }

    @Override
    public int retro_get_register_data(String register) {
        return delegates.get(0).retro_get_register_data(register);
    }

    @Override
    public void fuse_set_show_frame(boolean v) {
        delegates.forEach(d -> d.fuse_set_show_frame(v));
    }

    @Override
    public int fuse_get_show_frame() {
        return delegates.get(0).fuse_get_show_frame();
    }

    @Override
    public void retro_write_port(int port, int value) {
        delegates.forEach(d -> d.retro_write_port(port, value));
    }

    @Override
    public void retro_select_machine(String type) {
        delegates.forEach(d -> d.retro_select_machine(type));
    }

    @Override
    public void retro_if1_page(boolean in) {
        delegates.forEach(d -> d.retro_if1_page(in));
    }

    @Override
    public int retro_read_lan_port() {
        return delegates.get(0).retro_read_lan_port();
    }

    @Override
    public Pointer retro_tstates_history() {
        return delegates.get(0).retro_tstates_history();
    }

    @Override
    public void retro_tstates_history_init() {
        delegates.forEach(LibretroCore::retro_tstates_history_init);
    }

    @Override
    public boolean retro_is_intruption_enabled() {
        return delegates.get(0).retro_is_intruption_enabled();
    }
}
