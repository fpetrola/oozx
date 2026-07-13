/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.z80.analysis;

import com.fpetrola.z80.bytecode.tests.MyMemorySetter;
import com.fpetrola.z80.bytecode.tests.MyRegistersSetter;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;

/**
 * RZX startup shared by the baseline and instrumented runners. Same logic the working
 * path used in PcInterceptor: force a few F values along the boot menu path, and when
 * pc reaches 34738 load the RZX snapshot (registers + memory) and start frame playback.
 * From then on it detects frame changes to record per-frame memory hashes.
 */
public class RzxBootstrap {
  public static final String DEFAULT_RZX =
      "/home/fernando/detodo/spectrum/oozx/Jet Set Willy - Mildly Patched.rzx";

  private final MiniZX app;
  private final RZXPlayerIO<?> io;
  private final String rzxPath;
  private final Runnable afterSnapshotLoad;
  public final FrameHasher hasher = new FrameHasher();
  private boolean initializing = true;
  private int lastFrame = -1;

  public RzxBootstrap(MiniZX app, RZXPlayerIO<?> io, String rzxPath, Runnable afterSnapshotLoad) {
    this.app = app;
    this.io = io;
    this.rzxPath = rzxPath;
    this.afterSnapshotLoad = afterSnapshotLoad;
  }

  public void onPc(int address) {
    if (initializing) {
      switch (address) {
        case 34629 -> app.F(0);
        case 34637, 34720, 34726, 34732 -> app.F(1);
        case 34738 -> {
          // hash of the whole boot-menu execution up to here: comparable between runs
          // even without an RZX file present.
          System.out.println("pre-snapshot mem hash: " + Long.toHexString(FrameHasher.hash(app.mem)));
          EmulatedMiniZX.setupRzx(new MyRegistersSetter(app), io, rzxPath, new MyMemorySetter(app));
          initializing = false;
          app.fetchCounter = -2;
          if (afterSnapshotLoad != null)
            afterSnapshotLoad.run();
        }
      }
    } else {
      int frame = io.getCurrentFrameIndex();
      if (frame != lastFrame) {
        hasher.onFrame(frame, app.mem);
        lastFrame = frame;
        Tracer.currentFrame = frame;
        TrackLog.onFrame(frame, app.mem);
      }
    }
  }
}