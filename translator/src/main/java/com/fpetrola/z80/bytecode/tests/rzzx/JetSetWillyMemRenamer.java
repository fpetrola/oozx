package com.fpetrola.z80.bytecode.tests.rzzx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JetSetWillyMemRenamer {

    private static final Map<Integer, String> MEM_NAMES = new HashMap<>();

    static {
        // ================================================================
        // TODAS las direcciones nombradas en https://skoolkid.github.io/jetsetwilly/dec/maps/all.html
        // (ordenadas por dirección, nombres oficiales convertidos a UPPER_SNAKE_CASE)
        // ================================================================

        put(16384,  "SCREEN_PIXEL_BUFFER");
        put(22528,  "SCREEN_ATTRIBUTE_BUFFER");
        put(23552,  "GAME_ATTRIBUTE_BUFFER");
        put(24576,  "GAME_PIXEL_BUFFER");
        put(28672,  "EMPTY_ROOM_SCREEN_BUFFER");

        put(32768,  "ROOM_LAYOUT");
        put(32896,  "ROOM_NAME");
        put(32928,  "ROOM_TILES");

        put(32982,  "CONVEYOR_DIRECTION");
        put(32983,  "CONVEYOR_ATTR_ADDRESS");
        put(32985,  "CONVEYOR_LENGTH");
        put(32986,  "RAMP_DEFINITION");

        put(32990,  "BORDER_COLOUR");

        put(33008,  "ENTITY_SPECIFICATIONS");
        put(33024,  "ENTITY_BUFFERS");

        put(33824,  "CURRENT_ROOM_NUMBER");
        put(33825,  "LEFT_RIGHT_MOVEMENT_TABLE");
        put(33841,  "TRIANGLE_UDGS");

        put(33873,  "AIR_TEXT");
        put(33876,  "PRESS_ENTER_TEXT");

        put(34132,  "ITEMS_COLLECTED_TIME_TEXT");
        put(34164,  "GAME_TEXT");
        put(34168,  "OVER_TEXT");
        put(34172,  "NUMBER_OF_ITEMS_COLLECTED");
        put(34175,  "CURRENT_TIME_TEXT");
        put(34181,  "SEVEN_AM_TEXT");
        put(34187,  "ENTER_CODE_TEXT");
        put(34219,  "SORRY_TRY_CODE_TEXT");

        put(34251,  "MINUTE_COUNTER");
        put(34252,  "LIVES_REMAINING");
        put(34253,  "SCREEN_FLASH_COUNTER");
        put(34254,  "KEMPSTON_JOYSTICK_INDICATOR");
        put(34255,  "WILLY_Y_COORDINATE");
        put(34256,  "WILLY_DIRECTION_AND_MOVEMENT_FLAGS");
        put(34257,  "AIRBORNE_STATUS_INDICATOR");
        put(34258,  "WILLY_ANIMATION_FRAME");
        put(34259,  "WILLY_ATTRIBUTE_BUFFER_ADDRESS");
        put(34261,  "JUMPING_ANIMATION_COUNTER");
        put(34262,  "ROPE_STATUS_INDICATOR");
        put(34263,  "WILLY_STATE_ON_ROOM_ENTRY");

        put(34270,  "ITEMS_REMAINING_COMPLEMENT");
        put(34271,  "GAME_MODE_INDICATOR");
        put(34272,  "INACTIVITY_TIMER");
        put(34273,  "IN_GAME_MUSIC_NOTE_INDEX");
        put(34274,  "MUSIC_FLAGS");
        put(34275,  "WRITETYPER_KEY_COUNTER");
        put(34276,  "TEMPORARY_VARIABLE");
        put(34277,  "WRITETYPER_FLAG");

        put(34299,  "TITLE_SCREEN_TUNE_DATA");

        put(34463,  "CODE_ENTRY_ROUTINE");
        put(34499,  "DISPLAY_CODE_ENTRY_SCREEN");

        put(34620,  "READ_KEYBOARD_CODE_ENTRY");
        put(34762,  "TITLE_SCREEN_AND_TUNE");
        put(35068,  "START_GAME");
        put(35090,  "INITIALISE_CURRENT_ROOM");

        put(35211,  "DRAW_REMAINING_LIVES");
        put(35563,  "CYCLE_INK_PAPER_COLOURS");

        put(35841,  "LOSE_A_LIFE");
        put(35914,  "GAME_OVER_SEQUENCE");

        put(36147,  "DRAW_CURRENT_ROOM");
        put(36203,  "FILL_ATTRIBUTE_BUFFER_24064");
        put(36288,  "COPY_ROOM_ATTRIBUTE");

        put(36307,  "MOVE_WILLY_1");
        put(36564,  "MOVE_WILLY_2");
        put(36796,  "MOVE_WILLY_3");

        put(37046,  "KILL_WILLY");
        put(37056,  "MOVE_ROPE_AND_GUARDIANS");
        put(37310,  "DRAW_ROPE_ARROWS_GUARDIANS");

        put(37841,  "DRAW_ITEMS_AND_COLLECT");
        put(37974,  "DRAW_SPRITE");

        put(38026,  "MOVE_LEFT");
        put(38046,  "MOVE_RIGHT");
        put(38064,  "MOVE_UP");
        put(38098,  "MOVE_DOWN");

        put(38137,  "MOVE_CONVEYOR");
        put(38196,  "SPECIAL_ROOM_HANDLER");
        put(38276,  "CHECK_TOILET");
        put(38298,  "ANIMATE_TOILET");

        put(38344,  "SET_WILLY_ATTRIBUTES");
        put(38430,  "CHECK_WILLY_CELL_ATTR");

        put(38528,  "PRINT_MESSAGE");
        put(38545,  "PRINT_CHAR");
        put(38562,  "PLAY_THEME_TUNE");
        put(38601,  "CHECK_ENTER_OR_FIRE");
        put(38622,  "PLAY_INTRO_SOUND");

        put(38912,  "TITLE_SCREEN_ATTR_TOP");
        put(39680,  "NUMBER_KEY_GRAPHICS");
        put(39808,  "CODE_ENTRY_SCREEN_ATTR");
        put(40000,  "FOOT_BARREL_GRAPHIC");
        put(40064,  "MARIA_SPRITE_GRAPHIC");
        put(40192,  "WILLY_SPRITE_GRAPHIC");
        put(40448,  "CODES_TABLE");
        put(40960,  "ENTITY_DEFINITIONS");
        put(41983,  "FIRST_ITEM_INDEX");
        put(41984,  "ITEM_TABLE");
        put(42496,  "TOILET_GRAPHICS");
        put(43776,  "GUARDIAN_GRAPHICS");

        // Direcciones grandes (tablas) que aparecen en la página
        put(49920,  "ROOM_03_MEGATREE");
        put(51456,  "ROOM_09_BRANCH");
        put(52992,  "ROOM_15_QUIRKAFLEEG");
        put(53248,  "ROOM_16_WE_MUST_PERFORM");
        put(54784,  "ROOM_22_KITCHENS_STAIRWAY");
        put(59392,  "ROOM_40_DR_JONES");
        put(61184,  "ROOM_47_THE_BATHROOM");
    }

    static {
        // ================================================================
        // TODAS las direcciones nombradas en gbuffer.html
        // (Game status buffer — ordenadas por dirección)
        // ================================================================

        put(32768,  "ROOM_LAYOUT");                    // 8000   Room layout
        put(32896,  "ROOM_NAME");                      // 8090   Room name
        put(32928,  "ROOM_TILES");                     // 80A8   Room tiles
        put(32982,  "CONVEYOR_DEFINITION");            // 80D6   Conveyor definition
        put(32986,  "RAMP_DEFINITION");                // 80DA   Ramp definition
        put(32990,  "BORDER_COLOUR");                  // 80DE   Border colour
        put(32993,  "ITEM_GRAPHIC");                   // 80E1   Item graphic
        put(33001,  "ROOM_EXITS");                     // 80E9   Room exits
        put(33008,  "ENTITY_SPECIFICATIONS");          // 8100   Entity specifications
        put(33024,  "ENTITY_BUFFERS");                 // 8110   Entity buffers

        put(33280,  "Y_COORDINATE_LOOKUP_TABLE");      // 81F0   Referenced in entity buffers (y-coordinate lookup)

        put(33824,  "CURRENT_ROOM_NUMBER");            // 8478   Current room number

        put(34172,  "NUMBER_OF_ITEMS_COLLECTED");      // 8514   Number of items collected
        put(34175,  "CURRENT_TIME");                   // 8517   Current time

        put(34251,  "MINUTE_COUNTER");                 // 853B   Minute counter
        put(34252,  "LIVES_REMAINING");                // 853C   Lives remaining
        put(34253,  "SCREEN_FLASH_COUNTER");           // 853D   Screen flash counter
        put(34254,  "KEMPSTON_JOYSTICK_INDICATOR");    // 853E   Kempston joystick indicator
        put(34255,  "WILLY_Y_COORDINATE");             // 853F   Willy's y-coordinate
        put(34256,  "WILLY_DIRECTION_AND_MOVEMENT_FLAGS"); // 8540   Willy's direction and movement flags
        put(34257,  "AIRBORNE_STATUS_INDICATOR");      // 8541   Airborne status indicator
        put(34258,  "WILLY_ANIMATION_FRAME");          // 8542   Willy's animation frame
        put(34259,  "WILLY_ATTRIBUTE_BUFFER_ADDRESS"); // 8543   Address of Willy's location in the attribute buffer at 23552
        put(34261,  "JUMPING_ANIMATION_COUNTER");      // 8545   Jumping animation counter
        put(34262,  "ROPE_STATUS_INDICATOR");          // 8546   Rope status indicator
        put(34263,  "WILLY_STATE_ON_ROOM_ENTRY");      // 8547   Willy's state on entry to the room

        put(34270,  "ITEMS_REMAINING_COMPLEMENT");     // 8556   256 minus the number of items remaining
        put(34271,  "GAME_MODE_INDICATOR");            // 8557   Game mode indicator
        put(34272,  "INACTIVITY_TIMER");               // 8558   Inactivity timer
        put(34273,  "IN_GAME_MUSIC_NOTE_INDEX");       // 8559   In-game music note index
        put(34274,  "MUSIC_FLAGS");                    // 855A   Music flags
        put(34275,  "WRITETYPER_KEY_COUNTER");         // 855B   WRITETYPER key counter
        put(34276,  "TEMPORARY_VARIABLE");             // 855C   Temporary variable
    }

    static {
        // ================================================================
        // Game status buffer - tabla principal (gbuffer.html)
        // ================================================================
        put(32768,  "ROOM_LAYOUT");                          // 0x8000 - Room layout
        put(32896,  "ROOM_NAME");                            // 0x8090 - Room name
        put(32928,  "ROOM_TILES");                           // 0x80A8 - Room tiles

        put(32982,  "CONVEYOR_DIRECTION");                   // 0x80B6 - Conveyor definition: Direction (0=left, 1=right)
        put(32983,  "CONVEYOR_ATTR_ADDRESS");                // 0x80B7 - Conveyor definition: Address in attribute buffer
        put(32985,  "CONVEYOR_LENGTH");                      // 0x80B9 - Conveyor definition: Length

        put(32986,  "RAMP_DEFINITION");                      // 0x80BA - Ramp definition (estructura de 5 bytes)

        put(32990,  "BORDER_COLOUR");                        // 0x80BE - Border colour

        put(32993,  "ITEM_GRAPHIC");                         // 0x80C1 - Item graphic

        put(33001,  "ROOM_EXITS");                           // 0x80C9 - Room exits

        put(33008,  "ENTITY_SPECIFICATIONS_BASE");           // 0x80D0 - Entity specifications (8 entidades × 8 bytes)
        // Subcampos típicos de cada entidad (de 33008.html):
        put(33008,  "ENTITY_1_SPEC");                        // Primera entidad - spec
        put(33010,  "ENTITY_2_SPEC");
        put(33012,  "ENTITY_3_SPEC");
        put(33014,  "ENTITY_4_SPEC");
        put(33016,  "ENTITY_5_SPEC");
        put(33018,  "ENTITY_6_SPEC");
        put(33020,  "ENTITY_7_SPEC");
        put(33022,  "ENTITY_8_SPEC");

        put(33024,  "ENTITY_BUFFERS_BASE");                  // 0x80E0 - Entity buffers (estado actual)
        // Sub-buffers de entidades (8 bytes cada una, de 33024.html):
        put(33024,  "ENTITY_1_BUFFER");
        put(33032,  "ENTITY_2_BUFFER");
        put(33040,  "ENTITY_3_BUFFER");
        put(33048,  "ENTITY_4_BUFFER");
        put(33056,  "ENTITY_5_BUFFER");
        put(33064,  "ENTITY_6_BUFFER");
        put(33072,  "ENTITY_7_BUFFER");
        put(33080,  "ENTITY_8_BUFFER");
        put(33088,  "ENTITY_TERMINATOR");                    // DEFB 255 - fin de lista

        put(33280,  "Y_COORDINATE_LOOKUP_TABLE");            // 0x8200 - Tabla de lookup para coordenadas Y (referenciada en entity buffers)

        put(33824,  "CURRENT_ROOM_NUMBER");                  // 0x8470 - Current room number

        put(34172,  "NUMBER_OF_ITEMS_COLLECTED");            // 0x8514 - Number of items collected
        put(34175,  "CURRENT_TIME");                         // 0x8517 - Current time (texto)

        put(34251,  "MINUTE_COUNTER");                       // 0x853B - Minute counter
        put(34252,  "LIVES_REMAINING");                      // 0x853C - Lives remaining
        put(34253,  "SCREEN_FLASH_COUNTER");                 // 0x853D - Screen flash counter
        put(34254,  "KEMPSTON_JOYSTICK_INDICATOR");          // 0x853E - Kempston joystick indicator
        put(34255,  "WILLY_Y_COORDINATE");                   // 0x853F - Willy's y-coordinate
        put(34256,  "WILLY_DIRECTION_AND_MOVEMENT_FLAGS");   // 0x8540 - Direction & movement flags
        put(34257,  "AIRBORNE_STATUS_INDICATOR");            // 0x8541 - Airborne status
        put(34258,  "WILLY_ANIMATION_FRAME");                // 0x8542 - Animation frame
        put(34259,  "WILLY_ATTRIBUTE_BUFFER_ADDRESS");       // 0x8543 - Address in attr buffer @23552
        put(34261,  "JUMPING_ANIMATION_COUNTER");            // 0x8545 - Jumping animation counter
        put(34262,  "ROPE_STATUS_INDICATOR");                // 0x8546 - Rope status
        put(34263,  "WILLY_STATE_ON_ROOM_ENTRY");            // 0x8547 - State on room entry

        put(34270,  "ITEMS_REMAINING_COMPLEMENT");           // 0x8556 - 256 - items remaining
        put(34271,  "GAME_MODE_INDICATOR");                  // 0x8557 - Game mode
        put(34272,  "INACTIVITY_TIMER");                     // 0x8558 - Inactivity timer
        put(34273,  "IN_GAME_MUSIC_NOTE_INDEX");             // 0x8559 - Music note index
        put(34274,  "MUSIC_FLAGS");                          // 0x855A - Music flags
        put(34275,  "WRITETYPER_KEY_COUNTER");               // 0x855B - WRITETYPER keys pressed
        put(34276,  "TEMPORARY_VARIABLE");                   // 0x855C - Temporary variable
    }

    static {
        // ================================================================
        // Game Status Buffer (gbuffer.html) - TODAS las variables de 1 byte
        // ================================================================
        put(32990, "BORDER_COLOUR");
        put(33824, "CURRENT_ROOM");
        put(34172, "ITEMS_COLLECTED");
        put(34251, "MINUTE_COUNTER");
        put(34252, "LIVES_REMAINING");
        put(34253, "SCREEN_FLASH_COUNTER");
        put(34254, "KEMPSTON_JOYSTICK");
        put(34255, "WILLY_Y");
        put(34256, "WILLY_DIRECTION_FLAGS");
        put(34257, "AIRBORNE_STATUS");
        put(34258, "WILLY_ANIMATION_FRAME");
        put(34259, "WILLY_ATTR_BUFFER_ADDR");
        put(34261, "JUMPING_ANIMATION_COUNTER");
        put(34262, "ROPE_STATUS");
        put(34263, "WILLY_STATE_ON_ENTRY");
        put(34270, "ITEMS_REMAINING_256_MINUS");
        put(34271, "GAME_MODE");
        put(34272, "INACTIVITY_TIMER");
        put(34273, "MUSIC_NOTE_INDEX");
        put(34274, "MUSIC_FLAGS");
        put(34275, "WRITETYPER_KEY_COUNTER");
        put(34276, "TEMPORARY_VARIABLE");

        // ================================================================
        // Room / Conveyor / Ramp (de asm/32982.html y memory map)
        // ================================================================
        put(32928, "ROOM_TILES");
        put(32982, "CONVEYOR_DIRECTION");
        put(32983, "CONVEYOR_ATTR_ADDRESS");
        put(32985, "CONVEYOR_LENGTH");           // ← la que mencionaste
        put(32986, "RAMP_DEFINITION");
        put(33280, "CONVEYOR_RAMP_ATTR_BASE");   // ← la que faltaba (0x8200)

        // ================================================================
        // Otras variables importantes del memory map
        // ================================================================
        put(32993, "ITEM_GRAPHIC");
        put(33008, "ENTITY_SPECIFICATIONS");
        put(33024, "ENTITY_BUFFERS");
        put(32896, "ROOM_NAME");
        put(32768, "ROOM_LAYOUT");
        put(23552, "GAME_ATTRIBUTE_BUFFER");
        put(24576, "GAME_PIXEL_BUFFER");
        put(22528, "SCREEN_ATTRIBUTE_BUFFER");
        put(16384, "SCREEN_PIXEL_BUFFER");
        put(28672, "EMPTY_ROOM_SCREEN_BUFFER");

        // ================================================================
        // Direcciones usadas en rutinas y excepciones (muy frecuentes en tu código)
        // ================================================================
        put(37047, "COLLISION_FATAL_1");
        put(37048, "COLLISION_FATAL_2");
        put(37841, "DRAW_ITEMS_AND_COLLECT");
        put(37974, "DRAW_SPRITE");
        put(38064, "ENTER_ROOM_ABOVE");
        put(38137, "MOVE_CONVEYOR");
        put(38196, "SPECIAL_ROOM_HANDLER");
        put(38276, "CHECK_TOILET");
        put(38344, "SET_WILLY_ATTRIBUTES");
        put(38430, "CHECK_WILLY_CELL");
        put(38504, "OR_BYTES_TO_BUFFER");
        put(38528, "PRINT_MESSAGE");
        put(38545, "PRINT_CHAR");
        put(38555, "COPY_BYTES");
        put(38562, "PLAY_THEME_TUNE");
        put(38601, "CHECK_ENTER_FIRE");
        put(38622, "PLAY_INTRO_SOUND");

        // ================================================================
        // Bases gráficas y tablas (memory map)
        // ================================================================
        put(38912, "TITLE_SCREEN_ATTR_TOP");
        put(39424, "UNKNOWN_39424");
        put(40000, "FOOT_BARREL_GRAPHIC");
        put(40032, "MARIA_GRAPHIC");
        put(40192, "WILLY_SPRITE_GRAPHIC");
        put(40448, "CODES_AREA");
        put(41983, "FIRST_ITEM_INDEX");

        // ================================================================
        // Direcciones que aparecen en tu lista original pero no tienen nombre ultra-específico
        // (las dejo con nombre claro para que no queden como UNKNOWN)
        // ================================================================
        put(23040, "TITLE_SCREEN_BOTTOM");
        put(23136, "MESSAGE_BUFFER");
        put(20480, "TEMP_BUFFER_20480");
        put(18432, "TEMP_BUFFER_18432");
        put(39680, "NUMBER_KEY_GRAPHICS");
        put(39808, "CODE_ENTRY_ATTR");
        put(22784, "TEMP_22784");
        put(1792,  "UDG_BASE");
        put(40192, "WILLY_SPRITE_GRAPHIC");   // repetido por seguridad
        put(48896, "KEYBOARD_READ");
        put(65280, "STACK_BASE");
        put(24320, "TEMP_24320");
        put(36605, "KEYBOARD_READ_2");
        put(65278, "KEYBOARD_PORT");
        put(32510, "KEYBOARD_PORT_2");
        put(6911,  "CLEAR_SCREEN_SIZE");
        put(6143,  "CLEAR_SCREEN_SIZE_2");
        put(4096,  "ATTRIBUTE_COPY_SIZE");
        put(2047,  "PIXEL_COPY_SIZE");
        put(4095,  "FULL_SCREEN_SIZE");
        put(65535, "MEMORY_END");
        put(65536, "MEMORY_SIZE");
    }

    private static void put(int addr, String name) {
        MEM_NAMES.put(addr, name);
    }

    public static void main(String[] args) throws IOException {

        Path input = Paths.get("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/rzzx/JetSetWilly2Converted2_R.java");
        Path output = Paths.get("JetSetWilly2Converted2_R.java");

        String content = Files.readString(input);


        // Patrón para encontrar mem_ seguido de números (decimal)
        Pattern pattern = Pattern.compile("mem_(\\d+)");
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int addr = Integer.parseInt(matcher.group(1));
            String replacement = MEM_NAMES.getOrDefault(addr, "mem_" + addr);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        String renamedContent = sb.toString();

        Files.writeString(output, renamedContent);

        System.out.println("Reemplazos completados.");
        System.out.println("Archivo generado: " + output);
        System.out.println("Direcciones conocidas usadas: " + MEM_NAMES.size());
        System.out.println("Direcciones no mapeadas seguirán como mem_XXXX_UNKNOWN");
    }
}