/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         Visualizer.java
 * RCS:          $Id: Visualizer.java,v 1.34 2006/02/17 15:21:05 drc915 Exp $
 * Description:  Visualizer class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Nov 17, 2004
 * Modified		 Jul 23, 2007 by Bjoern Wiedersheim, Ulm University
 * Language:     Java
 * Package:      driver
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2005, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package vans.straw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.misc.Location;
import vans.straw.streets.RoadSegment;
import vans.straw.streets.Shape;

/**
 * <p>
 * Title: Visualizer
 * </p>
 * <p>
 * Description: Creates a GUI that displays node mobility.
 * </p>
 * 
 * @author David Choffnes
 * @version 0.1
 */
public class Visualizer implements VisualizerInterface {
	private static Visualizer activeInstance;
	/** gui configuration constants */

	/** true if showing routing text */
	final static boolean UPDATE_ROUTING_TEXT = true;
	/** offset from bottom of screen */
	private static final int BOTTOM_OFFSET = 100;

	/** key types */
	public static final int CIRCLE = 1;
	static public final int CAR = 2;

	/** The JFrame for displaying the GUI. */
	public static JFrame frame;

	/** The JPanel for displaying the GUI. */
	private JPanel panel;

	/** the button for showing congestion colors */
	private ZoomToggle zoomToggle;

	private JScrollPane fieldPane;

	/** the width of the field */
	private int fieldX;
	/** the height of the field */
	private int fieldY;

	/** zoom level */
	private static double zoom = 1;

	/** title pane dimensions */
	private final static int titlePaneHeight = 0;
	/** dimensions for the information panes */
	private final static int infoPaneHeight = 200;
	private final static int infoPaneWidth = 400;
	private final static int totalTopHeight = infoPaneHeight + titlePaneHeight;

	/*
	 * Bjoern Wiedersheim: insert disable drawing of circles
	 */
	public static final boolean DRAW_CIRCLE = false;

	/** Simulation configuration object */
	// public static JistExperiment je;
	public Mobility mob;

	/**
	 * Layered pane put into content pane.
	 */
	JLayeredPane layeredPane;
	/** time label */
	private JLabel timeLabel;
	/** Displays routing textual information */
	private JEditorPane routingEditorPane;
	/** Displays general textual information */
	private JEditorPane generalEditorPane;

	/** Radius for circle representing transmission radius */
	private double radioRadius;
	/** stores the nodes for this sim run */
	public HashMap<Integer, Node> nodes;

	private JCheckBox showCommunication;

	private Field field;
	private JPanel contentPane;
	private JPanel keyPanel;
	private GridBagConstraints c2;

	/**
	 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
	 * 
	 *         The JPanelCircle class draws a cirlce in a JPanel.
	 */
	public class JPanelCircle extends JPanel {

		private static final long serialVersionUID = 1L;
		Color c;
		int x;
		int y;

		public JPanelCircle(Color c, int x, int y) {
			super();
			this.x = x;
			this.y = y;
			this.c = c;
		}

		public void paintComponent(Graphics g) {
			((Graphics2D) g).setStroke(new BasicStroke(3));
			g.setColor(c);
			g.drawOval(0, 0, x, y);
		}
	}

	public class PersistentCircle {
		Color c; // color
		int radius; // radius
		int x; // locations
		int y;
		long expire; // duration

		public PersistentCircle(Color c, long duration, int radius, int x, int y) {

			this.c = c;
			this.expire = duration;
			this.radius = radius;
			this.x = x;
			this.y = y;
		}

		public void paintComponent(Graphics g) {
			((Graphics2D) g).setStroke(new BasicStroke(3));
			g.setColor(c);
			g.drawOval((int) (x / zoom), (int) (y / zoom), radius, radius);
		}

	}

	public class RadioAnimator extends Thread {
		Node n;
		JPanel p;
		boolean stop = false;
		public Color color;

		/**
		 * @param n
		 * @param p
		 * @param color
		 */
		public RadioAnimator(Node n, JPanel p, Color color) {
			this.n = n;
			this.p = p;
			this.color = color;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */

		public void run() {
			while (n.animateRadiusStep != Node.MAX_ANIMATION_STEPS && !stop) {
				// n.animateRadius(p.getGraphics());

				try {
					Thread.sleep(Node.INTERSTEP_TIME);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public void stopThread() {
			stop = true;
		}
	}

	public class ZoomToggle extends JSpinner implements ChangeListener {

		private static final long serialVersionUID = 1L;

		public ZoomToggle() {
			super();
			this.addChangeListener(this);
		}

		/**
		 * @param model
		 */
		public ZoomToggle(SpinnerModel model) {
			super(model);
			this.addChangeListener(this);
		}

		public void stateChanged(ChangeEvent e) {
			zoom = ((Double) this.getValue()).doubleValue();
			System.out.println("zoom: " + zoom);
			updateVisualizer();
		}
	}

	/**
	 * The Node class represents the information for drawing a node on a field.
	 * Using a JButton to allow simpler click functionalities.
	 */
	public static class Node extends JButton implements ActionListener {

		private static final long serialVersionUID = 1L;
		/** location on the field */
		public float x, y;
		/** node's identifier */
		public int ip;
		/** default color for this node */
		public Color defaultColor;
		/** displays circle around node if true */
		public boolean showRadius = false;
		/** circle radius for this node */
		public double radioRadius;
		/** button sizes */
		static int buttonHeight = 15;
		static int buttonWidth = 22;
		// static JistExperiment lje = JistExperiment.getJistExperiment();
		// public StreetMobility sm;
		public Mobility mob;
		final static int MAX_ANIMATION_STEPS = 5;
		final static int INTERSTEP_TIME = 250; // in milliseconds

		private boolean animateRadius;
		private long nextTime;
		private int animateRadiusStep;
		public Color radiusColor = new Color(0.0f, 0.0f, 1.0f, 0.7f);// Color.BLUE;
		public Color defaultRadiusColor = radiusColor;

		public Node(float x, float y, int ip, Mobility stmob) {
			super(String.valueOf(ip));
			this.mob = stmob;
			this.x = x;
			this.y = y;
			this.ip = ip;
			this.setToolTipText("Node Ip: " + ip);
			this.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
			this.setSize(new Dimension(buttonWidth, buttonHeight));
			this.setText("" + ip);
			this.setMargin(new Insets(0, 0, 0, 0));

		}

		public void actionPerformed(ActionEvent evt) {
			// This method is called to respond when the user
			// presses the button. It sets the node to inspect.
			if ((mob != null) && (mob instanceof StreetMobility)) {
				StreetMobility sm = (StreetMobility) mob;
				if (sm.getCarToInspect() < 0)
					sm.setCarToInspect(ip);
				else {
					int i = sm.getCarToInspect();
					sm.unsetCarToInspect();
					if (ip != i)
						sm.setCarToInspect(ip);
				}
			}

		} // end actionPerformed()

		public void updateLocation(float newX, float newY, Field f) {

			this.x = newX;
			this.y = newY;
			setLocation((int) (x / zoom - buttonWidth / 2),
					(int) (y / zoom - buttonHeight / 2));

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if (!(obj instanceof Node))
				return false;
			Node n = (Node) obj;
			if (n.ip == ip)
				return true;
			else
				return false;
		}

		/**
		 * Recolors this node with the default color.
		 * 
		 */
		public void resetColor() {
			setBackground(defaultColor);
			this.setBorderPainted(false);
		}

		public void animateRadius() {
			showRadius = true;
			animateRadius = true;
			animateRadiusStep = 0;
			nextTime = System.currentTimeMillis() + INTERSTEP_TIME;

		}

		public void animateRadius(Graphics g) {
			// Graphics g = p.getGraphics();
			int denom = (int) (zoom * 2);
			if (animateRadius) {
				if (animateRadiusStep == MAX_ANIMATION_STEPS) {
					showRadius = false;
					animateRadius = false;
					denom = 10000;
				} else {
					if (System.currentTimeMillis() > nextTime) {
						animateRadiusStep++;
						nextTime += INTERSTEP_TIME;
					}
					denom *= (MAX_ANIMATION_STEPS - animateRadiusStep);
				}

			}

			int radius = (int) (radioRadius / denom);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setStroke(new BasicStroke(3));
			g2d.setColor(radiusColor);
			/*
			 * Bjoern Wiedersheim: modify disable drawing of circles
			 */
			if (DRAW_CIRCLE) {
				g2d.drawOval((int) (x / zoom - radius),
						(int) (y / zoom - radius), (int) (radius * 2),
						(int) (radius * 2));
			}
			g2d.setColor(Color.BLACK);

		}

		public void setColor(Color c) {
			this.setBorderPainted(true);
			this.setBorder(new LineBorder(c, 3));
		}
	}

	/**
	 * 
	 * The FieldPanel class contains and visualizes the actual simulation field.
	 */
	public class FieldPanel extends JPanel {

		private static final long serialVersionUID = 1L;
		/** the nodes on the field */
		public HashMap nodes;
		/** if true, draws a circle */
		public boolean drawCircle;
		/** radius of the circle */
		public int circleRadius;
		/** Location where to draw circle */
		public Location circleLoc;
		/** current simulation time */
		public long time;
		/** Displays the streets */
		private BufferedImage streetMap = null;
		/** Amount by which to adjust x coordinate of map points */
		public int mapXDisp = 0;
		public int[] segsToColor = new int[] { -1 };
		public Color[] colors = null;

		/**
		 * FieldPanel constructor.
		 * 
		 * @param nodes
		 *            the vector containing all of the JButtons
		 */
		public FieldPanel(HashMap nodes) {
			super();
			this.nodes = nodes;
			this.setLayout(null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		public void paintComponent(Graphics g) {
			// if (nodes!=null){
			// for (int i = 0; i < nodes.size(); i++){
			// Node n = (Node)nodes.get(new Integer(i));
			// if (n!=null /*&& n.animateRadius*/){
			// n.animateRadius(g);
			// }
			//
			// }
			// }
			g.setColor(getBackground());
			g.fillRect(0, 0, (int) (Node.buttonWidth * 1.4),
					(int) (Node.buttonHeight * 2.2));
			super.paintComponent(g);

			timeLabel.setText("Time: " + time / Constants.SECOND + " s");

			if (nodes != null) {

				if (drawCircle) {
					g.setColor(Color.CYAN);
					g.fillOval(
							(int) (circleLoc.getX() / zoom - circleRadius / 2),
							(int) (circleLoc.getY() / zoom - circleRadius / 2),
							circleRadius, circleRadius);
					g.setColor(Color.BLACK);

				}
				// draw the street map
				if (mob != null) {
					Graphics2D g2 = (Graphics2D) g;
					if (streetMap == null) {
						drawStreetMap();
					}
					g2.drawImage(streetMap, null, 0, 0);
				}
				// draw cars
				for (int i = 0; i < nodes.size(); i++) {
					Node n = (Node) nodes.get(new Integer(i));
					if (n != null /* && n.animateRadius */) {
						n.animateRadius(g);
					}

				}
			} // end case nodes
		}

		/**
		 * Draws roads without coloring segments.
		 * 
		 */
		private void drawStreetMap() {
			uncolorSegments();
			drawStreetMap(new int[] { -1 }, null);

		}

		/**
		 * Draws roads, optionally coloring some of them
		 * 
		 * @param segmentsToColor
		 *            the segments to color differently
		 * @param colors
		 *            the colors to use
		 */
		private void drawStreetMap(int segmentsToColor[], Color colors[]) {
			/* get the segments */
			// StreetMobility sm = je.sm;
			if ((mob == null) || !(mob instanceof StreetMobility))
				return;
			StreetMobility sm = (StreetMobility) mob;
			Vector segments = sm.getSegments();
			Iterator it = segments.iterator();
			/* get the bounds of the map */
			Location start, end;
			Location topLeft = sm.getBounds()[0];
			Location bottomRight = sm.getBounds()[3];
			int x = (int) StrictMath.abs(topLeft.getX() - bottomRight.getX());
			int y = (int) StrictMath.abs(topLeft.getY() - bottomRight.getY());

			/* create the map image */
			streetMap = new BufferedImage((int) (x / zoom), (int) (y / zoom),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2D = streetMap.createGraphics();
			g2D.setColor(Color.black);

			/* paint the map */
			while (it.hasNext()) {
				RoadSegment rs = (RoadSegment) it.next();
				start = rs.getStartPoint();
				end = rs.getEndPoint();

				g2D.setStroke(new BasicStroke(rs.getStrokeWidth()));

				if (rs.getShapeIndex() == -1) {
					g2D.drawLine((int) (start.getX() / zoom) - mapXDisp,
							(int) (start.getY() / zoom),
							(int) (end.getX() / zoom) - mapXDisp,
							(int) (end.getY() / zoom));
				} else {
					HashMap shapes = sm.getShapes();
					Shape s = (Shape) shapes
							.get(new Integer(rs.getShapeIndex()));
					start = rs.getStartPoint();
					for (int i = 0; i < s.points.length; i++) {
						end = s.points[i];
						g2D.drawLine((int) (start.getX() / zoom),
								(int) (start.getY() / zoom),
								(int) (end.getX() / zoom),
								(int) (end.getY() / zoom));
						start = end;
					}
					end = rs.getEndPoint();
					g2D.drawLine((int) (start.getX() / zoom),
							(int) (start.getY() / zoom),
							(int) (end.getX() / zoom),
							(int) (end.getY() / zoom));
				}
			}
			colorMapSegments(segmentsToColor, colors, g2D);
		}

		private void colorMapSegments(int[] segmentsToColor, Color[] colors,
				Graphics2D g2D) {

			segsToColor = segmentsToColor;
			this.colors = colors;
			// StreetMobility sm = je.sm;
			if ((mob == null) || !(mob instanceof StreetMobility))
				return;
			StreetMobility sm = (StreetMobility) mob;
			Vector segments = sm.getSegments();
			Location start;
			Location end;
			// color the segments
			for (int i = 0; i < segmentsToColor.length; i++) {

				if (segmentsToColor[i] >= 0) {
					g2D.setColor(colors[i]);
					RoadSegment rs = (RoadSegment) segments
							.get(segmentsToColor[i]);
					g2D.setStroke(new BasicStroke(rs.getStrokeWidth()));

					start = rs.getStartPoint();
					end = rs.getEndPoint();

					if (rs.getShapeIndex() == -1) {
						g2D.drawLine((int) (start.getX() / zoom) - mapXDisp,
								(int) (start.getY() / zoom),
								(int) (end.getX() / zoom) - mapXDisp,
								(int) (end.getY() / zoom));
					} else {
						HashMap shapes = sm.getShapes();
						Shape s = (Shape) shapes.get(new Integer(rs
								.getShapeIndex()));
						start = rs.getStartPoint();
						for (int j = 0; j < s.points.length; j++) {
							end = s.points[j];
							g2D.drawLine((int) (start.getX() / zoom),
									(int) (start.getY() / zoom),
									(int) (end.getX() / zoom),
									(int) (end.getY() / zoom));
							start = end;
						}
						end = rs.getEndPoint();
						g2D.drawLine((int) (start.getX() / zoom),
								(int) (start.getY() / zoom),
								(int) (end.getX() / zoom),
								(int) (end.getY() / zoom));
					}
				}
			}
		}

		/**
		 * Set the color of a single road segment.
		 * 
		 * @param rs
		 *            the RoadSegment to color.
		 * @param c
		 *            the color to use
		 */
		public void setSegmentColor(RoadSegment rs, Color c) {
			// drawStreetMap(new int[]{rs.getSelfIndex()}, new Color[]{c});
			Graphics2D g2d = streetMap.createGraphics();
			uncolorSegments();
			colorMapSegments(new int[] { rs.getSelfIndex() },
					new Color[] { c }, g2d);
		}

		/**
		 * Sets the color of several segments.
		 * 
		 * @param rss
		 *            RoadSegments to color.
		 * @param colors
		 *            the colors to use
		 */
		public void setSegmentsColor(RoadSegment[] rss, Color colors[]) {
			int[] ids = new int[rss.length];

			for (int i = 0; i < rss.length; i++) {
				ids[i] = rss[i].getSelfIndex();
			}

			uncolorSegments();
			while (streetMap == null)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
				}
			colorMapSegments(ids, colors, streetMap.createGraphics());

		}

		private void uncolorSegments() {
			if (colors != null) {
				Color black[] = new Color[colors.length];
				for (int i = 0; i < colors.length; i++)
					black[i] = Color.BLACK;
				colorMapSegments(segsToColor, black, streetMap.createGraphics());
			}

		}

	}

	/**
	 * Visualizer creates and shows the GUI when an outside synchronized thread
	 * instantiates an instance of it.
	 * 
	 * @param je
	 *            the simulation configuration object
	 */
	public Visualizer(Field field, int x, int y) {
		this.mob = field.getMobility();
		activeInstance = this;
		if (!((mob instanceof StreetMobility) && ((x == 0) || (y == 0)))) {
			this.fieldX = x;
			this.fieldY = y;
		} else {
			Location loc[] = ((StreetMobility) mob).getBounds();
			this.fieldX = (int) StrictMath.abs(loc[3].getX() - loc[0].getX());
			this.fieldY = (int) StrictMath.abs(loc[3].getY() - loc[0].getY());
		}
		nodes = new HashMap<Integer, Node>();

		try {
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (Exception e) {
		}

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

		synchronized (this) {
			while (frame == null) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
		}
		frame.invalidate();
	}

	/**
	 * Creates the field and shows it.
	 */
	private synchronized void createAndShowGUI() {
		JFrame frame = new JFrame("Visualizer");
		Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		if (d.width < fieldX || d.height < fieldY) {
			zoom = StrictMath.max(
					(int) StrictMath.ceil((double) fieldX / d.width), fieldY
							/ (d.height - BOTTOM_OFFSET - totalTopHeight - 20));
		}

		fieldX = StrictMath.max((int) (4 * infoPaneWidth) + 30, fieldX);
		d.setSize(
				StrictMath.min(d.width, fieldX + 10),
				StrictMath.min(d.height - BOTTOM_OFFSET, fieldY / (zoom)
						+ totalTopHeight + 60));

		// ensure proper width for info panes

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(d);
		// frame.setPreferredSize(d);
		frame.setMaximizedBounds(new Rectangle(d.width, d.height));

		contentPane = new JPanel();
		contentPane.setOpaque(true);
		contentPane.setSize(d);
		contentPane.setPreferredSize(d);

		layeredPane = new JLayeredPane();
		layeredPane.setBounds(0, 0, (int) (fieldX / zoom),
				(int) (fieldY / zoom));
		layeredPane.setSize((int) (fieldX / zoom), (int) (fieldY / zoom));
		layeredPane.setPreferredSize(new Dimension((int) (fieldX / zoom),
				(int) (fieldY / zoom)));

		contentPane.setLocation(0, 0);

		panel = new FieldPanel(nodes); // added
		panel.setOpaque(false);

		panel.setBounds(0, 0, (int) (this.fieldX / zoom),
				(int) (this.fieldY / zoom));
		panel.setSize((int) (fieldX / zoom), (int) (fieldY / zoom));
		panel.setPreferredSize(new Dimension((int) (fieldX / zoom),
				(int) (fieldY / zoom)));
		layeredPane.add(panel);

		// This is used for routing-protocol-specific info
		routingEditorPane = new JEditorPane();
		routingEditorPane.setEditable(false);

		// Put the editor pane in a scroll pane.
		JScrollPane editorScrollPane = new JScrollPane(routingEditorPane);
		editorScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		editorScrollPane.setPreferredSize(new Dimension(infoPaneWidth,
				infoPaneHeight));
		editorScrollPane.setMinimumSize(new Dimension(10, 10));
		contentPane.add(editorScrollPane);
		routingEditorPane.setLocation(0, fieldY + 5);
		routingEditorPane.setText("Routing information...");

		JPanel middlePanel = new JPanel(new GridBagLayout());
		middlePanel.setBorder(BorderFactory.createLineBorder(Color.black));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0;
		timeLabel = new JLabel("0 seconds");
		timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		middlePanel.add(timeLabel, c);
		c.gridy++; // for space

		// zoom JSpinner
		JPanel spinnerPanel = new JPanel();
		JLabel spinnerLabel = new JLabel("Zoom factor: ");
		SpinnerModel model = new SpinnerNumberModel(zoom, 0.01, 200, 0.5);
		zoomToggle = new ZoomToggle(model);
		spinnerPanel.add(spinnerLabel);
		spinnerPanel.add(zoomToggle);
		c.gridy++;
		middlePanel.add(spinnerPanel, c);

		// checkbox for communication display
		JPanel checkBoxPanel2 = new JPanel();
		JLabel checkBoxLabel2 = new JLabel("Show communication: ");
		showCommunication = new JCheckBox();
		checkBoxPanel2.add(checkBoxLabel2);
		checkBoxPanel2.add(showCommunication);
		c.gridy++;
		middlePanel.add(checkBoxPanel2, c);

		contentPane.add(middlePanel);

		// key panel
		keyPanel = new JPanel(new GridBagLayout());
		keyPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.gridy = 0;
		JLabel keyLabel = new JLabel("Key");
		keyLabel.setHorizontalAlignment(SwingConstants.CENTER);
		keyPanel.add(keyLabel, c2);
		// keyPanel.setPreferredSize(new Dimension(150, 150));
		// c2.gridy++; // for space
		contentPane.add(keyPanel);

		JPanel textPanel = new JPanel(new GridBagLayout());
		GridBagConstraints textConstraints = new GridBagConstraints();
		textConstraints.fill = GridBagConstraints.VERTICAL;
		textConstraints.gridy = 0;
		// This is used for general info
		generalEditorPane = new JEditorPane();
		generalEditorPane.setEditable(false);

		// Put the editor pane in a scroll pane.
		JScrollPane generalScrollPane = new JScrollPane(generalEditorPane);
		generalScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		generalScrollPane.setPreferredSize(new Dimension(infoPaneWidth,
				infoPaneHeight - 10));
		generalScrollPane.setMinimumSize(new Dimension(10, 10));
		textPanel.add(generalScrollPane, textConstraints);
		// generalEditorPane.setLocation(0, fieldY+5);
		generalEditorPane.setText("General info");

		textConstraints.gridy++;

		contentPane.add(textPanel);

		fieldPane = new JScrollPane(layeredPane);
		fieldPane.setSize(d.width, (int) (fieldY / zoom + 20));
		fieldPane.setPreferredSize(new Dimension(d.width,
				(int) (fieldY / zoom + 20)));

		contentPane.add(fieldPane);

		frame.setContentPane(contentPane);

		// show frame
		frame.pack();
		frame.setVisible(true);

		layeredPane.repaint();

		Visualizer.frame = frame;
		notifyAll();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#displaceNode(jist.swans.misc.Location,
	 * int, jist.swans.misc.Location)
	 */
	public void displaceNode(Location step, int ip) {

		Node n = (Node) nodes.get(new Integer(ip));
		n.x += step.getX();
		n.y += step.getY();
		n.updateLocation(n.x, n.y, field);
		n.setToolTipText("Node " + ip + " @ (" + n.x + ", " + n.y + ")");

		n.repaint();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#setFocus(int)
	 */
	public void setFocus(int ip) {
		Node n = (Node) nodes.get(new Integer(ip));
		n.requestFocusInWindow();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#drawTransmitCircle(int)
	 */
	public void drawTransmitCircle(int ip) {
		Node n = (Node) nodes.get(new Integer(ip));
		n.showRadius = true;
		n.radioRadius = radioRadius;
		panel.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#drawAnimatedTransmitCircle(int,
	 * java.awt.Color)
	 */
	public void drawAnimatedTransmitCircle(int ip, Color color) {
		Node n = (Node) nodes.get(new Integer(ip));
		if (n != null) {
			n.radioRadius = radioRadius;
			n.animateRadius();
			n.radiusColor = color;
			panel.repaint();
		} else {
			System.out.println("Visualizer: Could not find node " + ip);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#hideTransmitCircle(int)
	 */
	public void hideTransmitCircle(int ip) {
		Node n = (Node) nodes.get(new Integer(ip));
		n.showRadius = false;
		panel.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#setNodeColor(int, java.awt.Color)
	 */
	public void setNodeColor(int i, Color c) {
		Node n = (Node) nodes.get(new Integer(i));
		if (n == null) {
			return;
		}

		n.setBackground(c);
		// n.setColor(c);
		panel.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#setToolTip(int, java.lang.String)
	 */
	public void setToolTip(int ip, String text) {
		Node n = (Node) nodes.get(new Integer(ip));
		n.setToolTipText(text);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#setRoutingPaneText(int, java.lang.String)
	 */
	public void setRoutingPaneText(int ip, String text) {
		if (UPDATE_ROUTING_TEXT)
			routingEditorPane.setText("Node " + ip + ":\n" + text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#setGeneralPaneText(java.lang.String)
	 */
	public void setGeneralPaneText(String text) {
		generalEditorPane.setText(text);
	}

	/**
	 * Sets the max base distance that a message will propagate (i.e., within 1
	 * std dev).
	 * 
	 * @param maxDistance
	 */
	public void setBaseTranmit(double maxDistance) {
		this.radioRadius = maxDistance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#resetColors()
	 */
	public void resetColors() {
		Iterator it = nodes.values().iterator();
		while (it.hasNext()) {
			Node n = (Node) it.next();
			n.resetColor();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#drawCircle(int, jist.swans.misc.Location)
	 */
	public void drawCircle(int r, Location loc) {
		// TODO update such that center of circle is at loc
		FieldPanel fp = (FieldPanel) panel;
		fp.circleRadius = r;
		fp.circleLoc = loc;
		fp.drawCircle = true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#removeCircle()
	 */
	public void removeCircle() {
		FieldPanel fp = (FieldPanel) panel;
		fp.drawCircle = false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * driver.VisualizerInterface#colorSegment(jist.swans.field.streets.RoadSegment
	 * , java.awt.Color)
	 */
	public void colorSegment(RoadSegment rs, Color c) {
		((FieldPanel) panel).setSegmentColor(rs, c);
		((FieldPanel) panel).repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#colorSegments(java.lang.Object[],
	 * java.awt.Color[])
	 */
	public void colorSegments(Object[] objects, Color colors[]) {

		RoadSegment[] rs = new RoadSegment[objects.length];
		for (int i = 0; i < objects.length; i++)
			rs[i] = (RoadSegment) objects[i];
		((FieldPanel) panel).setSegmentsColor(rs, colors);
		((FieldPanel) panel).repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#colorSegments(java.lang.Object[],
	 * java.awt.Color)
	 */
	public void colorSegments(Object[] objects, Color color) {
		Color[] colors = new Color[objects.length];
		RoadSegment[] rs = new RoadSegment[objects.length];
		for (int i = 0; i < objects.length; i++) {
			rs[i] = (RoadSegment) objects[i];
			colors[i] = color;
		}
		((FieldPanel) panel).setSegmentsColor(rs, colors);
		((FieldPanel) panel).repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#removeNode(int)
	 */
	public void removeNode(int id) {

		Node n = (Node) nodes.remove(new Integer(id));
		panel.remove(n);
		n.setVisible(false);

		panel.repaint();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#updateVisualizer()
	 */
	public void updateVisualizer() {
		Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		d.setSize(
				StrictMath.min(d.width, fieldX + 30),
				StrictMath.min(d.height - BOTTOM_OFFSET, fieldY / (zoom)
						+ totalTopHeight + 90));

		frame.setSize(d);
		// frame.setPreferredSize(d);
		frame.setMaximizedBounds(new Rectangle(d.width, d.height));

		contentPane.setSize(d);
		contentPane.setPreferredSize(d);

		layeredPane.setBounds(0, 0, (int) (fieldX / zoom),
				(int) (fieldY / zoom));
		layeredPane.setSize((int) (fieldX / zoom), (int) (fieldY / zoom));
		layeredPane.setPreferredSize(new Dimension((int) (fieldX / zoom),
				(int) (fieldY / zoom)));

		panel.setBounds(0, 0, (int) (this.fieldX / zoom),
				(int) (this.fieldY / zoom));
		panel.setSize((int) (fieldX / zoom), (int) (fieldY / zoom));
		panel.setPreferredSize(new Dimension((int) (fieldX / zoom),
				(int) (fieldY / zoom)));

		fieldPane.setSize(d.width - 30, d.height - totalTopHeight - 30);
		fieldPane.setPreferredSize(new Dimension(d.width - 30, d.height
				- totalTopHeight - 60));
		fieldPane.setLocation(0, totalTopHeight + 10);

		FieldPanel fp = ((FieldPanel) panel);
		if (mob != null)
			fp.drawStreetMap(fp.segsToColor, fp.colors);
		frame.pack();
		if (nodes != null) {
			for (int i = 0; i < nodes.size(); i++) {
				Node n = (Node) nodes.get(new Integer(i));
				if (n != null)
					n.updateLocation(n.x, n.y, field);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#addNode(float, float, int)
	 */
	public void addNode(float initX, float initY, int ip) {
		// add node to field
		Node n = new Node(initX, initY, ip, mob);
		n.radioRadius = radioRadius;
		nodes.put(new Integer(ip), n);
		panel.add(n);
		((FieldPanel) panel).nodes = nodes;
		n.setLocation((int) (initX / zoom), (int) (initY / zoom));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#drawCircle(int, int)
	 */
	public void drawCircle(int ip, int r) {
		FieldPanel fp = (FieldPanel) panel;
		fp.circleRadius = r;
		Node n = (Node) nodes.get(new Integer(ip));
		fp.circleLoc = new Location.Location2D(n.x, n.y);
		fp.drawCircle = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#setNumberOfNodes(int)
	 */
	public void setNumberOfNodes(int totalNodes) {
		nodes = new HashMap<Integer, Node>(totalNodes);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#showCommunication()
	 */
	public boolean showCommunication() {
		return showCommunication.isSelected();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#setField(jist.swans.field.Field)
	 */
	public void setField(Field f) {

		this.field = f;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#resetColor(int)
	 */
	public void resetColor(int nodenum) {
		Node n = (Node) nodes.get(new Integer(nodenum));
		n.resetColor();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#updateTime(long)
	 */
	public void updateTime(long time) {
		((FieldPanel) panel).time = time;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#registerKeyItem(java.awt.Color, int,
	 * java.lang.String)
	 */
	public void registerKeyItem(Color c, int type, String text) {
		switch (type) {
		case Visualizer.CIRCLE:
			int x = 10;
			int y = 10;
			JPanelCircle jp = new JPanelCircle(c, x, y);
			jp.setSize(new Dimension(x, y));
			c2.gridy++;
			keyPanel.add(jp, c2);
			// c2.gridx++;
			JLabel jl = new JLabel(text);
			keyPanel.add(jl, c2);
			// c2.gridx--;
			break;
		case Visualizer.CAR:
			JButton jb = new JButton();
			jb.setSize(new Dimension(Node.buttonWidth, Node.buttonHeight));
			jb.setBorderPainted(true);
			jb.setBorder(new LineBorder(c, 3));
			c2.gridy++;
			keyPanel.add(jb, c2);
			// c2.gridx++;
			JLabel jl2 = new JLabel(text);
			keyPanel.add(jl2, c2);
			// c2.gridx--;
			break;
		default:
			throw new RuntimeException("Unsupported!");
		}
		frame.pack();
	}

	public static Visualizer getActiveInstance() {
		return activeInstance;
	}

	/**
	 * Return true if the node is still on the map; false otherwise.
	 * 
	 * @param ip
	 *            the node identifier
	 * @return
	 */
	public boolean hasNode(int ip) {
		return nodes.get(new Integer(ip)) != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see driver.VisualizerInterface#updateNodeLocation(float, float, int)
	 */
	public void updateNodeLocation(float newX, float newY, int ip) {
		Node n = (Node) nodes.get(new Integer(ip));
		n.updateLocation(newX, newY, field);
		n.repaint();

	}

}