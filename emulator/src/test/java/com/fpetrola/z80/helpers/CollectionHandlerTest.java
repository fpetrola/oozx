package com.fpetrola.z80.helpers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The items as an array kept in step with the collection: what a memory access walks to tell its listeners, with no
 * lambda made per access (one per write was a gigabyte of garbage a second in oozx's 3D viewer, its rooms' eight
 * machines running at twice the speed).
 */
class CollectionHandlerTest {
  @Test
  void theItemsFollowEveryChange() {
    CollectionHandler<String> h = new CollectionHandler<>();
    assertEquals(List.of(), List.of(h.items()));
    h.add("a");
    assertEquals(List.of("a"), List.of(h.items()));
    h.add("b");
    h.add("c");
    assertEquals(List.of("a", "b", "c"), List.of(h.items()));
    h.remove("b");
    assertEquals(List.of("a", "c"), List.of(h.items()));
    h.disable();
    assertEquals(List.of(), List.of(h.items()));
    h.enable();
    assertEquals(List.of("a", "c"), List.of(h.items()));
    h.removeAll();
    assertEquals(List.of(), List.of(h.items()));
  }

  @Test
  void theSameArrayWhileNothingChanges() {
    CollectionHandler<String> h = new CollectionHandler<>();
    h.add("a");
    org.junit.jupiter.api.Assertions.assertSame(h.items(), h.items());
  }
}
