package net.sf.jftp.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import net.sf.jftp.system.StringUtils;
import net.sf.jftp.system.logging.Log;

public class RestConnection implements BasicConnection {
	static String serverUrl = "http://localhost:8080";

	private Vector<ConnectionListener> listeners = new Vector<ConnectionListener>();
	private String localPath = "/";
	private String pwd = "/";
	private String filterText = "";
	
	class RemoteFile {
		String name;
		String date;
		String content;
		int perms;
		public RemoteFile(String name, String content, String date, int perms) {
			this.name = name;
			this.content = content;
			this.date = date;
			this.perms = perms;
		}
	}

	List<RemoteFile> files;
	String host;
	String user;
	String pass;
	int port;
	String type;

	public RestConnection(String host, String user, String pass, int port, String type) {
		this.host = host;
		this.user = user;
		this.pass = pass;
		this.port = port;
		this.type = type;

		this.files = new ArrayList<RemoteFile>();
	}

	private int sendWebRequest(String path) {
		return sendWebRequest(path, "GET", "");
	}
	
	private int sendWebRequest(String path, StringBuffer out) {
		return sendWebRequest(path, "GET", "", out);
	}

	private int sendWebRequest(String path, String method, String data) {
		return sendWebRequest(path, method, data, null);
	}
	
	private int sendWebRequest(String path, String method, String data, StringBuffer out) {
		HttpURLConnection connection = null;
		
		boolean writeData = !method.equals("GET") && !data.isEmpty();
		
		try {
			// Create connection
			URL url = new URL(serverUrl + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method);
			connection.setRequestProperty("content-type", "application/json");
			connection.setRequestProperty("username", user);
			connection.setRequestProperty("password", pass);
			connection.setRequestProperty("port", Integer.toString(port));
			connection.setRequestProperty("ftp-host", host);
			connection.setRequestProperty("ftp-type", type);
			
			if (writeData) {
				byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

				connection.setDoOutput(true);
				connection.setFixedLengthStreamingMode(dataBytes.length);
				
				connection.connect();
				
				try (OutputStream os = connection.getOutputStream()) {
					os.write(dataBytes);
				}
			}

			// Get Response
			if (out != null) {
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					out.append(inputLine);
				}
				in.close();
			}
			return connection.getResponseCode();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public void connect() {
		int response = sendWebRequest("/checkconnection");

		if (response != 200) {
			Log.debug("Failed to connect! Got response code: " + response);
			return;
		}
		
		for (ConnectionListener listener : listeners) {
			listener.connectionInitialized(this);
			listener.updateRemoteDirectory(this);
		}
	}

	@Override
	public void sendRawCommand(String cmd) {}

	@Override
	public void disconnect() {}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public String getPWD() {
		return pwd;
	}

	@Override
	public boolean cdup() {
		return chdir(pwd.substring(0, pwd.lastIndexOf("/") + 1));
	}

	@Override
	public boolean mkdir(String dirName) {
		Log.debug("Making empty directories is not supported");
		return false;
	}

	@Override
	public void list() throws IOException {
		StringBuffer out = new StringBuffer();
		int response = sendWebRequest("/download?path=" + getPWD(), out);
		
		if (response != 200) {
			throw new IOException("Failed to download all files");
		}
				
		JSONObject obj = new JSONObject(out.toString());
				
		JSONArray files = obj.getJSONArray("Files");
		
		this.files.clear();
				
		for (int i = 0; i < files.length(); i++) {
			JSONObject file = files.getJSONObject(i);
			
			if (!filterText.isEmpty() && !file.getString("file").contains(filterText)) {
				continue;
			}
			
			this.files.add(new RemoteFile(file.getString("file"), file.getString("content"), file.getString("date"), FtpConstants.W));
		}
		
		this.files.sort(new Comparator<RemoteFile>() {
			@Override
			public int compare(RemoteFile o1, RemoteFile o2) {
				return o1.name.compareTo(o2.name);
			}	
		});
	}

	@Override
	public boolean chdir(String p) {
		chdirNoRefresh(p);
        
        for (ConnectionListener listener : this.listeners) {
        	listener.updateRemoteDirectory(this);
        }

		return true;
	}

	@Override
	public boolean chdirNoRefresh(String p) {
		p = p.replace('\\', '/');

        if(StringUtils.isRelative(p))
        {
            p = pwd + p;
        }

        p = p.replace('\\', '/');
        
        pwd = p;

		return true;
	}

	@Override
	public String getLocalPath() {
		return localPath;
	}

	@Override
	public boolean setLocalPath(String newPath) {
		localPath = newPath;
		return true;
	}

	@Override
	public String[] sortLs() {		
		String[] sortedList = new String[this.files.size()];
		
		for (int i = 0; i < this.files.size(); i++) {
			sortedList[i] = this.files.get(i).name;
		}
		
		return sortedList;
	}

	@Override
	public String[] sortSize() {
		String[] sortedList = new String[this.files.size()];
		
		for (int i = 0; i < this.files.size(); i++) {
			sortedList[i] = Integer.toString(this.files.get(i).content.length());
		}
		
		return sortedList;
	}

	@Override
	public Date[] sortDates() {
		Date[] sortedList = new Date[this.files.size()];
		
		for (int i = 0; i < this.files.size(); i++) {
			sortedList[i] = new Date(this.files.get(i).date);
		}

		return sortedList;
	}

	@Override
	public int[] getPermissions() {
		int[] sortedList = new int[this.files.size()];
		
		for (int i = 0; i < this.files.size(); i++) {
			sortedList[i] = this.files.get(i).perms;
		}

		return sortedList;
	}

	@Override
	public int handleDownload(String file) {
		return download(file);
	}

	@Override
	public int handleUpload(String file) {
		return upload(file);
	}

	@Override
	public int download(String file) {
		StringBuffer out = new StringBuffer();

		int response = sendWebRequest("/download?path=" + file, out);
		
		if (response != 200) {
			Log.debug("Failed to download a file");
			return -1;
		}
		
		JSONObject obj = new JSONObject(out.toString());
		
		JSONArray files = obj.getJSONArray("Files");
		
		if (files.length() != 1) {
			Log.debug("Irregular response from downloading");
			return -1;
		}
		
		JSONObject fileResponse = files.getJSONObject(0);
		
		String[] fileSplit = fileResponse.getString("file").split("/");
		String fileName = fileSplit[fileSplit.length - 1];
		String fileContent = fileResponse.getString("content");
		
		File newFile = new File(localPath + fileName);
		
		try {
			newFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			FileWriter fileWriter = new FileWriter(localPath + fileName, false);
			fileWriter.write(fileContent);
			fileWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return 0;
	}

	@Override
	public int upload(String file) {
		return upload(file, null);
	}

	@Override
	public int upload(String file, InputStream in) {
		StringBuilder stringBuilder = new StringBuilder();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(localPath + file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line.replaceAll("[^a-zA-Z0-9]", " "));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String content = stringBuilder.toString();
										
		int response = sendWebRequest("/upload", "POST", "[{\"path\": \"" + pwd.substring(0, pwd.length() - 1) + "\", \"fileName\": \"" + file + "\", \"body\": \"" + content + "\"}]");
		
		if (response == 200) {
			for (ConnectionListener listener : this.listeners) {
				listener.updateProgress(file, DataConnection.FINISHED, -1);
			}
		} else {
			for (ConnectionListener listener : this.listeners) {
				listener.updateProgress(file, DataConnection.FAILED, -1);
			}
		}

		return 0;
	}

	@Override
	public InputStream getDownloadInputStream(String file) {
		Log.debug("get download input stream");

		return null;
	}

	@Override
	public int removeFileOrDir(String file) {
		int response = sendWebRequest("/delete", "DELETE", "[{\"path\": \"\", \"fileName\": \"" + file.substring(1) + "\"}]");
		
		if (response == 200) {
			for (ConnectionListener listener : this.listeners) {
				listener.updateProgress(file, DataConnection.FINISHED, -1);
			}
		} else {
			for (ConnectionListener listener : this.listeners) {
				listener.updateProgress(file, DataConnection.FAILED, -1);
			}
		}

		return 0;
	}

	@Override
	public void addConnectionListener(ConnectionListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void setConnectionListeners(Vector<ConnectionListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public boolean rename(String from, String to) {
		Log.debug("rename");

		return false;
	}

	@Override
	public void filter(String searchText) {
		filterText = searchText;
		for (ConnectionListener listener : this.listeners) {
			listener.updateRemoteDirectory(this);
		}
		filterText = "";
	}

}