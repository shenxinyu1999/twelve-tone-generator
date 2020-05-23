package test;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class TwelveToneGenerator implements ActionListener {

	static Sequencer sequencer;
	JFrame frame;
	JPanel panel = new JPanel();
	JTable table;
	JTextField field;

	Integer[] toneRow = generateToneRow();
	Integer[] inverse = generateBaseInv();

	public Integer[] generateToneRow() {
		ArrayList<Integer> notes = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
		int randomShuffleTimes = (int) Math.floor(Math.random() * 10 + 10);

		for (int i = 0; i < randomShuffleTimes; i++) {
			Collections.shuffle(notes);
		}

		return notes.toArray(new Integer[12]);
	}

	public Integer[] generateBaseInv() {
		Integer[] inv = new Integer[12];

		inv[0] = toneRow[0];
		for (int i = 1; i < 12; i++) {
			int diff = toneRow[i] - toneRow[0];
			inv[i] = (toneRow[0] - diff + 12) % 12;
		}

		return inv;
	}

	public Integer[][] generateMatrix() {
		Integer[][] matrix = new Integer[12][12];

		for (int i = 0; i < 12; i++) {
			matrix[0][i] = toneRow[i];
			matrix[i][0] = inverse[i];
		}

		for (int i = 1; i < 12; i++) {
			Integer[] newRow;
			newRow = transpose(matrix[i][0]);
			for (int j = 1; j < 12; j++) {
				matrix[i][j] = newRow[j];
			}
		}

		return matrix;
	}

	public Integer[] transpose(Integer startNote) {
		Integer[] row = new Integer[12];

		row[0] = startNote;

		int diff = row[0] - toneRow[0];
		if (diff < 0)
			diff += 12;

		for (int i = 1; i < 12; i++) {
			row[i] = (toneRow[i] + diff) % 12;
		}

		return row;
	}

	public static void main(String[] args) throws MidiUnavailableException {
		sequencer = MidiSystem.getSequencer();
		sequencer.open();

		TwelveToneGenerator gui = new TwelveToneGenerator();

		gui.setGUI();
	}

	public void setGUI() {
		frame = new JFrame("Twelve tone generator!!!");

		JButton generate = new JButton("Generate!");
		generate.addActionListener(this);

		field = new JTextField();
		field.setPreferredSize(new Dimension(200, 25));

		panel.add(generate);
		panel.add(field);

		frame.getContentPane().add(BorderLayout.NORTH, panel);
		frame.setSize(500, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source instanceof JButton) {
			boolean status = false;
			Integer[] inputRow = new Integer[12];
			String input = field.getText();
			String[] split = input.split(",");

			if (input.length() > 0 && split.length == 12) {
				status = true;

				for (int i = 0; i < 12; i++) {
					inputRow[i] = Integer.parseInt(split[i]);
				}
			}

			if (status)
				toneRow = inputRow;
			else
				toneRow = generateToneRow();
			inverse = generateBaseInv();

			Integer[][] matrix = generateMatrix();

			Object[][] data = new Object[14][14];

			for (int i = 0; i < 14; i++) {
				data[0][i] = "v";
				data[13][i] = "^";
				data[i][0] = ">";
				data[i][13] = "<";
			}

			data[0][0] = "";
			data[13][13] = "";
			data[13][0] = "";
			data[0][13] = "";

			for (int i = 1; i < 13; i++) {
				for (int j = 1; j < 13; j++) {
					data[i][j] = matrix[i - 1][j - 1];
				}
			}

			if (table != null)
				frame.getContentPane().remove(table);

			String[] columnNames = { "", "", "", "", "", "", "", "", "", "", "", "", "", "" };
			table = new JTable(data, columnNames) {
				public boolean editCellAt(int row, int column, java.util.EventObject e) {
					return false;
				}
			};
			table.setCellSelectionEnabled(true);

			table.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent evt) {
					int row = table.rowAtPoint(evt.getPoint());
					int col = table.columnAtPoint(evt.getPoint());
					if (row >= 0 && col >= 0) {
						Integer[] rowToHear = new Integer[12];
						if (col == 0 && row > 0 && row < 12) {
							try {
								generateSound(matrix[row - 1]);
							} catch (InvalidMidiDataException e) {
								e.printStackTrace();
							}
						}else if (row == 0 && col > 0 && col < 12) {
							for (int i = 0; i < 12; i++) {
								rowToHear[i] = matrix[i][col - 1];
							}
							
							try {
								generateSound(rowToHear);
							} catch (InvalidMidiDataException e) {
								e.printStackTrace();
							}
						}else if (row == 13 && col > 0 && col < 12) {
							for (int i = 0; i < 12; i++) {
								rowToHear[i] = matrix[11 - i][col - 1];
							}
							
							try {
								generateSound(rowToHear);
							} catch (InvalidMidiDataException e) {
								e.printStackTrace();
							}
						}else if (col == 13 && row > 0 && row < 12) {
								for (int i = 0; i < 12; i++) {
									rowToHear[i] = matrix[row - 1][11 - i];
								}
								
								try {
									generateSound(rowToHear);
								} catch (InvalidMidiDataException e) {
									e.printStackTrace();
								}
						}	
					}
				}
			});

			for (int i = 0; i < 14; i++) {
				TableColumn column = table.getColumnModel().getColumn(i);
				column.setPreferredWidth(20);
			}

			frame.getContentPane().add(BorderLayout.SOUTH, table);
			frame.repaint();
			frame.pack();
		}
	}

	public void generateSound(Integer[] matrix) throws InvalidMidiDataException {
		Sequence sequence = new Sequence(Sequence.PPQ, 4);
		Track track = sequence.createTrack();

		for (int row = 0; row < 12; row++) {
			ShortMessage first = new ShortMessage();
			first.setMessage(192, 1, 1, 0);
			MidiEvent changeInstrument = new MidiEvent(first, 1);
			track.add(changeInstrument);

			ShortMessage onMessage = new ShortMessage();
			onMessage.setMessage(144, 1, matrix[row] + 36, 100);
			MidiEvent noteOn = new MidiEvent(onMessage, row * 3 + 1);
			track.add(noteOn);

			ShortMessage offMessage = new ShortMessage();
			offMessage.setMessage(128, 1, matrix[row] + 36, 100);
			MidiEvent noteOff = new MidiEvent(offMessage, row * 3 + 4);
			track.add(noteOff);

		}

		sequencer.setSequence(sequence);
		sequencer.start();

	}
}