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

import java.util.Arrays;

public class BlipBuffer {
    // Constants
    public static final int BLIP_BUFFER_ACCURACY = 16;
    private static final int BLIP_BUFFER_DEF_MSEC_LENGTH = 1000 / 4;
    private static final int BLIP_BUFFER_DEF_STEREO = 0;
    private static final int BLIP_BUFFER_DEF_ENTIRE_BUFF = 1;
    private static final int BUFFER_EXTRA = BlipSynth.BLIP_WIDEST_IMPULSE + 2;

    // Fields
    public long factor;
    public long offset;
    public long[] buffer;
    private long bufferSize;
    private long readerAccum;
    private int bassShift;
    private long sampleRate;
    private long clockRate;
    private int bassFreq;
    private int length;

    // Constructor
    public BlipBuffer() {
        init();
    }

    // Initialize buffer
    private void init() {
        factor = Long.MAX_VALUE;
        offset = 0;
        buffer = null;
        bufferSize = 0;
        sampleRate = 0;
        readerAccum = 0;
        bassShift = 0;
        clockRate = 0;
        bassFreq = 16;
        length = 0;
    }

    // Cleanup
    public void delete() {
        buffer = null;
    }

    // Set sample rate and buffer length
    public String setSampleRate(long samplesPerSec, int msecLength) {
        long newSize = (Long.MAX_VALUE >> BLIP_BUFFER_ACCURACY) - BUFFER_EXTRA - 64;
        if (msecLength != 0) {
            long s = (samplesPerSec * (msecLength + 1) + 999) / 1000;
            if (s < newSize) {
                newSize = s;
            }
        }

        if (bufferSize != newSize) {
            long[] newBuffer = new long[(int) (newSize + BUFFER_EXTRA)];
            if (newBuffer == null) {
                return "Out of memory";
            }
            buffer = newBuffer;
            bufferSize = newSize;
        }

        sampleRate = samplesPerSec;
        length = (int) (newSize * 1000 / samplesPerSec - 1);
        if (clockRate != 0) {
            setClockRate(clockRate);
        }
        setBassFreq(bassFreq);
        clear(BLIP_BUFFER_DEF_ENTIRE_BUFF);
        return null;
    }

    // Set clock rate
    public void setClockRate(long rate) {
        clockRate = rate;
        factor = clockRateFactor(rate);
    }

    // Set bass frequency
    public void setBassFreq(int freq) {
        bassFreq = freq;
        int shift = 31;
        if (freq > 0) {
            shift = 13;
            long f = (freq << 16) / sampleRate;
            while ((f >>= 1) != 0 && --shift != 0) {
            }
        }
        bassShift = shift;
    }

    // End frame
    public void endFrame(long time) {
        offset += time * factor;
    }

    // Read samples
    public long readSamples(short[] dest, int maxSamples, int stereo) {
        long count = samplesAvail();
        if (count > maxSamples) {
            count = maxSamples;
        }

        if (count > 0) {
            int sampleShift = BlipSynth.BLIP_SAMPLE_BITS - 16;
            long accum = readerAccum;

            if (stereo == 0) {
                for (int n = 0; n < count; n++) {
                    long s = accum >> sampleShift;
                    accum -= accum >> bassShift;
                    accum += buffer[n];
                    dest[n] = (short) Math.clamp(s, -32767, 32767);
                }
            } else {
                for (int n = 0; n < count; n++) {
                    long s = accum >> sampleShift;
                    accum -= accum >> bassShift;
                    accum += buffer[n];
                    dest[n * 2] = (short) Math.clamp(s, -32767, 32767);
                }
            }

            readerAccum = accum;
            removeSamples(count);
        }
        return count;
    }

    // Clear buffer
    public void clear(int entireBuffer) {
        offset = 0;
        readerAccum = 0;
        if (buffer != null) {
            long count = entireBuffer != 0 ? bufferSize : samplesAvail();
            Arrays.fill(buffer, 0, (int) (count + BUFFER_EXTRA), 0);
        }
    }

    // Remove samples
    public void removeSamples(long count) {
        if (count > 0) {
            removeSilence(count);
            long remain = samplesAvail() + BUFFER_EXTRA;
            System.arraycopy(buffer, (int) count, buffer, 0, (int) remain);
            Arrays.fill(buffer, (int) remain, (int) (remain + count), 0);
        }
    }

    // Remove silence
    public void removeSilence(long count) {
        offset -= count << BLIP_BUFFER_ACCURACY;
    }

    // Get samples available
    public long samplesAvail() {
        return offset >> BLIP_BUFFER_ACCURACY;
    }

    // Get clock rate factor
    public long clockRateFactor(long clockRate) {
        double ratio = (double) sampleRate / clockRate;
        return (long) Math.floor(ratio * (1L << BLIP_BUFFER_ACCURACY) + 0.5);
    }
}