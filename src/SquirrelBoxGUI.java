/*
SquirrelBox 1.6 alpha 6.
Copyright (C) 2007 Dan Cook

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

import java.io.*;
import javax.swing.*;
import java.awt.event.*;

import java.util.*;
import java.io.*;

public abstract class SquirrelBoxGUI extends JFrame
implements javax.swing.event.DocumentListener, ActionListener, WindowListener, Runnable {
//GUI vars
	public javax.swing.filechooser.FileFilter adsFiles;
	public javax.swing.filechooser.FileFilter asmFiles;
	public javax.swing.filechooser.FileFilter txtFiles;
	public JTextArea[] asmOutput;
	public JFileChooser chooser;
	public JCheckBox hideLegal;
	public JCheckBox exitSave;
	public JCheckBox forgetMe;
	public JCheckBox exitAsk;
	public JCheckBox asmSave;
	public JCheckBox asmComp;
	public JCheckBox makeFun;
	public JLabel statusBar;
	public boolean firstTime;
	public boolean madeFunOf;
	public JDialog optionBox;
	public boolean compileAsm;
	public JDialog commandBox;
	public JTextArea commands;
	public JTabbedPane tpane;
    public File[] tabFiles;
    public File srcDir;
    public File asmDir;
    public File exeDir;
    public File incDir;

// Used to choose default directories
	public JDialog directories;
	public JTextField srcIn;
	public JTextField asmIn;
	public JTextField incIn;
	public JTextField exeIn;

//Compiler vars
	public boolean stopCompiling;
	public File[] sourceFiles;
	public String[] asmNames;
	public boolean[] asmUsed;
    private Thread compiler;
	public boolean loading;
	public String[] code;
	public function func;
	public int current;
	public Parser in;

//Static vars
	public static final int TI82 = 0;
	public static final int TI83 = 1;
	public static final int TI83_PLUS = 2;
	public static final int TI83_PLUS_SE = 3;
	public static final int TI84_PLUS = 4;
	public static final int TI84_PLUS_SE = 5;
	public static final int TI85 = 6;
	public static final int TI86 = 7;
	public static final int GENERAL_Z80 = 8;
	public static final int ASM_FEEDBACK = 9;

	public static final ImageIcon ADS_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("ADS.GIF"));
	public static final ImageIcon NEW_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("NEW.GIF"));
	public static final ImageIcon TXT_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("TXT.GIF"));
	public static final ImageIcon Z80_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("Z80.GIF"));
	public static final ImageIcon CALC_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("CALC.GIF"));
	public static final ImageIcon HELP_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("HELP.GIF"));
	public static final ImageIcon OPEN_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("OPEN.GIF"));
	public static final ImageIcon SAVE_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("SAVE.GIF"));
	public static final ImageIcon STOP_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("STOP.GIF"));
	public static final ImageIcon CLOSE_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("CLOSE.GIF"));
	public static final ImageIcon COMPILE_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("COMPILE.GIF"));
	public static final ImageIcon GENERAL_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("GENERAL.GIF"));
	public static final ImageIcon FEEDBACK_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("FEEDBACK.GIF"));
	public static final ImageIcon SAVE_ALL_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("SAVE_ALL.GIF"));
	public static final ImageIcon SQUIRREL_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("SQUIRREL.GIF"));
	public static final ImageIcon CLOSE_ALL_ICON = new ImageIcon(SquirrelBoxGUI.class.getResource("CLOSE_ALL.GIF"));

	public static final String VERSION = "SquirrelBox 1.6 alpha 6";

	public SquirrelBoxGUI() { super(VERSION); }

	public void init(String[] args) {
		compileAsm = false;
		tabFiles = new File[0];
		asmNames = new String[] {
			"TI-82","TI-83","TI-83+","TI-83+ SE",
			"TI-84+","TI-84+ SE","TI-85","TI-86",
			"General z80", "Assembler Feedback"
		}; asmUsed = new boolean[asmNames.length-1];
		asmOutput = new JTextArea[asmNames.length];
		sourceFiles = new File[asmNames.length];
		code = new String[asmUsed.length];
		compiler = new Thread(this);

		for(int i=0; i<asmOutput.length; i++) {
			asmOutput[i] = new JTextArea();
			asmOutput[i].setEditable(false);
			asmOutput[i].setTabSize(4);
			asmOutput[i].setFont(
				new java.awt.Font(
					"Lucida Console",
					(i == ASM_FEEDBACK)? java.awt.Font.BOLD : java.awt.Font.PLAIN,
					12
				)
			);
		}

		asmOutput[ASM_FEEDBACK].setBackground(java.awt.Color.black);

		try {
			javax.swing.UIManager.setLookAndFeel(
				javax.swing.UIManager.getSystemLookAndFeelClassName()
			);
		} catch(Exception e) { }

        setIconImage(SQUIRREL_ICON.getImage());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new java.awt.BorderLayout(2,2));

		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File"); menu.setMnemonic('F');

		JMenuItem item = new JMenuItem("New", NEW_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
		item.setMnemonic('N'); item.setActionCommand("N");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Open", OPEN_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
		item.setMnemonic('O'); item.setActionCommand("O");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Close");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
		item.setMnemonic('C'); item.setActionCommand("C");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Close All");
		item.setMnemonic('E'); item.setActionCommand("E");
		item.addActionListener(this); menu.add(item);

		menu.addSeparator();

		item = new JMenuItem("Save", SAVE_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
		item.setMnemonic('S'); item.setActionCommand("S");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Save As");
		item.setMnemonic('A'); item.setActionCommand("A");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Save All", SAVE_ALL_ICON);
		item.setMnemonic('L'); item.setActionCommand("L");
		item.addActionListener(this); menu.add(item);

		menu.addSeparator();

		item = new JMenuItem("Exit");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK));
		item.setMnemonic('X'); item.setActionCommand("X");
		item.addActionListener(this); menu.add(item);

		menuBar.add(menu);
		menu = new JMenu("Build"); menu.setMnemonic('B');

		item = new JMenuItem("Compile", COMPILE_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		item.setMnemonic('C'); item.setActionCommand("M");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Stop Compiling", STOP_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
		item.setMnemonic('S'); item.setActionCommand("Z");
		item.addActionListener(this); menu.add(item);

		menuBar.add(menu);
		menu = new JMenu("Options"); menu.setMnemonic('O');

		item = new JMenuItem("Preferences");
		item.setMnemonic('P'); item.setActionCommand("m");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Default Directories");
		item.setMnemonic('D'); item.setActionCommand("D");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Assembly Compilation");
		item.setMnemonic('A'); item.setActionCommand("^");
		item.addActionListener(this); menu.add(item);

		menuBar.add(menu);
		menu = new JMenu("Show Output"); menu.setMnemonic('S');

		item = new JMenuItem("TI-82", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK));
		item.setMnemonic('2'); item.setActionCommand("2");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("TI-83", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.CTRL_MASK));
		item.setMnemonic('3'); item.setActionCommand("3");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("TI-83 Plus", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.CTRL_MASK|KeyEvent.SHIFT_MASK));
		item.setMnemonic('P'); item.setActionCommand("P");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("TI-83+ SE", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.CTRL_MASK|KeyEvent.ALT_MASK));
		item.setMnemonic('S'); item.setActionCommand("T");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("TI-84 Plus", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.CTRL_MASK));
		item.setMnemonic('4'); item.setActionCommand("4");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("TI-84+ SE", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.CTRL_MASK|KeyEvent.SHIFT_MASK));
		item.setMnemonic('E'); item.setActionCommand("I");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("TI-85", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, KeyEvent.CTRL_MASK));
		item.setMnemonic('5'); item.setActionCommand("5");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("TI-86", CALC_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, KeyEvent.CTRL_MASK));
		item.setMnemonic('6'); item.setActionCommand("6");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("General z80", GENERAL_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK));
		item.setMnemonic('G'); item.setActionCommand("G");
		item.addActionListener(this); menu.add(item);

		item = new JMenuItem("Assembler Feedback", FEEDBACK_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK));
		item.setMnemonic('F'); item.setActionCommand("F");
		item.addActionListener(this); menu.add(item);

		menuBar.add(menu);
		menu = new JMenu("Help"); menu.setMnemonic('H');

		item = new JMenuItem("About", HELP_ICON);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		item.setMnemonic('A'); item.setActionCommand("B");
		item.addActionListener(this); menu.add(item);

		menuBar.add(menu);
		setJMenuBar(menuBar);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(5));

		java.awt.Insets insets = new java.awt.Insets(2,2,2,2);

		JButton button = new JButton(NEW_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("New"); button.setActionCommand("N");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(10));

		button = new JButton(OPEN_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("Open"); button.setActionCommand("O");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(20));

		button = new JButton(SAVE_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("Save"); button.setActionCommand("S");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(10));

		button = new JButton(SAVE_ALL_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("Save All"); button.setActionCommand("L");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(20));

		button = new JButton(CLOSE_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("Close"); button.setActionCommand("C");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(10));

		button = new JButton(CLOSE_ALL_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("Close All"); button.setActionCommand("E");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(20));

		button = new JButton(COMPILE_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("Compile"); button.setActionCommand("M");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(10));

		button = new JButton(STOP_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("Stop Compiling"); button.setActionCommand("Z");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(20));

		box.add(Box.createHorizontalGlue());
		button = new JButton(HELP_ICON);
		button.setContentAreaFilled(false); button.setBorderPainted(false);
		button.setToolTipText("About "+VERSION); button.setActionCommand("B");
		button.addActionListener(this); button.setMargin(insets);
		box.add(button); box.add(Box.createHorizontalStrut(5));

		getContentPane().add(box, java.awt.BorderLayout.NORTH);

		tpane = new JTabbedPane(SwingConstants.BOTTOM);
		getContentPane().add(tpane, java.awt.BorderLayout.CENTER);

		statusBar = new JLabel(" ");
		getContentPane().add(statusBar, java.awt.BorderLayout.SOUTH);

		chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);

		txtFiles = new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String name = f.getName();
				return
					name.lastIndexOf(".txt") == name.length()-4 ||
					name.indexOf('.') == -1;
			}
			public String getDescription() {
				return "Text files (.txt)";
			}
		}; chooser.addChoosableFileFilter(txtFiles);

		asmFiles = new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String name = f.getName();
				return
					name.lastIndexOf(".z80") == name.length()-4 ||
					name.lastIndexOf(".asm") == name.length()-4 ||
					name.lastIndexOf(".inc") == name.length()-4 ||
					name.indexOf('.') == -1;
			}
			public String getDescription() {
				return "z80 Assembly files (.z80 .asm .inc)";
			}
		}; chooser.addChoosableFileFilter(asmFiles);

		adsFiles = new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String name = f.getName();
				return
					name.lastIndexOf(".ads") == name.length()-4 ||
					name.indexOf('.') == -1;
			}
			public String getDescription() {
				return "ADSMBLG source files (.ads)";
			}
		}; chooser.setFileFilter(adsFiles);

		JPanel panel = new JPanel(new java.awt.GridLayout(4,1));

		button = new JButton("Source", ADS_ICON);
		button.setActionCommand("_S"); button.addActionListener(this);
		panel.add(button);

		button = new JButton("Include", ADS_ICON);
		button.setActionCommand("_I"); button.addActionListener(this);
		panel.add(button);

		button = new JButton("Assembly", Z80_ICON);
		button.setActionCommand("_A"); button.addActionListener(this);
		panel.add(button);

		button = new JButton("z80 Exec", GENERAL_ICON);
		button.setActionCommand("_E"); button.addActionListener(this);
		panel.add(button);

		chooser.setAccessory(panel);

		File tempFile = chooser.getCurrentDirectory();
		File sb = new File("SquirrelBox.jar");
		if(sb.exists())
			chooser.setCurrentDirectory(sb);
		File start = chooser.getCurrentDirectory();
		srcDir = new File(start, "Source");
		incDir = new File(srcDir, "Include");
		asmDir = new File(start, "Asm");
		exeDir = new File(start, "Exec");

		srcIn = new JTextField(50);
		asmIn = new JTextField(50);
		incIn = new JTextField(50);
		exeIn = new JTextField(50);

		directories = new JDialog(this, VERSION+" - Default Directories", true);
		directories.setDefaultCloseOperation(HIDE_ON_CLOSE);
		directories.setLocation(150, 100);
		directories.setResizable(false);

		JPanel padding = new JPanel();
		padding.setLayout(new BoxLayout(padding, BoxLayout.Y_AXIS));

		padding.add(Box.createVerticalStrut(5));

		panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
		panel.add(new JLabel("Source Files:"));
		padding.add(panel);

		button = new JButton(OPEN_ICON);
		button.setMargin(new java.awt.Insets(2,2,2,2));
		button.addActionListener(this);
		button.setActionCommand("dS");

		panel = new JPanel(new java.awt.FlowLayout());
		panel.add(srcIn); panel.add(button); padding.add(panel);

		padding.add(Box.createVerticalStrut(2));

		panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
		panel.add(new JLabel("Include Files:"));
		padding.add(panel);

		button = new JButton(OPEN_ICON);
		button.setMargin(new java.awt.Insets(2,2,2,2));
		button.addActionListener(this);
		button.setActionCommand("dI");

		panel = new JPanel(new java.awt.FlowLayout());
		panel.add(incIn); panel.add(button); padding.add(panel);

		padding.add(Box.createVerticalStrut(2));

		panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
		panel.add(new JLabel("Assembly Files:"));
		padding.add(panel);

		button = new JButton(OPEN_ICON);
		button.setMargin(new java.awt.Insets(2,2,2,2));
		button.addActionListener(this);
		button.setActionCommand("dA");

		panel = new JPanel(new java.awt.FlowLayout());
		panel.add(asmIn); panel.add(button); padding.add(panel);

		padding.add(Box.createVerticalStrut(2));

		panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
		panel.add(new JLabel("Executable Files:"));
		padding.add(panel);

		button = new JButton(OPEN_ICON);
		button.setMargin(new java.awt.Insets(2,2,2,2));
		button.addActionListener(this);
		button.setActionCommand("dE");

		panel = new JPanel(new java.awt.FlowLayout());
		panel.add(exeIn); panel.add(button); padding.add(panel);

		padding.add(Box.createVerticalStrut(2));

		panel = new JPanel(new java.awt.FlowLayout());

		button = new JButton("OK");
		button.setMargin(new java.awt.Insets(2,30,2,30));
		button.addActionListener(this);
		button.setActionCommand("dO");
		panel.add(button);

		panel.add(Box.createHorizontalStrut(2));

		button = new JButton("Cancel");
		button.setMargin(new java.awt.Insets(2,20,2,20));
		button.addActionListener(this);
		button.setActionCommand("dC");

		panel.add(button);
		padding.add(panel);
		padding.add(Box.createVerticalStrut(5));
		directories.getContentPane().add(padding);
        directories.pack();

		optionBox = new JDialog(this, VERSION+" - Options", true);
		optionBox.setDefaultCloseOperation(HIDE_ON_CLOSE);
		optionBox.setLayout(new java.awt.GridLayout(7,1));
		optionBox.setLocation(150, 100);
		optionBox.setResizable(false);

		exitAsk = new JCheckBox("Confirm before exiting SquirrelBox", true);
		hideLegal = new JCheckBox("Do not show the legal info on startup", false);
		exitSave = new JCheckBox("Automatically save everything on exit", false);
		asmComp = new JCheckBox("Auto-compile the assembly output code", false);
		asmSave = new JCheckBox("Auto-save the assembly output code", false);
		forgetMe = new JCheckBox("Reset settings after exiting SquirrelBox", false);
		makeFun = new JCheckBox("Stop making fun of me for using '+' :-(", false);

		asmSave.setActionCommand("<"); asmSave.addActionListener(this);
		asmComp.setActionCommand(">"); asmComp.addActionListener(this);
		makeFun.setActionCommand("+"); makeFun.addActionListener(this);

		button = new JButton("Finished");
		button.addActionListener(this);
		button.setActionCommand("o");

		java.awt.Container c = optionBox.getContentPane();

		c.add(exitAsk);  c.add(hideLegal);
		c.add(exitSave); c.add(asmSave);
		c.add(asmComp);  c.add(forgetMe);
		c.add(button);

		optionBox.pack();

		commandBox = new JDialog(this, VERSION+" - Assembly compilation commands", true);
		commandBox.setDefaultCloseOperation(HIDE_ON_CLOSE);
		commandBox.setLayout(new java.awt.BorderLayout());
		commandBox.setLocation(150, 100);

		commands = new JTextArea(5,50);
		commands.setText("C:\\Asm\\Tasm\\Asm.bat <asmName>");

		button = new JButton("Finished");
		button.addActionListener(this);
		button.setActionCommand(".");

		commandBox.getContentPane().add(commands, java.awt.BorderLayout.CENTER);
		commandBox.getContentPane().add(button, java.awt.BorderLayout.SOUTH);
		commandBox.pack();

		addWindowListener(this);

        setBounds(75, 50, 650, 550);
        setVisible(true);

		firstTime = false;
		madeFunOf = false;
		if(args.length > 0) open(args);

		File scDir = null;
		File asDir = null;
		File inDir = null;
		File exDir = null;
		String cmmnds = null;
		boolean hl, es, ea, ac, as, mf, mfo;

		try {
			DataInputStream dataIn = new DataInputStream(
	    		new FileInputStream("SquirrelBox.dat")
	    	);

			if(dataIn.readBoolean()) {
				forgetMe.setSelected(true);
				throw new IOException();
			}

			String name = "";
			int size = dataIn.readInt();
			for(int i=0; i<size; i++)
				name += dataIn.readChar();
			scDir = new File(name);

			name = "";
			size = dataIn.readInt();
			for(int i=0; i<size; i++)
				name += dataIn.readChar();
			asDir = new File(name);

			name = "";
			size = dataIn.readInt();
			for(int i=0; i<size; i++)
				name += dataIn.readChar();
			inDir = new File(name);

			name = "";
			size = dataIn.readInt();
			for(int i=0; i<size; i++)
				name += dataIn.readChar();
			exDir = new File(name);

			name = "";
			size = dataIn.readInt();
			for(int i=0; i<size; i++)
				name += dataIn.readChar();
			cmmnds = name;

			hl = dataIn.readBoolean();
			es = dataIn.readBoolean();
			ea = dataIn.readBoolean();
			ac = dataIn.readBoolean();
			as = dataIn.readBoolean();
			mf = dataIn.readBoolean();
			mfo = dataIn.readBoolean();

			if(args.length < 1) {
		    	File[] files = new File[dataIn.readInt()];
				for(int f=0; f<files.length; f++) {
					name = "";
					size = dataIn.readInt();
					for(int i=0; i<size; i++)
						name += dataIn.readChar();
					files[f] = new File(name);
				}
				int index = dataIn.readInt();
				if(index < tpane.getTabCount())
					tpane.setSelectedIndex(index);
				open(files);
			}

			srcDir = scDir; // Initializing AFTER any I/O processes
			asmDir = asDir; // Prevents parital or corrupt data in
			incDir = inDir;  // the case that an IOException is thrown
			exeDir = exDir;
			commands.setText(cmmnds);
			hideLegal.setSelected(hl);
			exitSave.setSelected(es);
			exitAsk.setSelected(ea);
			asmComp.setSelected(ac);
			asmSave.setSelected(as);
			makeFun.setSelected(mf);
			madeFunOf = mfo;

		} catch(IOException ioe) {
			firstTime = true;
			madeFunOf = false;
			statusBar.setText("Welcome to "+VERSION+'!');
		}

		if(madeFunOf)
			revealMakeFunOption();

		if(!hideLegal.isSelected())
	        showMessage(
	        	VERSION+".\nCopyright (C) 2007 Dan Cook\n\n" +
	        	"This program is free software; you can redistribute it and/or\n" +
	        	"modify it under the terms of the GNU General Public License\n" +
	        	"as published by the Free Software Foundation; either version 2\n" +
	        	"of the License, or (at your option) any later version.\n\n" +
	        	"This program is distributed in the hope that it will be useful,\n" +
	        	"but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
	        	"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the\n" +
	        	"GNU General Public License for more details.\n\n" +
	        	"You should have received a copy of the GNU General Public License\n" +
	        	"along with this program; if not, write to the Free Software\n" +
	        	"Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.",
	        	JOptionPane.PLAIN_MESSAGE
	        );

        if(firstTime) getDirectories();
		if(srcDir.exists()) chooser.setCurrentDirectory(srcDir);
		else if(tempFile.exists()) chooser.setCurrentDirectory(tempFile);
	}

	public java.awt.Color randomColor(boolean dark) {
		double hue = Math.random()*2*Math.PI;
		int r = (int)(32*Math.pow(1+Math.cos(hue),2));
		int g = (int)(32*Math.pow(1+Math.cos(hue+2*Math.PI/3),2));
		int b = (int)(35*Math.pow(1+Math.cos(hue-2*Math.PI/3),2));

		if(dark) return new java.awt.Color(r,g,b);
		return new java.awt.Color(255-r, 255-g, 255-b);
	}

	public void revealMakeFunOption() {
		optionBox = new JDialog(this, VERSION+" - Options", true);
		optionBox.setDefaultCloseOperation(HIDE_ON_CLOSE);
		optionBox.setLayout(new java.awt.GridLayout(8,1));
		optionBox.setLocation(150, 100);
		optionBox.setResizable(false);

		JButton button = new JButton("Finished");
		button.addActionListener(this);
		button.setActionCommand("o");

		java.awt.Container c = optionBox.getContentPane();
		c.add(exitAsk);  c.add(hideLegal);
		c.add(exitSave); c.add(asmSave);
		c.add(asmComp);  c.add(forgetMe);
		c.add(makeFun);  c.add(button);
		optionBox.pack();
	}

	public void getDirectories() {
		if(firstTime) {
			JOptionPane.showMessageDialog(
				this,
				"Welcome! to"+VERSION+" Since you are new here,\n" +
				"you are going to be asked to adjust some settings. You\n" +
				"can change these options from the Options menu at any time",
				"Welcome to "+VERSION+'!', JOptionPane.PLAIN_MESSAGE, SQUIRREL_ICON
			);

			statusBar.setText(" ");
		}

		srcIn.setText(srcDir.getPath());
		asmIn.setText(asmDir.getPath());
		incIn.setText(incDir.getPath());
		exeIn.setText(exeDir.getPath());
		directories.setVisible(true);
	}

	public void setDirectories() {
		directories.setVisible(false);
		srcDir = new File(srcIn.getText());
		asmDir = new File(asmIn.getText());
		exeDir = new File(exeIn.getText());
		incDir = new File(incIn.getText());

		if(firstTime) {
			firstTime = false;
			optionBox.setVisible(true);
			chooser.setCurrentDirectory(srcDir);
			directories.setDefaultCloseOperation(HIDE_ON_CLOSE);
		}
	}

	public boolean valid(String name) {
		for(int i=0; i<asmNames.length; i++)
			if(name.equals(asmNames[i]))
				return false;
		return true;
	}

	public void close() {
		try {
			int index = tpane.getSelectedIndex();
			String name = tpane.getTitleAt(index);

			if(name.indexOf(" *") > 0)
				if(Confirm("Save changes to "+name+'?')) {
					if(saveAs(index)) return;
				}

			if(index < tabFiles.length) {
				File[] remove = new File[tabFiles.length-1];
				for(int i=0; i<index; i++) remove[i] = tabFiles[i];
				for(int i=index; i<remove.length; i++) remove[i] = tabFiles[i+1];
				tabFiles = remove;
			} tpane.removeTabAt(index);
		} catch(Throwable t) { uhoh(t); }
	}

	private boolean fileType(String name, String ext) {
		int dot = name.lastIndexOf('.');
		if(dot < 0) return false;
		return (dot == name.lastIndexOf(ext));
	}

	public void open(File file) {
		String name = file.getName();
		if(!valid(name))
			showMessage("You cannot open a file called "+name, JOptionPane.WARNING_MESSAGE);
		else {
			if(addTab(name, file)) {
				try {
					int tab = getTab(name);
					JTextArea area = getTextArea(tab);
					area.read(new BufferedReader(new FileReader(file)), null);
					area.getDocument().addDocumentListener(this);
					tpane.setSelectedIndex(tab);
					area.setTabSize(4);
				} catch(FileNotFoundException f) {
					showMessage("File not found: '"+name+"'", JOptionPane.WARNING_MESSAGE);
				} catch(IOException e) {
					showMessage("Error reading from file: '"+name+"'", JOptionPane.WARNING_MESSAGE);
				}
			}
		}
	}

	public void open(File[] files) {
		for(File f: files) open(f);
		//if(tpane.getTabCount() > 0)
		//	tpane.setSelectedIndex(0);
	}

	public void open(String[] files) {
		for(String f: files) open(new File(f));
		if(tpane.getTabCount() > 0)
			tpane.setSelectedIndex(0);
	}

	public void save(int tab, File file) {
		if(file == null) { saveAs(tab); return; }

        try {
            new DataOutputStream(
            	new FileOutputStream(file)
            ).writeBytes(getTextArea(tab).getText());

            if(tab < tabFiles.length) {
            	tabFiles[tab] = file;
            	String name = file.getName();
            	tpane.setTitleAt(tab, file.getName());
            	if(fileType(name, ".ads")) tpane.setIconAt(tab, ADS_ICON);
            	else if(fileType(name, ".txt")) tpane.setIconAt(tab, NEW_ICON);
            	else if(
            		fileType(name, ".z80") || fileType(name, ".asm") || fileType(name, ".inc")
            	) tpane.setIconAt(tab, Z80_ICON);
            	else tpane.setIconAt(tab, HELP_ICON);
            } else if(compileAsm)
            	compileSavedAssembly(file, file.getName());
        } catch(IOException ioe) {
            String retry = "Retry";
            if(
            	JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
            		this, "Error saving to "+tabFiles[tab].getName(), VERSION,
            		JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
            		SQUIRREL_ICON, new String[] {retry, "Cancel"}, retry
            	)
			) try { saveAs(tab); }
        	catch(Throwable throwable) { uhoh(throwable); }
        }
	}

	public boolean saveAs(int tab) { // true == Cancel
        String name = getTab(tab);
		boolean asm = !valid(name);
		boolean useDefault = (asm)? false : (tabFiles[tab] != null);

        if(name.indexOf(".ads") > 0) chooser.setFileFilter(adsFiles);
        else if(name.indexOf(".txt") > 0) chooser.setFileFilter(txtFiles);
        else if(asm || fileType(name,".z80") || fileType(name,".asm") || fileType(name,".inc"))
            chooser.setFileFilter(asmFiles);

		File previousDir = chooser.getCurrentDirectory();
		javax.swing.filechooser.FileFilter previousFilter = chooser.getFileFilter();

		try {
			if(useDefault)
				chooser.setCurrentDirectory(tabFiles[tab]);
			else if(chooser.getFileFilter() == adsFiles)
				chooser.setCurrentDirectory(srcDir);
			else if(chooser.getFileFilter() == asmFiles)
				chooser.setCurrentDirectory(asmDir);
		} catch(Exception e) { }

		if(asm) {
			int index = utils.find(asmNames, name);
			if(index >= 0 && sourceFiles[index] != null) {
				name = sourceFiles[index].getName();
				index = name.lastIndexOf(".ads");
				if(index >= 0) name = name.substring(0, index);
				chooser.setSelectedFile(new File(name+".z80"));
			}
		}

        if(JFileChooser.APPROVE_OPTION != chooser.showSaveDialog(this)) {
        	if(previousDir.exists()) {
        		chooser.setCurrentDirectory(previousDir);
        		chooser.setFileFilter(previousFilter);
        	} return true;
        }

        File file = chooser.getSelectedFile();
        name = file.getPath();

        if(name.indexOf('.') < 0) {
            if(chooser.getFileFilter() == txtFiles) name += ".txt";
            else if(chooser.getFileFilter() == asmFiles) name += ".z80";
            else if(chooser.getFileFilter() == adsFiles) name += ".ads";
            file = new File(name);
        };

        if(file.exists()) {
            String rename = "Rename";
            switch(
            	JOptionPane.showOptionDialog(
            		this,
            		"File "+file.getName()+" already exists", VERSION,
            		JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
            		SQUIRREL_ICON, new String[] {rename, "Overwrite", "Cancel"}, rename
            	)
            ) {
            	case JOptionPane.NO_OPTION: break;
	            case JOptionPane.YES_OPTION: return saveAs(tab);
	            default: return false;
            }
        }

        try { save(tab, file); }
        catch(Throwable throwable) { uhoh(throwable); }

        return false;
	}

	public void saveAll() {
		for(int i=0; i<tabFiles.length; i++) save(i, tabFiles[i]);
	}

	public void exit() {
		if(compiler.isAlive())
			return;
		if(
			exitAsk.isSelected() &&
			JOptionPane.YES_OPTION !=
			JOptionPane.showConfirmDialog(
				this,
				"Do you really want to leave?",
				VERSION,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				SQUIRREL_ICON
			)
		) return;

		String name;

		if(exitSave.isSelected()) saveAll();
		else {
			loop: for(int i=0; i<tabFiles.length; i++) {
				name = tpane.getTitleAt(i);
				if(name.indexOf(" *") == name.length()-2) {
					switch(
						JOptionPane.showConfirmDialog(
							this,
							"Save changes before Exiting?",
							VERSION,
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							SQUIRREL_ICON
						)
					) {
						case JOptionPane.YES_OPTION: saveAll();
						case JOptionPane.NO_OPTION: break loop;
						default: return;
					}
				}
			}
		}

		try {
			int remove = 0;
			File[] copy = new File[tabFiles.length];

			for(int i=0; i<tabFiles.length; i++) {
				if(tabFiles[i] == null) remove++;
				else copy[i-remove] = tabFiles[i];
			}

			File[] savedFiles = new File[copy.length-remove];

			for(int i=0; i<savedFiles.length; i++)
				savedFiles[i] = copy[i];

			DataOutputStream dataOut = new DataOutputStream(
	    		new FileOutputStream("SquirrelBox.dat")
	    	);

			dataOut.writeBoolean(forgetMe.isSelected());

			if(!forgetMe.isSelected()) {
				name = srcDir.getPath();
				dataOut.writeInt(name.length());
				for(int i=0; i<name.length(); i++)
					dataOut.writeChar(name.charAt(i));

				name = asmDir.getPath();
				dataOut.writeInt(name.length());
				for(int i=0; i<name.length(); i++)
					dataOut.writeChar(name.charAt(i));

				name = incDir.getPath();
				dataOut.writeInt(name.length());
				for(int i=0; i<name.length(); i++)
					dataOut.writeChar(name.charAt(i));

				name = exeDir.getPath();
				dataOut.writeInt(name.length());
				for(int i=0; i<name.length(); i++)
					dataOut.writeChar(name.charAt(i));

				name = commands.getText();
				dataOut.writeInt(name.length());
				for(int i=0; i<name.length(); i++)
					dataOut.writeChar(name.charAt(i));

				dataOut.writeBoolean(hideLegal.isSelected());
				dataOut.writeBoolean(exitSave.isSelected());
				dataOut.writeBoolean(exitAsk.isSelected());
				dataOut.writeBoolean(asmComp.isSelected());
				dataOut.writeBoolean(asmSave.isSelected());
				dataOut.writeBoolean(makeFun.isSelected());
				dataOut.writeBoolean(madeFunOf);

		    	dataOut.writeInt(savedFiles.length);

				for(File f: savedFiles) {
					name = f.getPath();
					dataOut.writeInt(name.length());
					for(int i=0; i<name.length(); i++)
						dataOut.writeChar(name.charAt(i));
				}
				if(savedFiles.length < 0) dataOut.writeInt(-1);
				else dataOut.writeInt(tpane.getSelectedIndex());
			}
		} catch(IOException ioe) {
			if(
				JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
					this,
					"An error occurred while saving the comiler state.\n" +
					"Click NO and try again. Only click YES if you keep\n" +
					"seeing this message and cannot exit otherwise. Doing\n" +
					"so will the default directories and your preferences.\n\n" +
					"Do you wish to exit "+VERSION+" anyway?", VERSION,
					JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE
				)
			) return;
		}

		System.exit(0);
	}

	public void show(int tab, boolean clear) {
		try {
			int place = getTab(asmNames[tab]);

			if(tpane.getSelectedIndex() != place)
				asmOutput[tab].setForeground(randomColor(tab != ASM_FEEDBACK));

			if(place < 0) {
				int index = tpane.getTabCount();
				int nextIndex = index;

				for(int i=0; i<asmNames.length; i++) {
					if(getTab(asmNames[i]) >= 0) {
						nextIndex--;
						if(i > tab) // This snippet was added to place asmTabs in the right order
							index = nextIndex;
					}
				}

				tpane.insertTab(
					asmNames[tab],
					(tab == GENERAL_Z80)? GENERAL_ICON : (tab == ASM_FEEDBACK)? FEEDBACK_ICON : CALC_ICON,
					new JScrollPane(
						asmOutput[tab],
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
					), "Compiled "+asmNames[tab]+" Output Code", index
				); tpane.setSelectedIndex(index);
			} else tpane.setSelectedIndex(place);
			if(clear) asmOutput[tab].setText("");
		} catch(Throwable t) { uhoh(t); }
	}

	public JTextArea getTextArea(int index) {
		return (JTextArea)((JScrollPane)tpane.getComponentAt(index)).getViewport().getView();
	}

    public String getTab(int tab) {
        String name = tpane.getTitleAt(tab);
        if(name.indexOf(" *") == name.length()-2)
            name = name.substring(0, name.length()-2);
        return name;
    }

    public int getTab(String name) {
        for(int tab=0; tab < tpane.getTabCount(); tab++)
            if(name.equals(getTab(tab))) return tab;
        return -1;
    }

    public boolean addTab(String title, File file) {
        try {
        	int tab = getTab(title);
	        if(tab >= 0) {
		        if( !Confirm(
			        	"There is already a file called " +
			        	title+".\nDo you want to replace it?"
			        )
				) return false;

		        tpane.setSelectedIndex(tab);
		        JTextArea text = getTextArea(tab);
		        if(file != null)
		            text.read(new BufferedReader(new FileReader(file)), null);
		        text.requestFocus();
		        return true;
	        }

            JTextArea text = new JTextArea();
            text.setFont(new java.awt.Font("Lucida Console", java.awt.Font.PLAIN, 12));
            text.getDocument().addDocumentListener(this);
            //text.setForeground(randomColor(true));
            text.setTabSize(4);
            ImageIcon icon = null;
            String type = null;

            if(fileType(title, ".ads")) {
                type = "Source File";
                icon = ADS_ICON;
            } else if(fileType(title, ".txt")) {
                type = "Text File";
                icon = TXT_ICON;
            } else if(
            	fileType(title, ".z80") || fileType(title, ".asm") || fileType(title, ".inc")
            ) {
            	type = "z80 Source File";
            	icon = Z80_ICON;
            } else {
            	type = "Unknown File-type";
            	icon = HELP_ICON;
            }

            tpane.insertTab(title, icon, new JScrollPane(text, 20, 30), type, tabFiles.length);
            tpane.setSelectedIndex(tabFiles.length);

            File copy[] = new File[tabFiles.length+1];
            for(int j = 0; j < tabFiles.length; j++)
                copy[j] = tabFiles[j];

            copy[tabFiles.length] = file;
            tabFiles = copy;
            text.requestFocus();
        } catch(Throwable throwable) { uhoh(throwable); }
        return true;
    }

	public void asmOut(String code) {
		if(loading) return;
		show(current, false);
		asmOutput[current].setText(asmOutput[current].getText()+code);
	}

	public void asmOutln(String code) { asmOut(code+'\n'); }
	public void asmOutln() { asmOut("\n"); }

	public void showMessage(String message, int type) {
		JOptionPane.showMessageDialog(
			this, message, VERSION, type,
			(type == JOptionPane.PLAIN_MESSAGE)? SQUIRREL_ICON : null
		);
	}

	private String shorten(String string, int max) {
		if(string.length() > max)
			return string.substring(0, max-3)+"...";
		int count = 0;
		int perLine = 0;
		for(int i=0; i<string.length(); i++) {
			if(++perLine > 100)
				if(i < string.length()-1)
					string = string.substring(0,i)+"\n  -->  "+string.substring(i);
			if(string.charAt(i) == '\n' || string.charAt(i) == '\r') {
				if(++count > 20)
					return string.substring(0,i)+"...";
				perLine = 0;
			}
		} return string;
	}

	public void ErrorMessage(String message, String code) throws CompileError {
		if(code != null) message += "\nFOUND: "+code;

		JOptionPane.showMessageDialog(
			this, "ERROR: "+message +
			"\n\nSOME CODE AFTER THE ERROR:\n"+shorten(in.stack.toString(), 500),
			VERSION+" (Error Found)", JOptionPane.WARNING_MESSAGE
		);

		CompileError ce = new CompileError();
		ce.printStackTrace();
		throw ce;
	}

	public void ErrorMessage(String message) throws CompileError {
		ErrorMessage(message, null);
	}

	public void WarningMessage(String message) throws CompileError {
		String ok = "Coninue anyway";
		if(
			JOptionPane.OK_OPTION !=
			JOptionPane.showOptionDialog(
				this, "Warning:\n"+message, VERSION, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE, null, new String[]{ok, "Fix the problem"}, ok
			)
		) throw new CompileError();
	}

	public boolean Confirm(String message) {
		return JOptionPane.YES_OPTION ==
			JOptionPane.showConfirmDialog(
				this, message, VERSION,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				SQUIRREL_ICON
			);
	}

	private void uhoh(Throwable thrown) {
		StringWriter stacktrace = new StringWriter();
		thrown.printStackTrace(new PrintWriter(stacktrace));
		String report = stacktrace.toString();
		statusBar.setText("AAHHHHHHHH!!!!!!!!  ABORT! ABORT! ABORT!");
		showMessage(
			"An unexpected Error occurred. SquirrelBox will be terminated\n" +
			"and you will lose all unsaved data. Please record the following\n" +
			"and report it to me so I can try to fix the problem (press [ALT]+\n" +
			"[Print Screen] to copy an image of this window)\n\nHave a good day! :-)\n\n" +
			shorten(report, 500),
			JOptionPane.ERROR_MESSAGE
		); System.exit(1);
	}

	private void replace(StringBuilder cmd, String what, String with) {
		do {
			int index = cmd.indexOf(what);
			if(index < 0) break;
			cmd.replace(index, index+what.length(), with);
		} while(true);
	}

	public void compileSavedAssembly(File asmFile, String title) {
		compileAsm = false;
		StringBuilder cmd = new StringBuilder(commands.getText());

		String name = asmFile.getAbsolutePath();
		replace(cmd, "<asmFile>", name);

		int index = name.lastIndexOf('.');
		if(index > 0) name = name.substring(0,index);
		replace(cmd, "<asmFileName>", name);

		name = asmFile.getName();
		index = name.lastIndexOf('.');
		if(index > 0) name = name.substring(0,index);
		replace(cmd, "<asmName>", name);

		replace(cmd, "<asmParent>", asmFile.getParentFile().getAbsolutePath());
		replace(cmd, "<asmDir>", asmDir.getAbsolutePath());
		replace(cmd, "<exeDir>", exeDir.getAbsolutePath());

		statusBar.setText("Assembling "+title+"...");

		try {
            Runtime rt = Runtime.getRuntime();
            final Process proc = rt.exec(cmd.toString());

			show(ASM_FEEDBACK, true);

            new Thread() {
				public void run() {
					try {
						BufferedReader br = new BufferedReader(
							new InputStreamReader(proc.getErrorStream())
						); String line = null;
						while((line = br.readLine()) != null)
							asmOutput[ASM_FEEDBACK].setText(asmOutput[ASM_FEEDBACK].getText()+line+'\n');
					} catch (IOException ioe) {
						StringWriter stacktrace = new StringWriter();
						ioe.printStackTrace(new PrintWriter(stacktrace));
						asmOutput[ASM_FEEDBACK].setText(
							asmOutput[ASM_FEEDBACK].getText()+
							"\n\n>>>>AN I/O EXCEPTION WAS THROWN<<<<\n\n"+
							shorten(stacktrace.toString(),1000)+"\n\n"
						);
					}
				}
            }.start();

            new Thread() {
				public void run() {
					try {
						BufferedReader br = new BufferedReader(
							new InputStreamReader(proc.getInputStream())
						); String line = null;
						while((line = br.readLine()) != null)
							asmOutput[ASM_FEEDBACK].setText(asmOutput[ASM_FEEDBACK].getText()+line+'\n');
					} catch (IOException ioe) {
						StringWriter stacktrace = new StringWriter();
						ioe.printStackTrace(new PrintWriter(stacktrace));
						asmOutput[ASM_FEEDBACK].setText(
							asmOutput[ASM_FEEDBACK].getText()+
							"\n\n>>>>AN I/O EXCEPTION WAS THROWN<<<<\n\n"+
							shorten(stacktrace.toString(),2000)+"\n\n"
						);
					}
				}
            }.start();

            if(proc.waitFor() == 0) {
            	statusBar.setText(title+" was assembled successfully.");
            	try { Thread.sleep(1500); }
            	catch(Throwable t) { }
            	return;
            }

			statusBar.setText(title+" failed to assemble properly.");
	        showMessage(title+" failed to assemble properly.", JOptionPane.ERROR_MESSAGE);
        } catch(Throwable t) {
        	uhoh(t);
        }
	}

	public void compileAssembly(int tab) {
		compileAsm = true; saveAs(tab);
	}

	public void doCompile() {
		int index = tpane.getSelectedIndex();

		if(tpane.getTabCount() < 1) {
			statusBar.setText("Brilliant!");
			showMessage("There is nothing to compile!", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if(index >= tabFiles.length) {
			compileAssembly(index);
			return;
		}

		String name = getTab(index);

		if(fileType(name, ".z80") || fileType(name, ".asm")) {
			compileSavedAssembly(tabFiles[index], name); return;
		}

		if(!fileType(name, ".ads")) {
			showMessage(
				"SquirrelBox only compiles Antidisassemblage source\n" +
				"files (*.ads) and z80 assembly source files (*.z80 or\n" +
				"*.asm, but not *.inc because it is not actually source).\n" +
				"Note: for z80 assembly files to compile properly, a z80\n" +
				"assembly compiler is required. (I recommend TASM, which\n" +
				"can be found at http://ticalc.org/).", JOptionPane.WARNING_MESSAGE
			); return;
		}

		stopCompiling = false;
		compiler = new Thread(this);
		tpane.setSelectedIndex(index);
		compiler.start();
	}

	public void run() {
		try {
			int index = tpane.getSelectedIndex();
			saveAll();
			func = null;
			File source = tabFiles[index];

			if(source == null) {
				showMessage(
					"There must be a saved copy of a\n" +
					"source file before you can compile it.",
					JOptionPane.WARNING_MESSAGE
				); return;
			}

			for(int i=0; i<asmUsed.length; i++)
				asmUsed[i] = false;
			try {
				try {
					in = new Preprocessor(
						source, getTextArea(index).getText(), this
					); ((Preprocessor)in).preprocess(null);
				} catch(CompileError ce) {
					statusBar.setText("Compilation stopped by preprocessor.");
					return;
				} compile(source);
			} catch(StopCompiling sc) {
				statusBar.setText("Compilation process aborted.");
				stopCompiling = false;
			} tpane.setSelectedIndex(index);
		} catch(Throwable t){uhoh(t);}
	}

	public void insertUpdate(javax.swing.event.DocumentEvent d) {
		int index = tpane.getSelectedIndex();
		tpane.setTitleAt(index, getTab(index)+" *");
	}

	public void removeUpdate(javax.swing.event.DocumentEvent d) {
		insertUpdate(d);
	}

	public abstract void compile(File source) throws StopCompiling;

	public void changedUpdate(javax.swing.event.DocumentEvent d) { }

	public void windowClosing(WindowEvent windowevent) { exit(); }
	public void windowClosed(WindowEvent windowevent) { exit(); }
	public void windowDeactivated(WindowEvent windowevent) { }
	public void windowDeiconified(WindowEvent windowevent) { }
	public void windowActivated(WindowEvent windowevent) { }
	public void windowIconified(WindowEvent windowevent) { }
	public void windowOpened(WindowEvent windowevent) { }

	public void actionPerformed(ActionEvent a) {
		String command = a.getActionCommand();
		char ch = command.charAt(0);
		if(compiler.isAlive()) {
			if(ch == 'Z') stopCompiling = true;
			return;
		}

		switch(ch) {
			case 'X': exit(); return;
			case 'L': saveAll(); return;
			case 'M': doCompile(); return;
			case 'D': getDirectories(); return;
			case '2': show(TI82, false); return;
			case '3': show(TI83, false); return;
			case '5': show(TI85, false); return;
			case '6': show(TI86, false); return;
			case '4': show(TI84_PLUS, false); return;
			case 'P': show(TI83_PLUS, false); return;
			case 'G': show(GENERAL_Z80, false); return;
			case 'F': show(ASM_FEEDBACK, false); return;
			case 'T': show(TI83_PLUS_SE, false); return;
			case 'I': show(TI84_PLUS_SE, false); return;
			case 'm': optionBox.setVisible(true); return;
			case 'o': optionBox.setVisible(false); return;
			case '^': commandBox.setVisible(true); return;
			case '.': commandBox.setVisible(false); return;

			case 'C':
				if(tpane.getTabCount() > 0)
					close();
				return;

			case 'd':
				char ch2 = a.getActionCommand().charAt(1);

				if(ch2 == 'O') setDirectories();
				else if(ch2 == 'C') {
					if(firstTime) setDirectories();
					else directories.setVisible(false);
				} else {
					chooser.setMultiSelectionEnabled(false);
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

					if(JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
						File newFile = chooser.getSelectedFile();

						switch(ch2) {
							case 'S': srcDir = newFile; srcIn.setText(newFile.getPath()); break;
							case 'I': incDir = newFile; incIn.setText(newFile.getPath()); break;
							case 'A': asmDir = newFile; asmIn.setText(newFile.getPath()); break;
							case 'E': exeDir = newFile; exeIn.setText(newFile.getPath()); break;
						}
					}

					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setMultiSelectionEnabled(true);
				}
				return;

			case 'E':
				if(Confirm("Close all tabs?"))
					while(tpane.getTabCount() > 0) close();
				return;

			case '+':
				showMessage(
					(makeFun.isSelected())?
						"FINE, you can have your stupid '+' sign!\n" +
						"Just stop crying about it already!!\n\n...sheesh..."
						: "In that case, maybe do deserve it :-P",
					JOptionPane.WARNING_MESSAGE
				); return;

			case '<': // Deselecting the one deselects the other
				if(!asmSave.isSelected())
					asmComp.setSelected(false);
				return;

			case '>': // Selecting the one selects the other
				if(asmComp.isSelected())
					asmSave.setSelected(true);
				return;

			case 'A':
				if(tpane.getTabCount() > 0)
					saveAs(tpane.getSelectedIndex());
				return;

			case 'S':
				if(tpane.getTabCount() > 0) {
					int index = tpane.getSelectedIndex();
					save(index, (index < tabFiles.length)? tabFiles[index] : null);
				} return;

			case 'O':
				chooser.setMultiSelectionEnabled(true);
				if(chooser.APPROVE_OPTION == chooser.showOpenDialog(this))
					open(chooser.getSelectedFiles());
				chooser.setMultiSelectionEnabled(false);
				return;

			case '_':
				switch(command.charAt(1)) {
					case 'S':
						chooser.setCurrentDirectory(srcDir);
						chooser.setFileFilter(adsFiles); return;
					case 'I':
						chooser.setCurrentDirectory(incDir);
						chooser.setFileFilter(adsFiles); return;
					case 'A':
						chooser.setCurrentDirectory(asmDir);
						chooser.setFileFilter(asmFiles); return;
					case 'E':
						chooser.setCurrentDirectory(exeDir);
						chooser.setFileFilter(null);
					default: return;
				}

			case 'N':
			  	String title = JOptionPane.showInputDialog(this, "File name:");
		        if(title == null) return;
		        if(title.length() < 1) return;
		        while(Character.isWhitespace(title.charAt(0))) {
		        	if(title.length() < 2) return;
		        	title = title.substring(1);
		        }
		        if(title.indexOf('.') < 0) {
		            String ads = "ADSMBLG Source";
		            switch(
		            	JOptionPane.showOptionDialog(
		                	this, "Select a file-type:", VERSION,
		                	JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
		                	SQUIRREL_ICON, new String[] { ads, "z80 Source", "Plain Text" }, ads
		                )
		           	) {
		                case JOptionPane.YES_OPTION: title += ".ads"; break;
		                case JOptionPane.NO_OPTION: title += ".z80"; break;
		                case JOptionPane.CANCEL_OPTION: title += ".txt"; break;
		                default: return;
		           	}
		        } addTab(title+" *", null);
		        getTextArea(tpane.getSelectedIndex()).setText("");
		        return;

			case 'B': showMessage(
VERSION+".\nCopyright (C) 2007 Dan Cook\n\n"+

"SquirrelBox is the sole compiler for the \"Antidisassemblage\" programming\n" +
"language, designed under the project code name of \"High Level z80\". HLz80\n" +
"is now the name of a Google group that is the home of this & other projects.\n\n" +

"Antidisassemblage (ADSMBLG) is a structured programming language for z80\n" +
"TI Graphing Calculators (TI-82, TI-83[+][SE], TI-84+[SE], TI-85, TI-86, and\n" +
"theoretically any other z80 system). The language closely resembles C++ &\n" +
"Java, along with an optional alternate syntax that better resembles TI-BASIC.\n\n" +

"Just like C/C++ and Assembly, Antidisassemblage can be customized for any\n" +
"environment through the use of preprocessor commands, \"include\" files/libs,\n" +
"customizable (assembly) macros, and low-level data manipulation (\"pointers\").\n\n" +

"For more information, resources, or to contribute to further development,\n" +
"please visit the HLz80 group at:  http://groups.google.com/group/HLz80/\n\n" +

"Special thanks to:\n" +
" - Jack Crenshaw, for the \"Let's make a compiler\" tutorials (a MAJOR help!)\"\n" +
" - Everyone who contributed their ideas and feedback, who helped test and\n" +
"   weed out bugs, and who helped me to solve many problems along the way.",
				JOptionPane.INFORMATION_MESSAGE
			); default: return;
		}
	}
}