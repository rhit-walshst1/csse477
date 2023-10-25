package net.sf.jftp.gui.tasks;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import net.miginfocom.swing.MigLayout;
import net.sf.jftp.JFtp;
import net.sf.jftp.gui.framework.HButton;
import net.sf.jftp.gui.framework.HFrame;
import net.sf.jftp.gui.framework.HInsetPanel;
import net.sf.jftp.gui.framework.HPanel;
import net.sf.jftp.gui.framework.HPasswordField;
import net.sf.jftp.gui.framework.HTextField;

public class UserLogin extends HFrame implements ActionListener, WindowListener {
	public HTextField user = new HTextField("Username:", "");
	public HPasswordField pass = new HPasswordField("Password:", "");
	private HPanel loginP = new HPanel();
	private HButton loginB = new HButton("Log In");
	private ComponentListener listener = null;
	private boolean loginSuccessful = false;
	public int userLevel = -1;
	private String usersFile = new File("").getAbsolutePath() + "\\src\\java\\net\\sf\\jftp\\config\\userLogins.txt";
	public static int ADMIN = 1;
	public static int BASIC = 0;
	public static int INVALID = -1;

	public UserLogin(ComponentListener l) {
		listener = l;
		init();
	}

	public UserLogin() {
		init();
	}

	public void init() {
		setTitle("Login");
		setBackground(loginP.getBackground());

		user.setEnabled(true);
		pass.text.setEnabled(true);

		HInsetPanel root = new HInsetPanel();
		root.setLayout(new MigLayout());

		root.add(user);
		root.add(pass, "wrap");

		root.add(new JLabel(" "), "wrap");

		root.add(loginP, "align right");
		loginP.add(loginB);
		loginB.addActionListener(this);

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		pass.text.addActionListener(this);

		getContentPane().setLayout(new BorderLayout(10, 10));
		getContentPane().add("Center", root);

		pack();
		setModal(false);
		setVisible(false);

		addWindowListener(this);
	}

	public void update() {
		fixLocation();
		setVisible(true);
		toFront();
		user.requestFocus();
	}

	public void actionPerformed(ActionEvent e) {
		if ((e.getSource() == loginB) || (e.getSource() == pass.text)) {
			setCursor(new Cursor(Cursor.WAIT_CURSOR));

			String username = user.getText();
			String userPassword = pass.getText();
			MessageDigest digest;
			try {
				digest = MessageDigest.getInstance("SHA-256");
				byte[] hashedPassword = digest.digest(
						  userPassword.getBytes(StandardCharsets.UTF_8));
				 
				int accessResults = getUserAccess(username, hashedPassword);

				if (accessResults == BASIC) {
					loginSuccessful = true;
					JFtp.setUserLevel(BASIC);
				} else if (accessResults == ADMIN) {
					loginSuccessful = true;
					JFtp.setUserLevel(ADMIN);
				} else {
					JOptionPane.showMessageDialog(JFtp.mainFrame, "Invalid username or password!");
					return;
				}
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
			}

			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			this.dispose();

			JFtp.mainFrame.setVisible(true);
			JFtp.mainFrame.toFront();

			if (listener != null) {
				listener.componentResized(new ComponentEvent(this, 0));
			}

		}
	}
	
	/* s must be an even-length string. */
	// From: https://stackoverflow.com/a/140861
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	private int getUserAccess(String username, byte[] password) {
		try {
			String filePath = usersFile;
			FileReader fileReader = new FileReader(filePath);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String[] parts = line.split(",");
				
				if (parts.length != 3) {
					bufferedReader.close();
					throw new RuntimeException("Invalid userLogins.txt file");
				}
				
				String fileUsername = parts[0];
				String filePassword = parts[1];
				byte[] fileHashPassword = hexStringToByteArray(filePassword);
				String filePrivileges = parts[2];

				if (username.equals(fileUsername)) {
					if (Arrays.equals(password, fileHashPassword)) {
						bufferedReader.close();

						if (filePrivileges.equals("admin")) {
							return ADMIN;
						} else if (filePrivileges.equals("basic")) {
							return BASIC;
						}
						
						throw new RuntimeException("Invalid userLogins.txt file");
					} else {
						bufferedReader.close();
						return INVALID;
					}
				}
			}
			
			bufferedReader.close();
			return INVALID;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return INVALID;
	}

	public void windowClosing(WindowEvent e) {
		this.dispose();
		if (!loginSuccessful) {
			System.exit(0);
		}
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void pause(int time) {
		try {
			Thread.sleep(time);
		} catch (Exception ex) {
		}
	}

}
