package com.fpetrola.oozx.speccy.machine;

/** How a model measures its frame: the processor's clock in Hz, and what its ULA was measured to do. */
public record MachineTimings(long processorSpeed, Frame frame) {

  /** Something measured in four parts laid end to end: a border, the picture, the other border, the retrace. */
  public record Span(int before, int picture, int after, int retrace) {
    public int length() {
      return before + picture + after + retrace;
    }
  }

  /**
   * A ULA's frame: a line in T-states, a frame in lines, how long /INT is held, and the T-state of
   * the picture's first pixel as it was measured - a datum, not the border's lines times a line.
   */
  public record Frame(Span line, Span lines, int interruptLength, int firstPixel) {
  }

  public static final Frame FERRANTI_5C_6C = new Frame(new Span(24, 128, 24, 48), new Span(48, 192, 48, 24), 32, 14336);
  public static final Frame FERRANTI_60HZ = new Frame(new Span(24, 128, 24, 48), new Span(24, 192, 25, 23), 32, 8960);
  public static final Frame FERRANTI_7C = new Frame(new Span(24, 128, 24, 52), new Span(48, 192, 48, 23), 36, 14362);
  public static final Frame AMSTRAD_ASIC = new Frame(new Span(24, 128, 24, 52), new Span(48, 192, 48, 23), 32, 14365);
  /** 224 clocks over 320 lines is 71680 to a frame, which at 3.584MHz is fifty of them a second. */
  public static final Frame PENTAGON = new Frame(new Span(36, 128, 28, 32), new Span(64, 192, 48, 16), 36, 17988);

  public int tstatesPerLine() {
    return frame.line.length();
  }

  public int linesPerFrame() {
    return frame.lines.length();
  }

  public int tstatesPerFrame() {
    return tstatesPerLine() * linesPerFrame();
  }

  public int interruptLength() {
    return frame.interruptLength;
  }

  public int firstPixel() {
    return frame.firstPixel;
  }

  /** The same model in a unit that starts the picture one T-state later, which is all that differs. */
  public MachineTimings late() {
    return new MachineTimings(processorSpeed, new Frame(frame.line, frame.lines, frame.interruptLength, frame.firstPixel + 1));
  }
}
