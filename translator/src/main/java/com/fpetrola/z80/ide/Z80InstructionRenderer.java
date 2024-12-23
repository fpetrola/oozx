/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.ide;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class Z80InstructionRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);

        if (value != null && column == 2) { // Ensure it's the Instruction column
            String instruction = value.toString();
            String highlighted = highlightInstruction(instruction);
            label.setText("<html>" + highlighted + "</html>");
        }

        return label;
    }

    private String highlightInstruction(String instruction) {
        // Highlighting condition codes (Z, NZ, C, NC, PE, PO, P, M) in valid contexts
        instruction = instruction.replaceAll("\\b(JP|JR|CALL|RET)\\s+(Z|NZ|C|NC|PE|PO|P|M)\\b",
            "$1 <span style='color:blue; font-style:italic;'>$2</span>");
        // Highlighting all Z80 mnemonics

        instruction = instruction.replaceAll("\\b(LD|LDI|LDD|LDIR|LDDR|PUSH|POP|EX|EXX|EXAF|ADD|ADC|SUB|SBC|AND|OR|XOR|CP|INC|DEC|RLC|RL|RRC|RR|SLA|SRA|SRL|BIT|SET|RES|JP|JR|DJNZ|CALL|RET|RETI|RETN|RST|NOP|HALT|DI|EI|IN|OUT)\\b",
            "<span style='color:blue; font-weight:bold;'>$1</span>");

        // Highlighting 8-bit registers (A, B, C, D, E, H, L, I, R)
        instruction = instruction.replaceAll("\\b(A|B|C|D|E|H|L|I|R)\\b",
            "<span style='color:green; font-weight:bold;'>$1</span>");

        // Highlighting 16-bit registers (AF, BC, DE, HL, IX, IY, SP, PC)
        instruction = instruction.replaceAll("\\b(AF|BC|DE|HL|IX|IY|SP|PC)\\b",
            "<span style='color:green; font-style:italic; font-weight:bold;'>$1</span>");


        // Highlighting hexadecimal numbers ending with H
        instruction = instruction.replaceAll("0x([0-9A-Fa-f]+)",
            "<span style='color:orange;'>0x$1</span>");

        // Highlighting immediate decimal numbers
        instruction = instruction.replaceAll("\\b(\\d+)\\b",
            "<span style='color:purple;'>$1</span>");

        // Highlighting parentheses
        instruction = instruction.replaceAll("(\\(|\\))",
            "<span style='color:teal; font-weight:bold;'>$1</span>");

        // Highlighting plus symbol
        instruction = instruction.replaceAll("(\\+)",
            "<span style='color:red; font-weight:bold;'>$1</span>");

        return instruction;
    }
}
