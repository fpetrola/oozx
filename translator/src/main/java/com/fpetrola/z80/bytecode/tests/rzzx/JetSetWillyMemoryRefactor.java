package com.fpetrola.z80.bytecode.tests.rzzx;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;

public class JetSetWillyMemoryRefactor {

  private static final Pattern MEM_PATTERN = Pattern.compile("mem\\[(\\d+)]");

  private static final Map<Integer, String> ADDRESS_NAMES = new HashMap<>();

  static {
    ADDRESS_NAMES.put(33824, "currentRoomNumber");
    ADDRESS_NAMES.put(34251, "minuteCounter");
    ADDRESS_NAMES.put(34252, "livesRemaining");
    ADDRESS_NAMES.put(34253, "screenFlashCounter");
    ADDRESS_NAMES.put(34254, "kempstonJoystickIndicator");
    ADDRESS_NAMES.put(34255, "willyYCoordinate");
    ADDRESS_NAMES.put(34256, "willyDirectionAndMovementFlags");
    ADDRESS_NAMES.put(34257, "airborneStatusIndicator");
    ADDRESS_NAMES.put(34258, "willyAnimationFrame");
    ADDRESS_NAMES.put(34262, "ropeStatusIndicator");
    ADDRESS_NAMES.put(34270, "itemsRemainingComplement");
    ADDRESS_NAMES.put(34271, "gameModeIndicator");
    ADDRESS_NAMES.put(34272, "inactivityTimer");
    ADDRESS_NAMES.put(34273, "inGameMusicNoteIndex");
    ADDRESS_NAMES.put(34274, "musicFlags");
    ADDRESS_NAMES.put(34275, "writetyperKeyCounter");
    ADDRESS_NAMES.put(34276, "temporaryVariable");
    ADDRESS_NAMES.put(32990, "borderColour");
  }

  public static void main(String[] args) throws Exception {

    Path input = Paths.get("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/rzzx/JetSetWilly2Converted2.java");
    Path output = Paths.get("JetSetWilly2Converted2_R.java");

    String code = Files.readString(input);

    Matcher matcher = MEM_PATTERN.matcher(code);

    Set<Integer> foundAddresses = new HashSet<>();

    while (matcher.find()) {
      foundAddresses.add(Integer.parseInt(matcher.group(1)));
    }

    String refactored = code;

    HashSet<Integer> integers = new HashSet<>(foundAddresses);
    for (Integer address : integers) {

      String property = ADDRESS_NAMES.get(address);

      if (property != null) {
//        property = "mem_" + address;

        refactored = refactored.replaceAll(
            "mem\\[" + address + "]",
            property
        );
      } else
        foundAddresses.remove(address);
    }

    String properties = generateProperties(foundAddresses);

    refactored = insertProperties(refactored, properties);

    Files.writeString(output, refactored);

    System.out.println("Refactor complete.");
  }

  private static String generateProperties(Set<Integer> addresses) {

    StringBuilder sb = new StringBuilder();
    sb.append("\n    // Generated memory properties\n");

    for (Integer address : addresses) {

      String name = ADDRESS_NAMES.get(address);

      if (name != null) {
//        name = "mem_" + address;

        sb.append("    private int ")
            .append(name)
            .append("; // ")
            .append(address)
            .append("\n");
      }
    }

    return sb.toString();
  }

  private static String insertProperties(String code, String properties) {

    int classIndex = code.indexOf("{");

    if (classIndex == -1)
      return code;

    return code.substring(0, classIndex + 1)
           + properties
           + code.substring(classIndex + 1);
  }
}