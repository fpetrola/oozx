/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.oozx.speccy.modules.z80.Disassembly;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

/**
 * Instructions the way the debugger lists them: a gutter that says where it breaks, the address,
 * the bytes and the instruction in colour. Whoever shows it says which lines, and which of them
 * break; a table and not a window, so a graph can draw one inside each of its boxes.
 */
public class InstructionListing extends JTable {

  private final List<Disassembly.Line> lines = new ArrayList<>();
  private final Lines model = new Lines();
  private static final Pattern HEX = Pattern.compile("0x([0-9A-F]+)");

  private final TableColumn bytes;
  private IntPredicate breaks = address -> false;
  private boolean decimal;

  public InstructionListing() {
    setModel(model);
    getColumnModel().getColumn(0).setMaxWidth(24);
    getColumnModel().getColumn(1).setMaxWidth(70);
    getColumnModel().getColumn(2).setMaxWidth(110);
    getColumnModel().getColumn(3).setCellRenderer(new Coloured());
    bytes = getColumnModel().getColumn(2);
  }

  /** Addresses and operands in decimal instead of hex; the bytes stay in hex, they are opcodes. */
  public void decimal(boolean decimal) {
    this.decimal = decimal;
    model.fireTableDataChanged();
  }

  public void showBytes(boolean shown) {
    if (shown == (getColumnModel().getColumnCount() == model.getColumnCount())) return;
    if (shown) {
      addColumn(bytes);
      moveColumn(getColumnCount() - 1, 2);
    } else {
      removeColumn(bytes);
    }
  }

  public void show(List<Disassembly.Line> shown) {
    lines.clear();
    lines.addAll(shown);
    model.fireTableDataChanged();
  }

  public List<Disassembly.Line> lines() {
    return List.copyOf(lines);
  }

  public void breaksAt(IntPredicate breaks) {
    this.breaks = breaks;
  }

  /** Selects the line that starts at an address, or none when it is not listed. */
  public void select(int address) {
    clearSelection();
    for (int row = 0; row < lines.size(); row++) {
      if (lines.get(row).address() == address) setRowSelectionInterval(row, row);
    }
  }

  private class Lines extends AbstractTableModel {
    private final String[] columns = {"", "Address", "Bytes", "Instruction"};

    public int getRowCount() {
      return lines.size();
    }

    public int getColumnCount() {
      return columns.length;
    }

    public String getColumnName(int column) {
      return columns[column];
    }

    public Object getValueAt(int row, int column) {
      Disassembly.Line at = lines.get(row);
      return switch (column) {
        case 0 -> breaks.test(at.address()) ? "●" : "";
        case 1 -> decimal ? String.valueOf(at.address()) : "%04X".formatted(at.address());
        case 2 -> at.bytes();
        default -> decimal ? HEX.matcher(at.instruction()).replaceAll(hex -> String.valueOf(Integer.parseInt(hex.group(1), 16)))
            : at.instruction();
      };
    }
  }

  /**
   * Assembly with its parts in different colours: what it does, what it does it to, and the numbers.
   * <p>
   * From the debugger this window comes from, with the colours picked twice - a blue that reads on
   * white does not read on the dark desk - and the rules pointed at the text this disassembler
   * writes, where every number says {@code 0x} and a displacement is a signed pair of digits.
   */
  private static class Coloured extends DefaultTableCellRenderer {

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
          .replaceAll("(0x[0-9A-F]+|[+-][0-9A-F]{2}|\\b[0-9]+\\b)",
              "<span style='color:" + numbers + ";'>$1</span>")
          .replaceAll("([()])",
              "<span style='color:" + brackets + "; font-weight:bold;'>$1</span>");
    }
  }
}
