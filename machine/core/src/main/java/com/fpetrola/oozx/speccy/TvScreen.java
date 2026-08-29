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

import java.awt.image.BufferedImage;

/**
 * How the picture reached the television, as a filter over the frame the machine drew.
 * <p>
 * A Spectrum plugged into an aerial socket does not look like a Spectrum on a monitor, and the
 * difference is not nostalgia: a composite signal carries colour on a subcarrier with a fraction
 * of the bandwidth it carries brightness on, so colour spreads sideways across a scan line while
 * edges of brightness stay where they are. That is why a red word on blue smears to its right on
 * a telly and does not on a monitor, and it is most of what people recognise as "how it looked".
 * <p>
 * WHAT THIS IS AND IS NOT. It filters the finished frame, per scan line, which is the right shape
 * for that spreading: the artefacts of a composite signal are horizontal, along the line. It is
 * not a signal simulation - the machine hands over a frame of colour numbers, not a waveform, so
 * nothing here can depend on the phase between the pixel clock and the colour subcarrier. What it
 * can do, and does, is the bandwidth: blur the colour hard, the brightness barely, and the eye
 * reads the rest.
 * <p>
 * PAL rather than NTSC, because that is what these machines were sold into, so the colour axes
 * are U and V.
 */
public enum TvScreen {

  /** Straight from the machine: what the display file says, and nothing between. */
  RGB_MONITOR("RGB Monitor", 0, 0, 0, 0),

  /**
   * A scart lead carries colour separately, so it keeps nearly all of it; what it loses is the
   * last of the sharpness, which is why this is a shade softer than the monitor and no more.
   */
  SCART("Scart (RGB)", 0.4, 1.2, 0, 0),

  /**
   * One wire for everything. Brightness survives; colour is carried on a subcarrier with about a
   * quarter of the bandwidth, so it arrives spread over several pixels and every colour boundary
   * is a gradient. This is the one that looks like a television.
   */
  COMPOSITE("Composite Video", 0.7, 4.0, 0, 0),

  /**
   * Modulated up to a channel and demodulated back down by a 1980s tuner, which costs more
   * bandwidth again and adds what a tuner adds: overshoot at hard edges, and snow.
   */
  AERIAL("Aerial (RF)", 1.5, 6.5, 0.5, 4);

  private final String label;
  /** How far brightness spreads sideways, in pixels. */
  private final double lumaSpread;
  /** How far colour spreads sideways, in pixels; the whole point is that it is the larger one. */
  private final double chromaSpread;
  /** Overshoot at a brightness edge, as a fraction: what a tuner rings with. */
  private final double ringing;
  /** Snow, as the amplitude of the noise added to brightness. */
  private final int snow;
  /**
   * The gentle spreads, worked out once here rather than once a scan line.
   * <p>
   * Null where a box carries the spread instead. Building the weights per line meant an
   * allocation and a handful of exponentials for every one of the 236 of them, and cost more
   * than the filtering: 5.6 ms a frame for the aerial instead of 2.8.
   */
  private final double[] lumaWeights;
  private final double[] chromaWeights;

  TvScreen(String label, double lumaSpread, double chromaSpread, double ringing, int snow) {
    this.label = label;
    this.lumaSpread = lumaSpread;
    this.chromaSpread = chromaSpread;
    this.ringing = ringing;
    this.snow = snow;
    this.lumaWeights = weightsFor(lumaSpread);
    this.chromaWeights = weightsFor(chromaSpread);
  }

  /** The gaussian for a spread too gentle for a box, or null when a box will carry it. */
  private static double[] weightsFor(double sigma) {
    if (sigma <= 0 || sigma >= 1) {
      return null;
    }
    int reach = Math.max(1, (int) Math.ceil(sigma * 3));
    double[] weights = new double[reach + 1];
    double total = 0;
    for (int d = 0; d <= reach; d++) {
      weights[d] = Math.exp(-(d * d) / (2 * sigma * sigma));
      total += d == 0 ? weights[d] : weights[d] * 2;
    }
    for (int d = 0; d <= reach; d++) {
      weights[d] /= total;
    }
    return weights;
  }

  public String label() {
    return label;
  }

  /** Nothing to do: the picture is already what the machine drew. */
  public boolean isTransparent() {
    return lumaSpread == 0 && chromaSpread == 0 && ringing == 0 && snow == 0;
  }

  public static TvScreen byName(String name) {
    for (TvScreen screen : values()) {
      if (screen.name().equalsIgnoreCase(name) || screen.label.equalsIgnoreCase(name)) {
        return screen;
      }
    }
    return RGB_MONITOR;
  }

  /**
   * Puts the frame through the lead, in place.
   * <p>
   * Before it is scaled up, on purpose: the spreading happens in the machine's own pixels, and a
   * blur measured in screen pixels of a window someone resized would be measuring the window.
   *
   * @param work scratch the size of one line, kept by the caller so a frame costs no allocation
   */
  public void apply(BufferedImage frame, Scratch work) {
    if (isTransparent()) {
      return;
    }
    int width = frame.getWidth();
    int height = frame.getHeight();
    work.sizeFor(width);
    double[] y = work.y, u = work.u, v = work.v, sharp = work.sharp;
    // The image's own pixels where it keeps them as ints, which is every buffer this draws into.
    // Going through getRGB and setRGB asks the colour model to take each row apart and put it
    // back together, and at fifty frames a second that was most of the cost of the filter.
    int[] pixels = directPixels(frame);
    int[] line = pixels == null ? work.line : pixels;

    for (int row = 0; row < height; row++) {
      int at = row * width;
      if (pixels == null) {
        frame.getRGB(0, row, width, 1, line, 0, width);
        at = 0;
      }
      for (int x = 0; x < width; x++) {
        int rgb = line[at + x];
        double r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        double luma = 0.299 * r + 0.587 * g + 0.114 * b;
        y[x] = luma;
        u[x] = 0.492 * (b - luma);
        v[x] = 0.877 * (r - luma);
      }

      spread(y, work.pass, width, lumaSpread, lumaWeights);
      spread(u, work.pass, width, chromaSpread, chromaWeights);
      spread(v, work.pass, width, chromaSpread, chromaWeights);
      if (ringing > 0) {
        // A tuner overshoots what it could not pass: an edge comes back with a bright lip on one
        // side of it and a dark one on the other.
        //
        // Against a WIDER blur of what is already blurred, not against the picture before it.
        // Ringing the original back in undoes the very bandwidth this is here to take away, and
        // the aerial then came out sharper than the composite lead - which is backwards, and is
        // what the first version of this did.
        System.arraycopy(y, 0, sharp, 0, width);
        spread(sharp, work.pass, width, lumaSpread * 2 + 1, null);
        for (int x = 0; x < width; x++) {
          y[x] += ringing * (y[x] - sharp[x]) * 2;
        }
      }
      if (snow > 0) {
        for (int x = 0; x < width; x++) {
          y[x] += work.noise.nextInt(snow * 2 + 1) - snow;
        }
      }

      for (int x = 0; x < width; x++) {
        double luma = y[x], cu = u[x], cv = v[x];
        line[at + x] = 0xFF000000
            | clamp(luma + 1.140 * cv) << 16
            | clamp(luma - 0.395 * cu - 0.581 * cv) << 8
            | clamp(luma + 2.032 * cu);
      }
      if (pixels == null) {
        frame.setRGB(0, row, width, 1, line, 0, width);
      }
    }
  }

  /** The image's pixels themselves, or null for an image that does not keep them as ints. */
  private static int[] directPixels(BufferedImage frame) {
    int type = frame.getType();
    if (type != BufferedImage.TYPE_INT_RGB && type != BufferedImage.TYPE_INT_ARGB) {
      return null;
    }
    java.awt.image.DataBuffer buffer = frame.getRaster().getDataBuffer();
    return buffer instanceof java.awt.image.DataBufferInt ints ? ints.getData() : null;
  }

  /**
   * Spreads a line sideways by the given radius.
   * <p>
   * Three box blurs rather than a gaussian, which is the usual trade: it is a gaussian to the eye
   * by the third pass, and each pass is a running total, so the cost does not grow with the
   * radius - and the radius here is the whole point of the difference between a scart lead and an
   * aerial.
   */
  private static void spread(double[] values, double[] pass, int width, double radius,
                             double[] weights) {
    if (radius <= 0) {
      return;
    }
    if (weights != null) {
      // A box is a whole pixel wide at least, and the gentle cases - a scart lead, the
      // brightness down a composite one - want less than that.
      //
      // A gaussian and not three taps weighted by the radius, which is what this was: three taps
      // stop blurring and start INVERTING once the sides pass a quarter each, so asking for more
      // blur gave less of it and then more again with the sign turned over. On a picture with a
      // pixel of dither in it - which is most of them - a composite lead came out sharper than a
      // scart one, and the ordering these are for was gone.
      gaussian(values, pass, width, weights);
      return;
    }
    int box = (int) Math.round(radius);
    for (int i = 0; i < 3; i++) {
      blur(values, pass, width, box);
    }
  }

  /** A small explicit gaussian, for the spreads too gentle for a box to carry. */
  private static void gaussian(double[] values, double[] pass, int width, double[] weights) {
    int reach = weights.length - 1;
    for (int x = 0; x < width; x++) {
      double sum = weights[0] * values[x];
      for (int d = 1; d <= reach; d++) {
        sum += weights[d] * (values[Math.max(0, x - d)] + values[Math.min(width - 1, x + d)]);
      }
      pass[x] = sum;
    }
    System.arraycopy(pass, 0, values, 0, width);
  }

  private static void blur(double[] values, double[] pass, int width, int radius) {
    int span = radius * 2 + 1;
    double total = values[0] * radius;
    for (int x = 0; x <= radius && x < width; x++) {
      total += values[x];
    }
    // The line runs off both ends into more of what is there, which is what the edge of a picture
    // does; letting it run into nothing would draw a dark rim down both sides of the screen.
    total += values[width - 1] * Math.max(0, radius - (width - 1));
    for (int x = 0; x < width; x++) {
      pass[x] = total / span;
      double leaving = values[Math.max(0, x - radius)];
      double arriving = values[Math.min(width - 1, x + radius + 1)];
      total += arriving - leaving;
    }
    System.arraycopy(pass, 0, values, 0, width);
  }

  private static int clamp(double value) {
    return value <= 0 ? 0 : value >= 255 ? 255 : (int) (value + 0.5);
  }

  /**
   * The room a frame needs to be filtered, kept between frames.
   * <p>
   * Fifty frames a second of a quarter of a megabyte each is not something to ask a garbage
   * collector to absorb when the same arrays will do.
   */
  public static class Scratch {
    double[] y = new double[0], u = new double[0], v = new double[0];
    double[] sharp = new double[0], pass = new double[0];
    int[] line = new int[0];
    final java.util.Random noise = new java.util.Random(20250829);

    void sizeFor(int width) {
      if (y.length >= width) {
        return;
      }
      y = new double[width];
      u = new double[width];
      v = new double[width];
      sharp = new double[width];
      pass = new double[width];
      line = new int[width];
    }
  }
}
