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

package com.fpetrola.oozx.fuse.sound.blip;

import static com.fpetrola.oozx.fuse.sound.blip.BlipBuffer.BLIP_RES;
import static java.lang.Math.*;

public class BlipEq {
  public final double treble;
  public final long rolloffFreq;
  public final long sampleRate;
  public final long cutoffFreq;

  public BlipEq(double trebleDb) {
    this(trebleDb, 0, 44100, 0);
  }

  public BlipEq(double treble, long rolloffFreq, long sampleRate, long cutoffFreq) {
    this.treble = treble;
    this.rolloffFreq = rolloffFreq;
    this.sampleRate = sampleRate;
    this.cutoffFreq = cutoffFreq;
  }

  void generate(int count, float[] fImpulse, int fImpulseIndex) {
    // lower cutoff for narrow kernels
    double oversample = BLIP_RES * 2.25 / count + 0.85;
    double halfRate = sampleRate * 0.5;
    if (cutoffFreq != 0)
      oversample = halfRate / cutoffFreq;
    double cutoff = rolloffFreq * oversample / halfRate;

    genSinc(count, (BLIP_RES * oversample), treble, cutoff, fImpulse, fImpulseIndex);

    // apply Hamming window
    double toFraction = PI / (count - 1);
    for (int i = count; i-- != 0; ) {
      float i1 = fImpulse[fImpulseIndex + i] * (float) (0.54f - 0.46f * cos(i * toFraction));
      fImpulse[fImpulseIndex + i] = i1;
    }
  }

  private void genSinc(int count, double oversample, double treble, double cutoff, float[] fImpulse, int fImpulseIndex) {
    if (cutoff > 0.999)
      cutoff = 0.999;
    if (treble < -300.0)
      treble = -300.0;
    if (treble > 5.0)
      treble = 5.0;

    double maxh = 4096.0;
    double rolloff = pow(10.0, 1.0 / (maxh * 20.0) * treble / (1.0 - cutoff));
    double pow_a_n = pow(rolloff, maxh - maxh * cutoff);
    double to_angle = PI / 2 / maxh / oversample;

    for (int i = 0; i < count; i++) {
      double angle = ((i - count) * 2 + 1) * to_angle;
      double cos_nc_angle = cos(maxh * cutoff * angle);
      double cos_nc1_angle = cos((maxh * cutoff - 1.0) * angle);
      double cos_angle = cos(angle);
      double c = (rolloff * cos((maxh - 1.0) * angle) - cos(maxh * angle)) * pow_a_n - rolloff * cos_nc1_angle + cos_nc_angle;
      double d = 1.0 + rolloff * (rolloff - cos_angle - cos_angle);
      double b = 2.0 - cos_angle - cos_angle;
      double a = 1.0 - cos_angle - cos_nc_angle + cos_nc1_angle;

      fImpulse[fImpulseIndex + i] = (float) ((a * d + c * b) / (b * d));
    }
  }
}
