/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.jftp.gui.hostchooser;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import net.sf.jftp.JFtp;
import net.sf.jftp.config.LoadSet;
import net.sf.jftp.config.SaveSet;
import net.sf.jftp.config.Settings;
import net.sf.jftp.gui.base.FtpHost;
import net.sf.jftp.gui.framework.HButton;
import net.sf.jftp.gui.framework.HFrame;
import net.sf.jftp.gui.framework.HInsetPanel;
import net.sf.jftp.gui.framework.HPanel;
import net.sf.jftp.gui.framework.HPasswordField;
import net.sf.jftp.gui.framework.HTextField;
import net.sf.jftp.gui.tasks.HostList;
import net.sf.jftp.net.FilesystemConnection;
import net.sf.jftp.net.FtpConnection;
import net.sf.jftp.net.FtpURLConnection;
import net.sf.jftp.net.wrappers.StartConnection;
import net.sf.jftp.system.StringUtils;
import net.sf.jftp.system.logging.Log;


public class ScpHostChooser extends HFrame implements ActionListener,
                                                   WindowListener
{
    public HTextField sourceHost = new HTextField("Source host:", "localhost");
    public HTextField sourceUser = new HTextField("Source user:", "");
    public HTextField sourcePath = new HTextField("Source path:", "");
    
    public HTextField destinationHost = new HTextField("Destination host:", "");
    public HTextField destinationUser = new HTextField("Destination user:", "");
    public HTextField destinationPath = new HTextField("Destination path:", "");
    
    public HTextField port = new HTextField("Port:", "22");
            
    private JCheckBox rBox = new JCheckBox("Recursively copy directories", true);
    private JCheckBox qBox = new JCheckBox("Hide progress meter and any other info other than errors", false);
    private JCheckBox cBox = new JCheckBox("Compress data when sending", false);
    private JCheckBox pBox = new JCheckBox("Preserve file access times", false);
    
    private HPanel okP = new HPanel();
    private HButton ok = new HButton("Copy");

    private HFrame h = new HFrame();

    private ComponentListener listener = null;
    
    public ScpHostChooser(ComponentListener l)
    {
        listener = l;
        init();
    }

    public ScpHostChooser()
    {
        init();
    }

    public void init()
    {
        setTitle("SCP...");
        setBackground(okP.getBackground());

        try {
        	LoadSet l = new LoadSet();
        	String[] login = l.loadSet(Settings.login_def);

        	if((login != null) && (login[0] != null))
        	{
        		destinationHost.setText(login[0]);
        		destinationUser.setText(login[1]);

        		if(login[4] != null)
        		{
        			destinationPath.setText(login[4]);
        		}

        		if(login[5] != null)
        		{
        			sourcePath.setText(login[5]);
        		}
        	}
        }
        catch(Exception ex) {
        	Log.debug("Error initializing connection values!");
        }
        
        HInsetPanel root = new HInsetPanel();
        root.setLayout(new MigLayout());
        
        root.add(sourceUser);
        root.add(sourceHost, "wrap");
        root.add(sourcePath);
        
        root.add(new JLabel(" "),"wrap");       
        
        root.add(destinationUser);
        root.add(destinationHost, "wrap");
        root.add(destinationPath);
        
        root.add(new JLabel(" "), "wrap");

        root.add(port);
        
        root.add(new JLabel(" "), "wrap");
        
        root.add(rBox);  
        root.add(qBox, "wrap");
        root.add(cBox);
        root.add(pBox, "wrap");
        
        root.add(new JLabel(" "), "wrap");
        
        root.add(okP, "align right");
        okP.add(ok);
        ok.addActionListener(this);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout(10,10));
        getContentPane().add("Center", root);
        
        pack();
        setModal(false);
        setVisible(false);

        addWindowListener(this);
    }

    public void update()
    {
    	fixLocation();
    	setVisible(true);
    	toFront();
    	sourceHost.requestFocus();
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == ok)
        {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            
            StringBuilder sb = new StringBuilder();
            
            sb.append("cmd /c start cmd.exe /C\"scp ");
            if (rBox.isSelected()) {
            	sb.append("-r ");
            }
            
            if (qBox.isSelected()) {
            	sb.append("-q ");
            }
            
            if (cBox.isSelected()) {
            	sb.append("-C ");
            }
            
            if (pBox.isSelected()) {
            	sb.append("-p ");
            }
            
            if (!port.getText().isEmpty()) {
                sb.append("-P ");
                sb.append(port.getText());
                sb.append(" ");
            }
            
            if (!sourceUser.getText().isEmpty()) {
                sb.append(sourceUser.getText());
                sb.append("@");
            }
            
            if (!sourceHost.getText().isEmpty()) {
                sb.append(sourceHost.getText());
                sb.append(":");
            }
            sb.append(sourcePath.getText());
            sb.append(" ");
            
            if (!destinationUser.getText().isEmpty()) {
                sb.append(destinationUser.getText());
                sb.append("@");
            }
            
            if (!destinationHost.getText().isEmpty()) {
                sb.append(destinationHost.getText());
                sb.append(":");
            }
            
            sb.append(destinationPath.getText());
            
            sb.append("\"");
            
            System.out.println(sb.toString());
                        
            try {
            	Process cpyFileLocal = Runtime.getRuntime().exec(sb.toString());
            	            	
            	InputStream stderr = cpyFileLocal.getInputStream();
            	InputStreamReader isr = new InputStreamReader(stderr);
            	BufferedReader br = new BufferedReader(isr);
            	String line = null;
            	System.out.println("<ERROR>");
            	
            	while ((line = br.readLine()) != null) {
            		System.out.println(line);
            	}
            	
            	System.out.println("</ERROR>");
            	int exitVal = cpyFileLocal.waitFor();
            	System.out.println("Process exitValue: " + exitVal);
            	System.out.println("...." + cpyFileLocal.exitValue());
            	System.out.println("Sending  complete...");
            } catch (Exception ex) {
            	ex.printStackTrace();
            }
            
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            this.dispose();

            JFtp.mainFrame.setVisible(true);
            JFtp.mainFrame.toFront();

            if(listener != null)
            {
                listener.componentResized(new ComponentEvent(this, 0));
            }

        }
    }

    public void windowClosing(WindowEvent e)
    {
        //System.exit(0);
        this.dispose();
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
    
    public void pause(int time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch(Exception ex)
        {
        }
    }

}
