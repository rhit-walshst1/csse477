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
import java.io.IOException;

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
import net.sf.jftp.net.RestConnection;
import net.sf.jftp.net.FtpURLConnection;
import net.sf.jftp.net.wrappers.StartConnection;
import net.sf.jftp.system.StringUtils;
import net.sf.jftp.system.logging.Log;


public class RestHostChooser extends HFrame implements ActionListener,
                                                   WindowListener
{
    public HTextField host = new HTextField("Hostname:", "somehost");
    public HTextField user = new HTextField("Username:", "user");

    public HPasswordField pass = new HPasswordField("Password:",
                                                    "password");
    public HTextField port = new HTextField("Port:    ", "21");
    public HTextField type = new HTextField("Type:    ", "FTP");

    private HPanel okP = new HPanel();
    private HButton ok = new HButton("Connect");

    public RestHostChooser()
    {
        init();
    }

    public void init()
    {
        setTitle("REST Connection...");
        setBackground(okP.getBackground());

        user.setEnabled(true);
        pass.text.setEnabled(true);

//        try {
//        	String[] login = LoadSet.loadSet(Settings.login_def);
//
//        	if((login != null) && (login[0] != null))
//        	{
//        		host.setText(login[0]);
//        		user.setText(login[1]);
//
//        		if(login[3] != null)
//        		{
//        			port.setText(login[3]);
//        		}
//        	}
//
//        	if(Settings.getStorePasswords())
//        	{
//        		if(login != null && login[2] != null)
//        		{
//        			pass.setText(login[2]);
//        		}
//        	}
//        	else
//        	{
//        		pass.setText("");
//        	}
//        }
//        catch(Exception ex) {
//        	Log.debug("Error initializing connection values!");
//        	//ex.printStackTrace();
//        }

        HInsetPanel root = new HInsetPanel();
        root.setLayout(new MigLayout());
        
        root.add(host);
        root.add(port, "wrap");
        root.add(user);
        root.add(pass, "wrap");
        root.add(type);
        
        root.add(new JLabel(" "),"wrap");
        
        root.add(okP, "align right");
        okP.add(ok);
        ok.addActionListener(this);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        pass.text.addActionListener(this);

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
    	host.requestFocus();
    }

    public void actionPerformed(ActionEvent e)
    {
        if((e.getSource() == ok) || (e.getSource() == pass.text))
        {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));

            JFtp.setHost(host.getText());

            String htmp = StringUtils.cut(host.getText(), " ");
            String utmp = StringUtils.cut(user.getText(), " ");
            String ptmp = StringUtils.cut(pass.getText(), " ");
            String potmp = StringUtils.cut(port.getText(), " ");
            String tpe = StringUtils.cut(type.getText(), " ");

            /* All the information of the current server are stored in JFtp.HostInfo */
            JFtp.hostinfo.hostname = htmp;
            JFtp.hostinfo.username = utmp;
            JFtp.hostinfo.password = ptmp;
            JFtp.hostinfo.port = potmp;
            JFtp.hostinfo.type = tpe.toLowerCase();

            StartConnection.startRestCon(htmp, utmp, ptmp, Integer.parseInt(potmp), tpe);

            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            this.dispose();

            JFtp.mainFrame.setVisible(true);
            JFtp.mainFrame.toFront();
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
