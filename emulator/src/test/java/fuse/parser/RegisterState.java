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

package fuse.parser;

// Represents register state in both .in and .expected files
public class RegisterState {
    private int af, bc, de, hl;
    private int af_, bc_, de_, hl_;
    private int ix, iy, sp, pc, memptr;
    private int i, r, iff1, iff2, im;
    private boolean halted;
    private int tstates;

    public RegisterState(int af, int bc, int de, int hl, int af_, int bc_, int de_, int hl_, int ix, int iy, int sp, int pc, int memptr,
                         int i, int r, int iff1, int iff2, int im, boolean halted, int tstates) {
        this.af = af;
        this.bc = bc;
        this.de = de;
        this.hl = hl;
        this.af_ = af_;
        this.bc_ = bc_;
        this.de_ = de_;
        this.hl_ = hl_;
        this.ix = ix;
        this.iy = iy;
        this.sp = sp;
        this.pc = pc;
        this.memptr = memptr;
        this.i = i;
        this.r = r;
        this.iff1 = iff1;
        this.iff2 = iff2;
        this.im = im;
        this.halted = halted;
        this.tstates = tstates;
    }

    // Getters and setters for all fields
    // (omitted here for brevity but should be included in full code)
}
