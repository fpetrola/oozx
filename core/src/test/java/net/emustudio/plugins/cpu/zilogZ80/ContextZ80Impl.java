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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ThreadSafe
public final class ContextZ80Impl  implements ContextZ80 {
    public final static int DEFAULT_FREQUENCY_KHZ = 4000;
    private final static byte NO_DATA = (byte) 0xFF;
    private final static Logger LOGGER = LoggerFactory.getLogger(ContextZ80Impl.class);

    private final ConcurrentMap<Integer, Context8080.CpuPortDevice> devices = new ConcurrentHashMap<>();

    private volatile EmulatorEngine engine;
    private volatile int clockFrequencyKHz = DEFAULT_FREQUENCY_KHZ;

    public void setEngine(EmulatorEngine engine) {
        this.engine = engine;
    }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(int port, Context8080.CpuPortDevice device) {
        Context8080.CpuPortDevice oldDevice = devices.get(port);
        if (oldDevice != null) {
            LOGGER.debug("[port={}, device={}] Could not attach device to given port. The port is already taken by: {}", port, device.getName(), oldDevice.getName());
            return false;
        }
        if (devices.putIfAbsent(port, device) != null) {
            LOGGER.debug("[port={}, device={}] Could not attach device to given port. The port is already taken.", port, device.getName());
            return false;
        }
        LOGGER.debug("[port={},device={}] Device was attached to CPU", port, device.getName());
        return true;
    }

    @Override
    public void detachDevice(int port) {
        if (devices.remove(port) != null) {
            LOGGER.debug("Detached device from port " + port);
        }
    }

    void clearDevices() {
        devices.clear();
    }

    void writeIO(int portAddress, byte data) {
        Context8080.CpuPortDevice device = devices.get(portAddress & 0xFF);
        if (device != null) {
            device.write(portAddress, data);
        }
    }

    byte readIO(int portAddress) {
        Context8080.CpuPortDevice device = devices.get(portAddress & 0xFF);
        if (device != null) {
            return device.read(portAddress);
        }
        return NO_DATA;
    }

    @Override
    public boolean isInterruptSupported() {
        return true;
    }

    @Override
    public void signalInterrupt(byte[] data) {
    }

    @Override
    public int getCPUFrequency() {
        return clockFrequencyKHz;
    }

    @Override
    public void setCPUFrequency(int frequency) {
        if (frequency <= 0) {
            throw new IllegalArgumentException("Invalid CPU frequency (expected > 0): " + frequency);
        }
        clockFrequencyKHz = frequency;
    }

    @Override
    public void signalNonMaskableInterrupt() {
    }

    @Override
    public void addCycles(long tStates) {
    }

    public boolean passedCyclesSupported() {
        return true;
    }
}
