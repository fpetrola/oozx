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
package net.emustudio.plugins.cpu.intel8080.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;

/**
 * Extended CPU context for 8080 processor.
 */
@PluginContext
public interface Context8080 extends CPUContext {

    /**
     * Attach a device into the CPU.
     *
     * @param port   CPU port where the device should be attached
     * @param device the device
     * @return true on success, false otherwise
     */
    boolean attachDevice(int port, CpuPortDevice device);

    /**
     * Detach a device from the CPU.
     *
     * @param port the CPU port number which will be freed.
     */
    void detachDevice(int port);

    /**
     * Set CPU frequency in kHZ
     *
     * @param freq new frequency in kHZ
     */
    void setCPUFrequency(int freq);

    /**
     * Device attachable to CPU port. It's not a DeviceContext because some machines need port address (low + high byte)
     * for being able to respond (e.g. ZX-spectrum port 0xFE).
     */
    interface CpuPortDevice {

        /**
         * Read a byte data from device
         *
         * @param portAddress port address. Low 8 bits is the port number.
         * @return byte data from the port
         */
        byte read(int portAddress);

        /**
         * Write data to the device
         *
         * @param portAddress port address. Low 8 bits is the port number.
         * @param data        byte data to be written
         */
        void write(int portAddress, byte data);

        /**
         * Get device port name
         *
         * @return device port name
         */
        String getName();
    }
}
