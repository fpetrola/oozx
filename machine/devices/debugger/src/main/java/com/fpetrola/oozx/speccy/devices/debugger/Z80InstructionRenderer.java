/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.devices.debugger;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Assembly with its parts in different colours: what it does, what it does it to, and the numbers.
 * <p>
 * From the debugger this window comes from, with the colours picked twice - a blue that reads on
 * white does not read on the dark desk - and the rules pointed at the text this disassembler
 * writes, where every number says {@code 0x} and a displacement is a signed pair of digits.
 */
public class Z80InstructionRenderer extends DefaultTableCellRenderer {

  private static final String MNEMONICS =
      "CCF|SCF|RRA|RLA|CPL|CPIR|CPDR|CPI|CPD|NEG|RLCA|RRCA|LDIR|LDDR|LDI|LDD|LD|PUSH|POP|EXX|EX"
          + "|ADD|ADC|SUB|SBC|AND|OR|XOR|CP|INC|DEC|RLC|RL|RRC|RR|SLA|SRA|SRL|BIT|SET|RES"
          + "|DJNZ|JP|JR|CALL|RETI|RETN|RET|RST|NOP|HALT|DI|EI|IN|OUT|IM";
  private static final String CONDITIONS = "NZ|Z|NC|C|PO|PE|P|M";
  private static final String REGISTERS16 = "AF|BC|DE|HL|IX|IY|SP|PC";

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    label.setText("<html>" + coloured(String.valueOf(value), isDark(table)) + "</html>");
    return label;
  }

  private static boolean isDark(JTable table) {
    Color background = table.getBackground();
    return background.getRed() + background.getGreen() + background.getBlue() < 3 * 128;
  }

  private static String coloured(String instruction, boolean dark) {
    String what = dark ? "#6FB3F2" : "blue";
    String on = dark ? "#7EC699" : "green";
    String numbers = dark ? "#E0A96D" : "#B35C00";
    String brackets = dark ? "#5FBFBF" : "teal";

    return instruction
        .replaceAll("\\b(JP|JR|CALL|RET)\\s+(" + CONDITIONS + ")\\b",
            "$1 <span style='color:" + what + "; font-style:italic;'>$2</span>")
        .replaceAll("\\b(" + MNEMONICS + ")\\b",
            "<span style='color:" + what + "; font-weight:bold;'>$1</span>")
        .replaceAll("\\b(" + REGISTERS16 + ")\\b",
            "<span style='color:" + on + "; font-style:italic; font-weight:bold;'>$1</span>")
        .replaceAll("\\b([ABCDEHLIR])\\b",
            "<span style='color:" + on + "; font-weight:bold;'>$1</span>")
        .replaceAll("(0x[0-9A-F]+|[+-][0-9A-F]{2})",
            "<span style='color:" + numbers + ";'>$1</span>")
        .replaceAll("([()])",
            "<span style='color:" + brackets + "; font-weight:bold;'>$1</span>");
  }
}
