package com.fpetrola.z80.bytecode.tests.rzzx;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class JSWMethodRenamer {

    static Map<Integer,String> ROUTINES = new HashMap<>();

    static {
        ROUTINES.put(33792,"gameJustLoaded");
        ROUTINES.put(34463,"giveTwoChancesForCodeEntry");
        ROUTINES.put(34499,"displayCodeEntryScreen");
        ROUTINES.put(34620,"readKeyboardDuringCodeEntry");
        ROUTINES.put(34762,"displayTitleScreenAndTheme");
        ROUTINES.put(35068,"startGame");
        ROUTINES.put(35090,"initialiseCurrentRoom");
        ROUTINES.put(35211,"drawRemainingLives");
        ROUTINES.put(35245,"mainLoopPart1");
        ROUTINES.put(35563,"cycleInkAndPaperColours");
        ROUTINES.put(35591,"mainLoopPart2");
        ROUTINES.put(35841,"loseLife");
        ROUTINES.put(35914,"displayGameOverSequence");
        ROUTINES.put(36147,"drawCurrentRoomToScreenBuffer");
        ROUTINES.put(36203,"fillAttributeBufferForCurrentRoom");
        ROUTINES.put(36288,"copyRoomAttributeByte");
        ROUTINES.put(36307,"moveWillyPart1");
        ROUTINES.put(36564,"moveWillyPart2");
        ROUTINES.put(36796,"moveWillyPart3");
        ROUTINES.put(37046,"killWilly");
        ROUTINES.put(37056,"moveRopeAndGuardians");
        ROUTINES.put(37310,"drawRopeArrowsGuardians");
        ROUTINES.put(37841,"drawItemsAndCollect");
        ROUTINES.put(37974,"drawSprite");
        ROUTINES.put(38026,"moveWillyToRoomLeft");
        ROUTINES.put(38046,"moveWillyToRoomRight");
        ROUTINES.put(38064,"moveWillyToRoomAbove");
        ROUTINES.put(38098,"moveWillyToRoomBelow");
        ROUTINES.put(38137,"moveConveyor");
        ROUTINES.put(38196,"dealWithSpecialRooms");
        ROUTINES.put(38276,"checkReachedToilet");
        ROUTINES.put(38298,"animateToilet");
        ROUTINES.put(38344,"checkAndSetWillySpriteAttributes");
        ROUTINES.put(38430,"checkSetAttributeForSpriteCell");
        ROUTINES.put(38455,"drawWillyToScreenBuffer");
        ROUTINES.put(38528,"printMessage");
        ROUTINES.put(38545,"printSingleCharacter");
        ROUTINES.put(38562,"playThemeTuneMoonlightSonata");
        ROUTINES.put(38601,"checkEnterZeroOrFirePressed");
        ROUTINES.put(38622,"playIntroMessageSound");
    }

    public static void main(String[] args) throws Exception {

        Path input = Paths.get("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/rzzx/JetSetWilly2Converted2_R.java");
        Path output = Paths.get("JetSetWilly2Converted2_Renamed.java");

        String code = Files.readString(input);

        Pattern methodPattern = Pattern.compile("\\$(\\d+)");
        Matcher matcher = methodPattern.matcher(code);

        Set<Integer> foundAddresses = new HashSet<>();

        while (matcher.find()) {
            int addr = Integer.parseInt(matcher.group(1));
            foundAddresses.add(addr);
        }

        Map<Integer,String> renameMap = new HashMap<>();

        for (int addr : foundAddresses) {

            String name = ROUTINES.get(addr);

            if (name == null) {
                name = "routine_" + addr;
            }

            renameMap.put(addr,name);
        }

        for (var entry : renameMap.entrySet()) {

            int addr = entry.getKey();
            String name = entry.getValue();

            code = code.replaceAll("\\$"+addr+"\\(", name + "(");
        }

        Files.writeString(output,code);

        System.out.println("Converted methods: " + renameMap.size());
        renameMap.forEach((k,v)-> System.out.println("$"+k+" -> "+v));
    }
}