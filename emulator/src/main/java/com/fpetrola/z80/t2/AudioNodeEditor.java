package com.fpetrola.z80.t2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class AudioNodeEditor extends JFrame {

    private Canvas canvas;

    public AudioNodeEditor() {
        setTitle("Editor de Nodos para Audio - Zoom Centrado + Feedback Origen");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        canvas = new Canvas();
        add(canvas);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem addNodeItem = new JMenuItem("Agregar Nodo");
        addNodeItem.addActionListener(e -> {
            Point p = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(p, canvas);
            canvas.addNode((int) (p.x / canvas.scale - 75), (int) (p.y / canvas.scale - 50));
        });
        popup.add(addNodeItem);
        canvas.setComponentPopupMenu(popup);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AudioNodeEditor().setVisible(true));
    }

    static class Canvas extends JPanel {
        private List<Node> nodes = new ArrayList<>();
        private List<Connection> connections = new ArrayList<>();

        private Port draggingFrom = null;  // Puerto de salida activo
        private Point dragCurrent = null;

        // Zoom y pan
        double scale = 1.0;
        double minScale = 0.2;
        double maxScale = 4.0;
        double offsetX = 0;  // Translación para pan (no usado aún, pero preparado)
        double offsetY = 0;

        private Port hoveredInput = null;   // Puerto entrada sobre el que estamos
        private Port activeOutput = null;   // Puerto salida desde el que estamos arrastrando (para feedback)

        public Canvas() {
            setBackground(Color.DARK_GRAY);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) return;

                    Point modelPoint = screenToModel(e.getPoint());
                    Node node = getNodeAt(modelPoint);
                    if (node != null) {
                        Port port = node.getPortAt(modelPoint);
                        if (port != null && port.isOutput) {
                            draggingFrom = port;
                            activeOutput = port;  // Activar feedback visual
                            dragCurrent = e.getPoint();
                            repaint();
                            return;
                        } else {
                            // Drag del nodo
                            node.dragging = true;
                            node.dragOffsetX = (int) (modelPoint.x - node.x);
                            node.dragOffsetY = (int) (modelPoint.y - node.y);
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggingFrom != null) {
                        Point modelPoint = screenToModel(e.getPoint());
                        Node targetNode = getNodeAt(modelPoint);
                        if (targetNode != null) {
                            Port targetPort = targetNode.getPortAt(modelPoint);
                            if (targetPort != null && !targetPort.isOutput && targetNode != draggingFrom.node) {
                                boolean exists = connections.stream()
                                        .anyMatch(c -> c.from == draggingFrom && c.to == targetPort);
                                if (!exists) {
                                    connections.add(new Connection(draggingFrom, targetPort));
                                }
                            }
                        }
                        draggingFrom = null;
                        activeOutput = null;
                        hoveredInput = null;
                        dragCurrent = null;
                        repaint();
                    }

                    for (Node n : nodes) n.dragging = false;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggingFrom != null) {
                        dragCurrent = e.getPoint();
                        updateHoveredInput(e.getPoint());
                        repaint();
                    } else {
                        Point modelPoint = screenToModel(e.getPoint());
                        for (Node node : nodes) {
                            if (node.dragging) {
                                node.x = (int) (modelPoint.x - node.dragOffsetX);
                                node.y = (int) (modelPoint.y - node.dragOffsetY);
                                repaint();
                            }
                        }
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (draggingFrom != null) {
                        updateHoveredInput(e.getPoint());
                        repaint();
                    }
                }
            });

            // Zoom con ruedita + Ctrl, centrado en el mouse
            addMouseWheelListener(e -> {
                if (e.isControlDown()) {
                    Point mouseScreen = e.getPoint();
                    Point mouseModelBefore = screenToModel(mouseScreen);

                    double oldScale = scale;
                    double delta = e.getWheelRotation() < 0 ? 1.15 : 0.85;
                    scale = Math.max(minScale, Math.min(maxScale, scale * delta));

                    // Recalcular offset para mantener el punto bajo el mouse fijo
                    Point mouseModelAfter = screenToModel(mouseScreen);

                    offsetX += (mouseModelBefore.x - mouseModelAfter.x) * scale;
                    offsetY += (mouseModelBefore.y - mouseModelAfter.y) * scale;

                    repaint();
                }
            });
        }

        private void updateHoveredInput(Point screenPoint) {
            Point modelPoint = screenToModel(screenPoint);
            hoveredInput = null;
            Node node = getNodeAt(modelPoint);
            if (node != null && draggingFrom != null && node != draggingFrom.node) {
                Port port = node.getPortAt(modelPoint);
                if (port != null && !port.isOutput) {
                    hoveredInput = port;
                }
            }
        }

        public void addNode(int x, int y) {
            nodes.add(new Node(x, y, "Nodo " + (nodes.size() + 1)));
            repaint();
        }

        private Node getNodeAt(Point modelPoint) {
            for (Node node : nodes) {
                if (new Rectangle(node.x, node.y, node.width, node.height).contains(modelPoint)) {
                    return node;
                }
            }
            return null;
        }

        // Convierte coordenadas de pantalla a modelo (teniendo en cuenta zoom y offset)
        private Point screenToModel(Point screen) {
            return new Point(
                    (int) ((screen.x - offsetX) / scale),
                    (int) ((screen.y - offsetY) / scale)
            );
        }

        private Point modelToScreen(Point model) {
            return new Point(
                    (int) (model.x * scale + offsetX),
                    (int) (model.y * scale + offsetY)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Aplicar transformaciones: offset + escala
            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);

            // Conexiones existentes
            for (Connection c : connections) {
                drawConnection(g2, c.from, c.to, Color.CYAN);
            }

            // Conexión temporal
            if (draggingFrom != null && dragCurrent != null) {
                Point start = draggingFrom.getAbsoluteCenter();
                Point end = screenToModel(dragCurrent);
                drawConnection(g2, start, end, Color.WHITE);
            }

            // Nodos
            for (Node node : nodes) {
                node.paint(g2, scale, hoveredInput, activeOutput);
            }

            g2.dispose();
        }

        private void drawConnection(Graphics2D g2, Port from, Port to, Color color) {
            Point start = from.getAbsoluteCenter();
            Point end = to.getAbsoluteCenter();
            drawConnection(g2, start, end, color);
        }

        private void drawConnection(Graphics2D g2, Point start, Point end, Color color) {
            int ctrlDist = Math.abs(start.x - end.x) / 2;
            Point ctrl1 = new Point(start.x + ctrlDist, start.y);
            Point ctrl2 = new Point(end.x - ctrlDist, end.y);

            CubicCurve2D curve = new CubicCurve2D.Double(
                    start.x, start.y, ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, end.x, end.y);

            g2.setColor(color);
            g2.setStroke(new BasicStroke(3f / (float) scale));
            g2.draw(curve);
        }
    }

    static class Node {
        int x, y;
        int width = 150;
        int height = 100;
        String title;
        boolean dragging = false;
        int dragOffsetX, dragOffsetY;

        List<Port> inputs = new ArrayList<>();
        List<Port> outputs = new ArrayList<>();

        public Node(int x, int y, String title) {
            this.x = x;
            this.y = y;
            this.title = title;

            inputs.add(new Port(this, false, 0));
            inputs.add(new Port(this, false, 1));
            outputs.add(new Port(this, true, 0));
            outputs.add(new Port(this, true, 1));
        }

        Port getPortAt(Point modelPoint) {
            for (Port port : inputs) {
                if (port.getModelBounds().contains(modelPoint)) return port;
            }
            for (Port port : outputs) {
                if (port.getModelBounds().contains(modelPoint)) return port;
            }
            return null;
        }

        void paint(Graphics2D g2, double scale, Port hoveredInput, Port activeOutput) {
            g2.setColor(new Color(40, 40, 50));
            g2.fillRoundRect(x, y, width, height, 20, 20);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(x, y, width, height, 20, 20);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, (int)(14 * scale)));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(title);
            g2.drawString(title, x + (width - tw) / 2, y + (int)(25 * scale));

            for (Port port : inputs) {
                boolean hovered = port == hoveredInput;
                port.paint(g2, scale, hovered, false);
            }
            for (Port port : outputs) {
                boolean active = port == activeOutput;
                port.paint(g2, scale, false, active);
            }
        }
    }

    static class Port {
        Node node;
        boolean isOutput;
        int index;

        private static final int BASE_RADIUS = 10;
        private static final int MARGIN = 20;

        public Port(Node node, boolean isOutput, int index) {
            this.node = node;
            this.isOutput = isOutput;
            this.index = index;
        }

        Point getAbsoluteCenter() {
            int portY = node.y + MARGIN + 25 + index * 30;
            int portX = isOutput ? node.x + node.width : node.x;
            return new Point(portX, portY);
        }

        Rectangle getModelBounds() {
            Point center = getAbsoluteCenter();
            int r = BASE_RADIUS;
            return new Rectangle(center.x - r, center.y - r, r * 2, r * 2);
        }

        void paint(Graphics2D g2, double scale, boolean isHovered, boolean isActive) {
            Point center = getAbsoluteCenter();
            int radius = (int) (BASE_RADIUS * scale);

            // Feedback fuerte si es el origen activo
            if (isActive) {
                g2.setColor(new Color(255, 165, 0)); // Naranja brillante
                g2.fillOval(center.x - (int)(radius * 1.8), center.y - (int)(radius * 1.8),
                        (int)(radius * 3.6), (int)(radius * 3.6));
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke((float)(4 * scale)));
                g2.drawOval(center.x - (int)(radius * 1.8), center.y - (int)(radius * 1.8),
                        (int)(radius * 3.6), (int)(radius * 3.6));
            }

            // Feedback si es destino hovered
            if (isHovered) {
                g2.setColor(Color.YELLOW);
                g2.fillOval(center.x - (int)(radius * 1.5), center.y - (int)(radius * 1.5),
                        (int)(radius * 3), (int)(radius * 3));
                g2.setColor(Color.ORANGE);
                g2.setStroke(new BasicStroke((float)(3 * scale)));
                g2.drawOval(center.x - (int)(radius * 1.5), center.y - (int)(radius * 1.5),
                        (int)(radius * 3), (int)(radius * 3));
            }

            // Puerto normal
            g2.setColor(isOutput ? Color.RED : Color.GREEN);
            g2.fillOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke((float)(2 * scale)));
            g2.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
        }
    }

    static class Connection {
        Port from;
        Port to;

        public Connection(Port from, Port to) {
            this.from = from;
            this.to = to;
        }
    }
}