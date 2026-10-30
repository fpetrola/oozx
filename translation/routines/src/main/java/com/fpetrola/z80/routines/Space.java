package com.fpetrola.z80.routines;

/**
 * A code address with the space its code lives in: space 0 is the main image; a bank paged into the window or a content copied over
 * a region is another space, so one Z80 address can hold several codes. The space travels in the bits above the 16 of the address,
 * and every step from an address to the next keeps it.
 */
public final class Space {
  private Space() {
  }

  /** The Z80 address of the key, for reading the image. */
  public static int address(int key) {
    return key & 0xffff;
  }

  /** The address {@code bytes} further on in the same space, wrapping at FFFF as the Z80 does. */
  public static int next(int key, int bytes) {
    return key & ~0xffff | key + bytes & 0xffff;
  }
}
