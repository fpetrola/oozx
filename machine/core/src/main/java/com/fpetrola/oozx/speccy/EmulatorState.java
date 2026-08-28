/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class EmulatorState extends Structure {
    public EmulatorState(Pointer data) {
        super(data);
    }

    public short pc;
    public short sp;
    public short af, bc, de, hl;
    public short iy, ix;
    public byte ime;
    public byte halted;
    public int tstates;

    @Override
    protected java.util.List<String> getFieldOrder() {
        return java.util.Arrays.asList(
            "pc", "sp", "af", "bc", "de", "hl", "iy", "ix",
            "ime", "halted", "tstates"
        );
    }
}
