package com.fpetrola.z80.routines;

import com.fpetrola.z80.bytecode.examples.RemoteZ80Translator;
import com.fpetrola.z80.transformations.StackAnalyzer;
import org.junit.Test;

import java.util.Set;

import static com.fpetrola.z80.routines.CodeVersions.Kind.*;
import static org.junit.Assert.*;

public class CodeVersionsTest {
  private final CodeVersions versions = new CodeVersions();

  private void site(int address, int[]... known) {
    for (int[] version : known)
      versions.record(address, known[0], version);
  }

  private CodeVersions.Kind kind(int address) {
    versions.decodeWith(RemoteZ80Translator.decoder());
    return versions.kinds().get(address);
  }

  private static int[] b(int... bytes) {
    return bytes;
  }

  @Test
  public void anImmediateThatChangesIsAnOperand() {
    site(0x838E, b(0xFE, 0x49), b(0xFE, 0x5A));
    assertEquals(OPERAND, kind(0x838E));
    assertEquals(Set.of(0x838F), versions.modifiedBytes());
    assertTrue(versions.instructionVersions(0x838E).isEmpty());
  }

  @Test
  public void anImmediatePatchedAtAFixedAddressIsAnOperand() {
    versions.patched(0x838E, b(0xFE, 0x49), Set.of(1));
    assertEquals(OPERAND, kind(0x838E));
    assertEquals(Set.of(0x838F), versions.modifiedBytes());
  }

  @Test
  public void anOpcodePatchedAtAFixedAddressIsAnInstructionSite() {
    versions.patched(0xD035, b(0x1C), Set.of(0));
    assertEquals(INSTRUCTION, kind(0xD035));
    assertEquals(1, versions.instructionVersions(0xD035).size());
  }

  @Test
  public void anOpcodeSwapIsAnInstructionSite() {
    site(0xD035, b(0x1C), b(0x14));
    assertEquals(INSTRUCTION, kind(0xD035));
    assertEquals(2, versions.instructionVersions(0xD035).size());
  }

  @Test
  public void theTargetsOfARewrittenCallAreSuccessors() {
    site(0xD015, b(0xCD, 0x7B, 0xD0), b(0xCD, 0x3B, 0xD0));
    assertEquals(INSTRUCTION, kind(0xD015));
    assertEquals(Set.of(0xD03B, 0xD07B), versions.successors());
  }

  @Test
  public void theTargetsOfARewrittenConditionalJumpAreSuccessors() {
    site(0xCEC1, b(0xDA, 0xC4, 0xCE), b(0xDA, 0xFC, 0xCE));
    assertEquals(INSTRUCTION, kind(0xCEC1));
    assertEquals(Set.of(0xCEC4, 0xCEFC), versions.successors());
  }

  @Test
  public void aLengthChangeMakesABlockThatSpreadsToItsNeighbours() {
    site(0x9BE2, b(0xC3, 0xEA, 0x9B), b(0x29));
    site(0x9BE3, b(0x17), b(0x29));
    assertEquals(BLOCK, kind(0x9BE2));
    assertEquals(BLOCK, kind(0x9BE3));
    assertEquals(Set.of(0x9BE2, 0x9BE3, 0x9BE4), versions.modifiedBytes());
  }

  @Test
  public void aBlockDoesNotSpreadToADistantSite() {
    site(0x9BE2, b(0xC3, 0xEA, 0x9B), b(0x29));
    site(0xA000, b(0x1C), b(0x14));
    assertEquals(BLOCK, kind(0x9BE2));
    assertEquals(INSTRUCTION, kind(0xA000));
  }

  @Test
  public void aDecodedJumpKnowsItsTargetWithoutBeingExecuted() {
    versions.decodeWith(RemoteZ80Translator.decoder());
    assertEquals(0xD07B, RoutineManager.fixedJumpTarget(versions.decode(0xD015, b(0xCD, 0x7B, 0xD0))));
    assertEquals(0xD017, RoutineManager.fixedJumpTarget(versions.decode(0xD010, b(0x18, 0x05))));
  }

  @Test
  public void aRepeatedVersionIsKeptOnce() {
    site(0x8000, b(0x3E, 1), b(0x3E, 2), b(0x3E, 2));
    assertEquals(2, versions.versions().get(0x8000).size());
  }

  @Test
  public void learningFromAnAnalyzerWithoutVersionsKeepsTheOnesAlreadyKnown() {
    StackAnalyzer footprint = new StackAnalyzer(null);
    footprint.codeVersions.record(0xD015, b(0xCD, 0x7B, 0xD0), b(0xCD, 0x3B, 0xD0));
    footprint.learnFrom(new StackAnalyzer(null));
    assertTrue(footprint.codeVersions.isVersioned(0xD015));
  }

  @Test
  public void learningFromARecordingBringsItsVersions() {
    StackAnalyzer recorded = new StackAnalyzer(null);
    recorded.codeVersions.record(0xD015, b(0xCD, 0x7B, 0xD0), b(0xCD, 0x3B, 0xD0));
    StackAnalyzer translation = new StackAnalyzer(null);
    translation.learnFrom(recorded);
    assertTrue(translation.codeVersions.isVersioned(0xD015));
  }

  @Test
  public void aBlockRegionSpansItsSitesUpToTheirLongestVersion() {
    site(0x9BE2, b(0xC3, 0xEA, 0x9B), b(0x29));
    site(0x9BE3, b(0x17), b(0x29));
    site(0xA000, b(0x1C), b(0x14));
    kind(0x9BE2);
    assertEquals(1, versions.blockRegions().size());
    assertArrayEquals(b(0x9BE2, 0x9BE5), versions.blockRegions().get(0));
  }

  @Test
  public void theContentOfABlockIsRecordedEachTimeItsCodeRunsWithANewShape() {
    site(0x9BE2, b(0xC3, 0xEA, 0x9B), b(0x29));
    kind(0x9BE2);
    int[] memory = new int[0x10000];
    memory[0x9BE2] = 0xC3;
    versions.recordBlockContent(0x9BE2, memory);
    versions.recordBlockContent(0x9BE3, memory);
    versions.recordBlockContent(0xA000, memory);
    assertEquals(1, versions.blockContents(0x9BE2).size());
    memory[0x9BE2] = 0x29;
    versions.recordBlockContent(0x9BE2, memory);
    assertEquals(2, versions.blockContents(0x9BE2).size());
    assertArrayEquals(b(0x29, 0, 0), versions.blockContents(0x9BE2).get(1));
  }

  @Test
  public void theBlockRegionsOfOneTemplateMergeSoTheirContentsAreRecordedTogether() {
    site(0x9BDA, b(0x79), b(0x26, 0xFD));
    site(0x9BE2, b(0xC3, 0xEA, 0x9B), b(0x29));
    site(0xA000, b(0x1C), b(0x14), b(0x00, 0x00));
    kind(0x9BDA);
    assertEquals(3, versions.blockRegions().size());
    versions.mergeBlockRegions(0x9BBF, 0x9C1D);
    assertEquals(2, versions.blockRegions().size());
    assertTrue(versions.blockRegions().stream().anyMatch(region -> region[0] == 0x9BDA && region[1] == 0x9BE5));
  }
}
