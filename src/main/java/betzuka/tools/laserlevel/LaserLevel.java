package betzuka.tools.laserlevel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import betzuka.tools.laserlevel.camera.SaxosCamera;


public class LaserLevel extends JFrame {

	
	private FrameAnalyzer analyser;
	private AnalyzedFrame frame;
	private Measurement measurement;
	private Settings settings = new Settings();
	private JDialog settingsDialog;
	
	private static class Measurement {
		private Double zero;

		public Double getZero() {
			return zero;
		}

		public void setZero(Double zero) {
			this.zero = zero;
		}

	}

	public double pixelToUM(double pixel) {
		return pixel * settings.getUmPerPixel();
	}
	
	public LaserLevel() {
		
		analyser = new FrameAnalyzer(new SaxosCamera(settings), settings);
		measurement = new Measurement();

		setLayout(new BorderLayout());
		Dimension dim = new Dimension(analyser.getWidth(), analyser.getHeight());
		JPanel content = new JPanel(new FlowLayout());
		JPanel curvePanel = new CurvePanel();
		curvePanel.setPreferredSize(dim);
		JPanel vidPanel = new VidPanel();
		vidPanel.setPreferredSize(dim);
		content.add(vidPanel);
		content.add(curvePanel);
		add(content, BorderLayout.CENTER);
	
		add(new MeasurementPanel(), BorderLayout.SOUTH);

		settingsDialog = new BeanDialog(this, "Preferences", settings);
		
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Options");
		
		JMenuItem item = new JMenuItem("Preferences");
		item.addActionListener((e) -> {
			settingsDialog.setVisible(true);
		});
		
		menu.add(item);
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
		
	}


	public void updateLevel() {
		frame = analyser.analyzeNextFrame();
		repaint();
	}

	private class MeasurementPanel extends JPanel {
		private JLabel error;

		private MeasurementPanel() {
			JButton zeroButton = new JButton("ZERO");
			zeroButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (frame!=null) {
						measurement.setZero(frame.getMaxima());
					} else {
						measurement.setZero(null);
					}
				}
			});
			error = new JLabel("");
			add(zeroButton);
			add(error);
		}

		@Override
	    public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (frame!=null && frame.hasFit() && measurement.getZero()!=null) {
				error.setText(String.format("%.4f", pixelToUM(frame.getMaxima()-measurement.getZero())));
			} else {
				error.setText("");
			}
		}
	}

	private class VidPanel extends JPanel {
		@Override
	    public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (frame!=null) {
				g.drawImage(frame.getImg(), 0, 0, null);
			}
		}
	}

	private class CurvePanel extends JPanel {
		@Override
	    public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (frame!=null) {
				g.setColor(Color.GREEN);
				double [] curve = frame.getIntensityCurve();
				for (int i=0;i<curve.length;i++) {
					g.drawLine(0, i, (int) Math.round(curve[i]*getWidth()), i);
				}
				if (frame.hasFit()) {
					g.setColor(Color.RED);
					int maxAt = (int)Math.round(frame.getMaxima());
					g.drawLine(0, maxAt, getWidth(), maxAt);
				}
				if (measurement.getZero()!=null) {
					g.setColor(Color.BLUE);
					int maxAt = (int)Math.round(measurement.getZero());
					g.drawLine(0, maxAt, getWidth(), maxAt);
				}
			}
		}
	}


	public static void main(String [] args) throws Exception {
		LaserLevel m = new LaserLevel();
		m.setSize(1920, 1024);
		m.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m.setVisible(true);

		while (true) {
			m.updateLevel();
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {}
		}
	}

}
