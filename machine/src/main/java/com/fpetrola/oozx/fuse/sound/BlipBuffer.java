package com.fpetrola.oozx.fuse.sound;/*
 * Blip_Buffer 0.4.0 - Java port
 * Original C++ by Shay Green, C port by Gergely Szasz, Java port by Grok (2025)
 * Band-limited sound synthesis and buffering
 *
 * Licensed under GNU LGPL 2.1
 */

public class BlipBuffer {
  // Configuración
  public static final int BLIP_BUFFER_ACCURACY = 16;
  public static final int BLIP_PHASE_BITS = 6;
  public static final int BLIP_WIDEST_IMPULSE = 16;
  public static final int BLIP_RES = 1 << BLIP_PHASE_BITS;

  public static final int BLIP_MED_QUALITY = 8;
  public static final int BLIP_GOOD_QUALITY = 12;
  public static final int BLIP_HIGH_QUALITY = 16;

  public static final int BLIP_SYNTH_QUALITY = BLIP_GOOD_QUALITY;
  public static final int BLIP_SYNTH_RANGE = 65535;
  public static final int BLIP_SYNTH_WIDTH = BLIP_SYNTH_QUALITY;

  public static final int BLIP_SAMPLE_BITS = 30;
  public static final int BLIP_UNSCALED = 65535;
  public static final int BLIP_MAX_LENGTH = 0;
  public static final int BUFFER_EXTRA = BLIP_WIDEST_IMPULSE + 2;

  // Campos
  public long factor;
  public long offset;
  private long[] buffer;
  private long bufferSize;
  private long readerAccum;
  private int bassShift;
  private long sampleRate;
  private long clockRate;
  private int bassFreq;
  private int length;

  public BlipBuffer() {
    factor = Long.MAX_VALUE;
    bassFreq = 16;
    length = 0;
  }

  public String setSampleRate(long samplesPerSec, int msecLength) {
    // === Cálculo seguro del tamaño del buffer ===
    long newSize = (Long.MAX_VALUE >>> BLIP_BUFFER_ACCURACY) - BUFFER_EXTRA - 64;
    if (msecLength != BLIP_MAX_LENGTH) {
      long s = (samplesPerSec * (msecLength + 1) + 999) / 1000;
      if (s < newSize) newSize = s;
    }

    // Aumentamos el tamaño para evitar overflow en resampleo
    long totalSize = newSize + BUFFER_EXTRA + 1024; // +1024 de margen de seguridad

    if (bufferSize != newSize || buffer == null || buffer.length < totalSize) {
      long[] newBuffer = new long[(int) totalSize];
      if (buffer != null) {
        System.arraycopy(buffer, 0, newBuffer, 0, Math.min(buffer.length, newBuffer.length));
      }
      buffer = newBuffer;
      if (buffer == null) return "Out of memory";
    }

    bufferSize = newSize;
    sampleRate = samplesPerSec;
    length = (int) (newSize * 1000 / samplesPerSec - 1);
    if (clockRate != 0) setClockRate(clockRate);
    setBassFreq(bassFreq);
    clear(true);
    return null;
  }

  public void setClockRate(long cps) {
    factor = clockRateFactor(cps);
    clockRate = cps;
  }

  public void endFrame(long t) {
    offset += t * factor;
  }

  public long readSamples(int[] out, int maxSamples, boolean stereo) {
    long count = samplesAvail();
    if (count > maxSamples) count = maxSamples;
    if (count == 0) return 0;

    int sampleShift = BLIP_SAMPLE_BITS - 16;
    int myBassShift = bassShift;
    long accum = readerAccum;
    int inPos = 0;

    if (!stereo) {
      for (int n = (int) count; n-- > 0; ) {
        long s = accum >> sampleShift;
        accum -= accum >> myBassShift;
        accum += buffer[inPos++];
        if ((short) s != s) {
          out[out.length - (int) count + n] = (byte) (0x7FFF - (s >> 24));
        } else {
          out[out.length - (int) count + n] = (byte) s;
        }
      }
    } else {
      int outPos = 0;
      for (int n = (int) count; n-- > 0; ) {
        long s = accum >> sampleShift;
        accum -= accum >> myBassShift;
        accum += buffer[inPos++];
        if ((short) s != s) {
          out[outPos] = (byte) (0x7FFF - (s >> 24));
        } else {
          out[outPos] = (byte) s;
        }
        outPos += 2;
      }
    }

    readerAccum = accum;
    removeSamples(count);
    return count;
  }

  public void setBassFreq(int frequency) {
    bassFreq = frequency;
    int shift = 31;
    if (frequency > 0) {
      shift = 13;
      long f = (frequency << 16) / sampleRate;
      while ((f >>= 1) != 0 && --shift > 0) ;
    }
    bassShift = shift;
  }

  public void clear(boolean entireBuffer) {
    offset = 0;
    readerAccum = 0;
    if (buffer != null) {
      long count = entireBuffer ? bufferSize : samplesAvail();
      for (int i = 0; i < count + BUFFER_EXTRA; i++) {
        buffer[i] = 0;
      }
    }
  }

  public long samplesAvail() {
    return offset >> BLIP_BUFFER_ACCURACY;
  }

  public void removeSamples(long count) {
    if (count > 0) {
      removeSilence(count);
      long remain = samplesAvail() + BUFFER_EXTRA;
      int remain1 = (int) remain;
      remain1 = (int) Math.min(remain1, buffer.length - count);
      System.arraycopy(buffer, (int) count, buffer, 0, remain1);
      for (int i = (int) remain; i < remain + count; i++) {
        if (i < buffer.length)
          buffer[i] = 0;
      }
    }
  }

  public void removeSilence(long count) {
    offset -= count << BLIP_BUFFER_ACCURACY;
  }

  public long clockRateFactor(long clockRate) {
    double ratio = (double) sampleRate / clockRate;
    long factor = (long) Math.floor(ratio * (1L << BLIP_BUFFER_ACCURACY) + 0.5);
    return factor;
  }

  // Getters
  public long getOffset() {
    return offset;
  }

  public long getFactor() {
    return factor;
  }

  public long[] getBuffer() {
    return buffer;
  }
}