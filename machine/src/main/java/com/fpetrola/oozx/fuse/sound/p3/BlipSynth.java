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

package com.fpetrola.oozx.fuse.sound.p3;

import static com.fpetrola.oozx.fuse.sound.p3.BlipBuffer.BLIP_SYNTH_RANGE;

public class BlipSynth {
  private final BlipSynthImpl impl;

  public BlipSynth(int quality, int range) {
    int absRange = range < 0 ? -range : range;
    short[] impulses = new short[BlipBuffer.BLIP_RES * (quality / 2) + 1];
    impl = new BlipSynthImpl(impulses, quality);
//    volume(1.0 / absRange);
  }

  public void volume(double v) {
    impl.volumeUnit(v * (1.0 /
        (BLIP_SYNTH_RANGE <
            0 ? -(BLIP_SYNTH_RANGE) :
            BLIP_SYNTH_RANGE)));
  }

  public void trebleEq(BlipEq eq) {
    impl.trebleEq(eq);
  }

  public void output(BlipBuffer buf) {
    impl.buf = buf;
    impl.lastAmp = 0;
  }

  public BlipBuffer output() {
    return impl.buf;
  }

  public void update(long time, int amplitude) {
    if (impl.buf != null) {
      int delta = amplitude - impl.lastAmp;
      impl.lastAmp = amplitude;
      offset(time, delta);
    }
  }

  public void offset(long time, int delta) {
    offsetResampled(time * impl.buf.factor + impl.buf.offset, delta);
  }

  public void offsetResampled(long time, int delta) {
    impl.offsetResampled(time, delta);
  }

  // Versión inline rápida
  public void offsetInline(long time, int delta) {
    offsetResampled(time * impl.buf.factor + impl.buf.offset, delta);
  }
}
