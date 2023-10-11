/* * This program is free software; you can redistribute it and/or * modify it under the terms of the GNU General Public License * as published by the Free Software Foundation; either version 2 * of the License, or (at your option) any later version. * * This program is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the * GNU General Public License for more details. * You should have received a copy of the GNU General Public License * along with this program; if not, write to the Free Software * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. */
package net.sf.jftp.util;

import net.sf.jftp.*;
import net.sf.jftp.config.*;
import net.sf.jftp.gui.framework.*;import net.sf.jftp.gui.tasks.FaultDisplay;
import net.sf.jftp.net.*;
import net.sf.jftp.util.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.util.*;


public class DataConnectionWriter
{		private String filename;

    public DataConnectionWriter(int port, String host)
    {    	this.filename = port + "-" + host + "-errors.txt";
    }        public void writeToFile(String msg, String exceptionMsg)     {    	try     	{    		File newFile = new File(this.filename);    		newFile.createNewFile(); // does not do anything if file exists    		FaultDisplay faultDisplay = new FaultDisplay(msg, newFile.getAbsolutePath());        	JFtp.desktop.add(faultDisplay, Integer.MAX_VALUE - 10);    		FileWriter fileWriter = new FileWriter(this.filename, true); // boolean for append    		fileWriter.write(msg + "\n" + exceptionMsg + "\n\n");    		fileWriter.close();    	} catch (Exception e) {    		e.printStackTrace();    	}    }    
}
