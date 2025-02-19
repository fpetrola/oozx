package com.fpetrola.z80.minizx.emulation;

/**
 * Notified on every port access. Used to live in the emulator as a generic
 * {@code OutListener<T>}, but {@code 0.0.2-alu} dropped that: its {@code IO} only has
 * {@code int in(int)}/{@code void out(int, int)}, no listener mechanism. Moving it here loses
 * nothing, since the list-keeping and notifying was always done by
 * {@link com.fpetrola.z80.minizx.RZXPlayerIO} in this module — the emulator only lent the
 * interface. The package stays as-is because {@code TaintReplay} references it by full name and
 * generated game variants import {@code com.fpetrola.z80.minizx.*} by name. The value is plain
 * {@code int}, not generic, matching what alu moves through its ports and what the taint's
 * port-to-routine attribution already works in.
 */
public interface OutListener {
  void outAt(int port, int value);
}
