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

import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.routines.Routine;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Z80Debugger {
  private static final Set<Integer> breakpoints = new HashSet<>(); // To store breakpoints
  public static BlocksManager blockManager;
  private static Thread updateThread;
  private static boolean ready;
  private static DefaultTableModel model;
  private static JCheckBox[] flagCheckboxes;
  public static Map<Routine, JComponent> instructionTables = new ConcurrentHashMap<>();
  public static JTree routinesTree;
  public static Map<Routine, DefaultMutableTreeNode> treeNodes = new HashMap<>();
  private static TreeSelectionListener treeSelectionListener;

  public static void main(String[] args) {
//    LafManager.install();
//    LafManager.install(new DarculaTheme());

    SwingUtilities.invokeLater(() -> createAndShowGUI(new Z80Emulator(), e -> {
    }));
  }

  public static void createAndShowGUI(Z80Emulator emulator1, TreeSelectionListener treeSelectionListener) {
//    setFont(new FontUIResource(new Font("Tahoma", Font.PLAIN, 10)));
    FontUIResource tahoma = new FontUIResource(new Font("Tahoma", Font.PLAIN, 8));
    setUIFont(0.8f);

    Z80Debugger.treeSelectionListener = treeSelectionListener;

    JPanel mainPanel = createMainPanel(emulator1);

    JFrame frame = createFrame(mainPanel);

//    changeFontRecursive(frame, 9);
  }

  private static void changeFontRecursive(Container root, float size) {
    for (Component c : root.getComponents()) {
      Font font1 = c.getFont().deriveFont(size);
      c.setFont(font1);
      if (c instanceof Container) {
        changeFontRecursive((Container) c, size);
      }
    }
  }

  public static JPanel createMainPanel(Z80Emulator emulator1) {
    JLabel[] registerLabels = {
        new JLabel("A: 00"), new JLabel("F: 00"), new JLabel("B: 00"), new JLabel("C: 00"),
        new JLabel("D: 00"), new JLabel("E: 00"), new JLabel("H: 00"), new JLabel("L: 00"),
        new JLabel("PC: 0000"), new JLabel("SP: 0000"),
        new JLabel("Z: 0"), new JLabel("N: 0"), new JLabel("H: 0"), new JLabel("C: 0")
    };

    // Main Layout
    JPanel mainPanel = new JPanel(new BorderLayout());

    createMenubar();


    JScrollPane instructionScrollPane = (JScrollPane) createInstructionTable();
    JTable instructionTable = (JTable) ((JViewport) instructionScrollPane.getComponent(0)).getComponent(0);


//      // Instruction view
//        JTable instructionTable = new JTable(new DefaultTableModel(new Object[]{"Address", "Instruction"}, 0));
//        instructionTable.getColumnModel().getColumn(0).setPreferredWidth(60); // Smaller address column width
//        JScrollPane instructionScrollPane = new JScrollPane(instructionTable);
//        instructionScrollPane.setBorder(BorderFactory.createTitledBorder("Instructions"));

    // Memory view
    JTable memoryTable = new JTable(new DefaultTableModel(0x10000 / 16, 17)); // 16 rows of 16 columns
    JScrollPane memoryScrollPane = new JScrollPane(memoryTable);
    memoryScrollPane.setBorder(BorderFactory.createTitledBorder("Memory"));
    JPanel memoryPanel = new JPanel(new BorderLayout());
    memoryPanel.add(memoryScrollPane, BorderLayout.NORTH);

    JTextField jumpToAddressField = new JTextField(10);
    JButton jumpToAddressButton = new JButton("Jump");
    JPanel jumpPanel = new JPanel();
    jumpPanel.add(new JLabel("Jump to Address: "));
    jumpPanel.add(jumpToAddressField);
    jumpPanel.add(jumpToAddressButton);
    memoryPanel.add(jumpPanel, BorderLayout.SOUTH);

    // Register view
    JPanel registerPanel = new JPanel();
    registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.Y_AXIS));
    registerPanel.setBorder(BorderFactory.createTitledBorder("Registers"));
    registerPanel.setPreferredSize(new Dimension(20, 400)); // Fixed width and height

    String[] registers = {"AF", "BC", "DE", "HL", "IX", "IY", "SP", "PC"};
    JTextField[] registerFields = new JTextField[registers.length];
    int f1 = 0;

    for (String reg : registers) {
      JPanel regPanel = new JPanel(new BorderLayout());
      regPanel.setMaximumSize(new Dimension(200, 30)); // Fixed height for each register line
      JLabel label = new JLabel(reg + ":");
      HighlightChangedTextField field = new HighlightChangedTextField("0000", 5);
      field.setMaximumSize(new Dimension(40, 25)); // Fixed width for register field
      regPanel.add(label, BorderLayout.WEST);
      regPanel.add(field, BorderLayout.SOUTH);
      registerFields[f1++] = field;
      registerPanel.add(regPanel);
    }

    JPanel flagPanel = new JPanel(new GridLayout(1, 4));
    flagCheckboxes = new JCheckBox[]{
        new JCheckBox("Z"), new JCheckBox("N"), new JCheckBox("H"), new JCheckBox("C")
    };
    for (JCheckBox checkbox : flagCheckboxes) {
      flagPanel.add(checkbox);
    }
    registerPanel.add(flagPanel);

    // Breakpoints view
    JTable breakpointsTable = new JTable(new MyDefaultTableModel());
    JScrollPane breakpointsScrollPane = new JScrollPane(breakpointsTable);
    breakpointsScrollPane.setBorder(BorderFactory.createTitledBorder("Breakpoints"));
    breakpointsTable.getColumnModel().getColumn(2).setCellRenderer(new Z80InstructionRenderer());

    JSplitPane leftSplitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, instructionScrollPane, registerPanel);
    leftSplitPane1.setResizeWeight(0.9);

    // Layout adjustments
    JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftSplitPane1, memoryScrollPane);
    leftSplitPane.setResizeWeight(0.7);


    JPanel rightPanel = new JPanel(new BorderLayout());
//    rightPanel.add(registerPanel, BorderLayout.NORTH);
    rightPanel.add(breakpointsScrollPane, BorderLayout.CENTER);
//    rightPanel.add(memoryPanel, BorderLayout.SOUTH);

    JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, rightPanel);
    mainSplitPane.setResizeWeight(0.8);

    JScrollPane treeScrollPanel = getjScrollPane();
    JSplitPane treeSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPanel, mainSplitPane);
    treeSplit.setResizeWeight(0.3);

    mainPanel.add(treeSplit, BorderLayout.CENTER);

    // Debugger logic placeholder

    instructionTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int row = instructionTable.rowAtPoint(e.getPoint());
        int col = instructionTable.columnAtPoint(e.getPoint());

        if (col == 0) {
          int address = Integer.parseInt((String) instructionTable.getValueAt(row, 1), 16);

          if (breakpoints.contains(address)) {
            breakpoints.remove(address);
          } else {
            breakpoints.add(address);
          }
        }

        if (row >= 0 && instructionTable.columnAtPoint(e.getPoint()) == 0) {
          DefaultTableModel model = (DefaultTableModel) instructionTable.getModel();
          String current = (String) model.getValueAt(row, 0);
          model.setValueAt(current.equals("●") ? "" : "●", row, 0);
          updateBreakpointsTable(breakpointsTable, instructionTable);
        }
        instructionTable.repaint();

      }
    });

    JPanel comp = new JPanel(new BorderLayout());
    createToolbar(emulator1, comp, instructionTable, memoryTable, registerLabels, registerFields);
    JToolBar toolBar = new JToolBar();
    JButton addBreakpointButton = new JButton("Add breakpoint", new ImageIcon("icons/step.png"));
    JButton removeBreakpointButton = new JButton("Remove breakpoint", new ImageIcon("icons/stepInto.png"));
    toolBar.add(addBreakpointButton);
    toolBar.add(removeBreakpointButton);
    comp.add(toolBar, BorderLayout.EAST);

    addBreakpointButton.addActionListener((e -> {
      DefaultTableModel breakpointsModel = (DefaultTableModel) breakpointsTable.getModel();

      breakpointsModel.addRow(new Object[]{
          true,
          0,
          "PC == 1",
          "Code"
      });
    }));

    mainPanel.add(comp, BorderLayout.NORTH);


    emulator1.setInstructionTableModel(instructionTable);
    emulator1.setBreakpointsModel((DefaultTableModel) breakpointsTable.getModel());
    return mainPanel;
  }

  private static JScrollPane getjScrollPane() {

    // Creating the root node
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Program");

//    // Creating child nodes
//    DefaultMutableTreeNode parent1 = new DefaultMutableTreeNode("Parent 1");
//    DefaultMutableTreeNode child1_1 = new DefaultMutableTreeNode("Child 1.1");
//    DefaultMutableTreeNode child1_2 = new DefaultMutableTreeNode("Child 1.2");
//
//    // Adding child nodes to the parent1
//    parent1.add(child1_1);
//    parent1.add(child1_2);
//
//    // Creating another set of child nodes
//    DefaultMutableTreeNode parent2 = new DefaultMutableTreeNode("Parent 2");
//    DefaultMutableTreeNode child2_1 = new DefaultMutableTreeNode("Child 2.1");
//    DefaultMutableTreeNode child2_2 = new DefaultMutableTreeNode("Child 2.2");
//
//    // Adding child nodes to the parent2
//    parent2.add(child2_1);
//    parent2.add(child2_2);
//
//    // Adding parent nodes to the root
//    root.add(parent1);
//    root.add(parent2);

    routinesTree = new JTree(root);
    routinesTree.addTreeSelectionListener(treeSelectionListener);
    JScrollPane treeScrollPanel = new JScrollPane(routinesTree);
    treeScrollPanel.setBorder(BorderFactory.createTitledBorder("Routines"));
    return treeScrollPanel;
  }

  protected static ImageIcon createImageIcon(String path,
                                             String description) {
    java.net.URL imgURL = Z80Debugger.class.getResource(path);
    if (imgURL != null) {
      ImageIcon imageIcon = new ImageIcon(imgURL, description);
      Image image = imageIcon.getImage();
      Image newimg = image.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
      imageIcon = new ImageIcon(newimg);

      return imageIcon;
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }

  private static void createToolbar(Z80Emulator emulator1, JPanel mainPanel, JTable instructionTable, JTable memoryTable, JLabel[] registerLabels, JTextField[] registerFields) {
    // Toolbar
    JToolBar toolBar = new JToolBar();
    JButton stepButton = createButton("Step Over", "/icons/step-over.png");
    JButton stepIntoButton = createButton("Step Into", "/icons/step-into.png");
    JButton stepOutButton = createButton("Step Out", "/icons/step-out.png");
    JButton continueButton = createButton("Continue", "/icons/continue.png");
    JButton pauseButton = createButton("Pause", "/icons/pause.png");
    JButton stopButton = createButton("Stop", "/icons/stop.png");
    toolBar.add(stepButton);
    toolBar.add(stepIntoButton);
    toolBar.add(stepOutButton);
    toolBar.add(continueButton);
    toolBar.add(pauseButton);
    toolBar.add(stopButton);
    mainPanel.add(toolBar, BorderLayout.WEST);
    stepButton.addActionListener(e -> {
      ready = true;
      emulator1.step();
      update(emulator1, instructionTable, memoryTable, registerLabels, registerFields);
    });

    stepIntoButton.addActionListener(e -> {
      emulator1.stepInto();
      update(emulator1, instructionTable, memoryTable, registerLabels, registerFields);
    });

    stepOutButton.addActionListener(e -> {
      emulator1.stepOut();
      update(emulator1, instructionTable, memoryTable, registerLabels, registerFields);
    });


    continueButton.addActionListener(e -> {
      ready = true;
      emulator1.continueExecution();
//      updateThread = createUpdateThread(emulator1, instructionTable, memoryTable, registerLabels, registerFields);
//      updateThread.start();
    });

    emulator1.setUpdateListener(() -> {
      SwingUtilities.invokeLater(() -> {
        update(emulator1, instructionTable, memoryTable, registerLabels, registerFields);
      });
    });

    pauseButton.addActionListener(e -> emulator1.pauseExecution());

    stopButton.addActionListener(e -> {
      ready = true;
      emulator1.stopExecution();
      breakpoints.clear();
      update(emulator1, instructionTable, memoryTable, registerLabels, registerFields);
    });

//    jumpToAddressButton.addActionListener(e -> {
//      try {
//        int address = Integer.parseInt(jumpToAddressField.getText(), 16);
//        scrollToAddress(memoryTable, address);
//      } catch (NumberFormatException ex) {
//        JOptionPane.showMessageDialog(frame, "Invalid address format", "Error", JOptionPane.ERROR_MESSAGE);
//      }
//    });
  }

  private static JButton createButton(String tooltipText, String iconPath) {
    JButton jButton = new JButton(null, createImageIcon(iconPath, ""));
    jButton.setToolTipText(tooltipText);
    return jButton;
  }

  private static void createMenubar() {
    // Menu Bar
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    fileMenu.add(new JMenuItem("Load Program"));
    fileMenu.add(new JMenuItem("Exit"));
    JMenu debugMenu = new JMenu("Debugging");
    debugMenu.add(new JMenuItem("Step"));
    debugMenu.add(new JMenuItem("Step Into"));
    debugMenu.add(new JMenuItem("Continue"));
    debugMenu.add(new JMenuItem("Pause"));
    debugMenu.add(new JMenuItem("Stop"));
    JMenu memoryMenu = new JMenu("Memory");
    memoryMenu.add(new JMenuItem("Jump to Address"));
    JMenu helpMenu = new JMenu("Help");
    helpMenu.add(new JMenuItem("About"));
    menuBar.add(fileMenu);
    menuBar.add(debugMenu);
    menuBar.add(memoryMenu);
    menuBar.add(helpMenu);
//    frame.setJMenuBar(menuBar);
  }

  public static JComponent createInstructionTable() {
    // Instruction view with breakpoints
    Rectangle r = new Rectangle(0, 0, 200, 200);

    Object[] columnNames = {"", "Address", "Bytes", "Instruction"};
    InstructionTableModel dm = new InstructionTableModel(columnNames);
    JTable instructionTable = new JTable(dm);
    dm.setComponent(instructionTable);

    model = (DefaultTableModel) instructionTable.getModel();

    // Renderer for the circle column
    instructionTable.getColumnModel().getColumn(0).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
      JPanel panel = new JPanel();
      panel.setOpaque(false);
      panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
      int address = Integer.parseInt((String) table.getValueAt(row, 1), 16);

      JButton circle = new JButton();
      circle.setPreferredSize(new Dimension(10, 10));
      circle.setBorderPainted(false);
      circle.setFocusPainted(false);
      circle.setContentAreaFilled(false);
      circle.setOpaque(false);

      if (breakpoints.contains(address)) {
        circle.setBackground(Color.RED);
        circle.setOpaque(true);
      }

      panel.add(circle);
      return panel;
    });

    setupColumnModel(instructionTable);
    instructionTable.setBounds(r);

    JScrollPane instructionScrollPane = new JScrollPane(instructionTable) {
      @Override
      public boolean isShowing() {
        return true;
      }
    };

    instructionScrollPane.getViewport().setBounds(r);
    instructionScrollPane.getVerticalScrollBar().setBounds(new Rectangle((int) (r.getX() + r.getWidth()), (int) r.getY(), (int) 20, (int) r.getHeight()));
    instructionScrollPane.getHorizontalScrollBar().setBounds(new Rectangle((int) r.getX(), (int) (r.getY() + r.getHeight()), (int) r.getWidth(), (int) 20));

    instructionScrollPane.setBounds(r);
    instructionScrollPane.setBorder(BorderFactory.createTitledBorder("Instructions"));
    instructionTable.setMaximumSize(new Dimension(100, 400)); // Fixed width and height
    return instructionScrollPane;
  }

  public static void setupColumnModel(JTable instructionTable) {
    instructionTable.getColumnModel().getColumn(0).setMaxWidth(30); // Circle column width
    instructionTable.getColumnModel().getColumn(1).setMaxWidth(50); // Address column width
    instructionTable.getColumnModel().getColumn(2).setMaxWidth(70); // Address column width
    instructionTable.getColumnModel().getColumn(3).setCellRenderer(new Z80InstructionRenderer());
  }

  public static void setUIFont(float scale) {
    Enumeration<Object> keys = UIManager.getLookAndFeelDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      if (value instanceof javax.swing.plaf.FontUIResource font) {
        int size = font.getSize();
        Font font1 = font.deriveFont(size * scale);
        UIManager.put(key, font1);
      }
    }
  }

  private static JFrame createFrame(JPanel mainPanel) {
    JFrame frame = new JFrame("Z80 Debugger");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 600);
    frame.add(mainPanel);
    frame.setVisible(true);
    return frame;
  }

  private static void update(Z80Emulator emulator1, JTable instructionTable, JTable memoryTable, JLabel[] registerLabels, JTextField[] registerFields) {
//    updateInstructionTable(emulator1, instructionTable);
//    updateMemoryTable(emulator1, memoryTable);
    updateRegisterPanel(emulator1, registerLabels, registerFields);
  }


  private static void updateBreakpointsTable(JTable breakpointsTable, JTable instructionTable) {
    DefaultTableModel breakpointsModel = (DefaultTableModel) breakpointsTable.getModel();
    DefaultTableModel instructionsModel = (DefaultTableModel) instructionTable.getModel();
    breakpointsModel.setRowCount(0);

    for (int i = 0; i < instructionsModel.getRowCount(); i++) {
      if ("●".equals(instructionsModel.getValueAt(i, 0))) {
        breakpointsModel.addRow(new Object[]{
            true,
            i,
            instructionsModel.getValueAt(i, 2),
            "Code"
        });
      }
    }
  }

  private static void updateInstructionTable(Z80Emulator emulator, JTable instructionTable) {
    String[] instructions = emulator.getInstructions();
    int lastInstructionPC = 0;
    Map<Integer, Integer> pcMap = new HashMap<>();
    int pc = emulator.getPC();

    for (int i = pc; i < pc + 50; i++) {
      String instruction = instructions[i];
      if (instruction != null) {
        lastInstructionPC = i;
        model.setValueAt("", i, 0);
        model.setValueAt(String.format("%04X", i), i, 1);
        model.setValueAt(instruction, i, 2);
      }
      pcMap.put(i, model.getRowCount() - 1);
    }

    Integer row = pcMap.get(pc);
    row = pc;
    instructionTable.setRowSelectionInterval(row, row);
    instructionTable.scrollRectToVisible(new Rectangle(instructionTable.getCellRect(row, 0, true)));
  }

  private static void updateInstructionTable2(Z80Emulator emulator, JTable instructionTable) {
    DefaultTableModel model = (DefaultTableModel) instructionTable.getModel();
    model.setRowCount(0);
    String[] instructions = emulator.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      model.addRow(new Object[]{"", String.format("%04X", i), instructions[i]});
    }
    int pc = emulator.getPC();
    instructionTable.setRowSelectionInterval(pc, pc);
    instructionTable.scrollRectToVisible(new Rectangle(instructionTable.getCellRect(pc, 0, true)));
  }

  private static void updateMemoryTable(Z80Emulator emulator, JTable memoryTable) {
    byte[] memory = emulator.getMemory();
    DefaultTableModel model = (DefaultTableModel) memoryTable.getModel();
    for (int row = 0; row < 0xFFFF / 16; row++) {
      model.setValueAt(String.format("%04X", row * 16), row, 0);
      for (int col = 1; col < 17; col++) {
        int address = row * 16 + (col - 1);
        model.setValueAt(String.format("%02X", memory[address]), row, col);
      }
    }
  }

  private static void updateRegisterPanel(Z80Emulator emulator, JLabel[] registerLabels, JTextField[] registerFields) {
    int[] registers = emulator.getRegisters();
    boolean[] flags = emulator.getFlags();
    String[] registerNames = {"AF", "BC", "DE", "HL", "IX", "IY", "SP", "PC"};
    for (int i = 0; i < registerNames.length; i++) {
      ((HighlightChangedTextField) registerFields[i]).updatePreviousValue();
      registerFields[i].setText("%04X".formatted(registers[i]));
//      registerLabels[i].setText(String.format("%s: %02X", registerNames[i], registers[i]));
    }

    String[] flagNames = {"Z", "N", "H", "C"};
    for (int i = 0; i < flagNames.length; i++) {
      registerLabels[10 + i].setText(String.format("%s: %d", flagNames[i], flags[i] ? 1 : 0));
    }

    flagCheckboxes[0].setSelected((registers[0] & 0x40) != 0);
    flagCheckboxes[1].setSelected((registers[0] & 0x02) != 0);
    flagCheckboxes[2].setSelected((registers[0] & 0x10) != 0);
    flagCheckboxes[3].setSelected((registers[0] & 0x01) != 0);

  }

  private static void scrollToAddress(JTable memoryTable, int address) {
    int row = address / 16;
    memoryTable.scrollRectToVisible(new Rectangle(memoryTable.getCellRect(row, 0, true)));
  }

  public static JComponent addInstructionTable(Routine routine) {
//    Block block = blockManager.findBlockByName(routine);
    JComponent jTable = instructionTables.get(routine);
    if (jTable == null) {
      instructionTables.put(routine, jTable = createInstructionTable());
      if (routinesTree != null) {
        TreeModel model1 = routinesTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model1.getRoot();
        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(routine);
        treeNodes.put(routine, newChild);
        SwingUtilities.invokeLater(() -> {
          root.add(newChild);
          routinesTree.updateUI();
        });
      }
    }

    return jTable;
  }

  private static class MyDefaultTableModel extends DefaultTableModel {
    public MyDefaultTableModel() {
      super(new Object[]{"Enabled", "Line", "Instruction", "Type"}, 0);
    }

  }

}

