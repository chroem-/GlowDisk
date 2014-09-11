/**
 * 
    This file is a part of the GlowDisk utility.
    Copyright (C) 2014 chroem

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.Glowstone.glowdisk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.Glowstone.glowdisk.virtualutils.VirtualDisk;
import org.Glowstone.glowdisk.virtualutils.VirtualFile;

public class GlowFile {

	public static final String pathSeparator = File.pathSeparator;
	public static final char pathSeparatorChar = File.pathSeparatorChar;
	
	public static final String separator = File.separator;
	public static final char separatorChar = File.pathSeparatorChar;
	
	private final boolean isVirtual = VirtualDisk.isEnabled();
	private String filePath;
	private final File file;
	private VirtualFile virtualFile;
	
	public GlowFile(String parent, String child) {
		this((parent.endsWith(separator)) ? parent + child : parent + separator + child);
	}
	
	public GlowFile(String pathname) {
		this.filePath = pathname;
		if (isVirtual) {	
			this.file = null;
			this.virtualFile = getVirtualFileByPath();
		} else {
			this.file = new File(pathname);
		}
	}
	
	public GlowFile(GlowFile parent, String child) {
		this.filePath = parent.getAbsolutePath() + child;
		if (isVirtual) {
			this.file = null;
			this.virtualFile = getVirtualFileByPath();
		} else {
			this.file = new File(parent.getFile(), child);
		}
	}
	
	private GlowFile(VirtualFile file) {
		this.filePath = file.getPath();
		this.virtualFile = file;
		this.file = null;
	}
	
	private GlowFile(File file) {
		this.filePath = file.getAbsolutePath();
		this.file = file;
	}

	private VirtualFile getVirtualFileByPath() {
		try {
			return getVirtualFileByPath(this.filePath);
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	private VirtualFile getVirtualFileByPath(String filePath) throws FileNotFoundException{
		String[] path = this.filePath.split(separator);
		path = (path[0].equals("")) ? Arrays.copyOfRange(path, 1, path.length): path;
		return virtualFile = VirtualDisk.getPrimaryDisk().getRoot().getChild(path);
	}
	
	protected boolean isVirtual() {
		return this.isVirtual;
	}
	
	protected File getFile() {
		return this.file;
	}
	
	protected VirtualFile getVirtualFile() {
		return this.virtualFile;
	}
	
	public OutputStream getOutputStream() throws IOException, FileNotFoundException {
		if (isVirtual) {
			if (!exists()) {
				this.getParentFile().mkdirs();
				this.createNewFile();
			}
			if (this.virtualFile.isDirectory()) {
				throw new IOException("The requested file is a directory and cannot be written to!");
			}
			return VirtualDisk.getPrimaryDisk().getOutputStream(this.virtualFile);
		} else {
			return new FileOutputStream(this.file);
		}		
	}
	
	public InputStream getInputStream() throws FileNotFoundException {
		if (isVirtual) {
			if (!exists()) {
				throw new FileNotFoundException("The file that is attempting to be read does not yet exist!");
			}
			return VirtualDisk.getPrimaryDisk().getInputStream(this.virtualFile);
		} else {
			return new FileInputStream(this.file);
		}
	}
	
	//check the following to make sure they have the correct throws statements!!!!
	
	public String getName() {
		String parentPath = filePath;
		if (parentPath.endsWith(separator)) {
			parentPath = filePath.substring(0, filePath.length() - 2);
		}
		String name = parentPath.substring(parentPath.lastIndexOf(separator), filePath.length() - 1);
		return name;
	}
	
	public String getParent() {
		if (isVirtual) {
			String parentPath = filePath;
			if (parentPath.endsWith(separator)) {
				parentPath = filePath.substring(0, filePath.length() - 2);
			}
			parentPath = parentPath.substring(0, parentPath.lastIndexOf(separator));
			return parentPath;
		} else {
			return file.getParent();
		}
	}
	
	public GlowFile getParentFile() {
		if (isVirtual) {
			return new GlowFile(getParent());
		} else {
			return new GlowFile(file.getParentFile());
		}
	}
	
	public String getAbsolutePath() {
		if (isVirtual) {
			return  filePath;
		} else {
			return file.getAbsolutePath();
		}
	}
	
	public boolean canWrite() {
		if (isVirtual) {
			boolean exists = exists();
			if (exists) {
				exists = virtualFile.getWriteable();
			}
			return exists;
		} else {
			return file.canWrite();
		}

	}
	
	public boolean exists() {
		if (isVirtual) {
			return this.virtualFile != null || ((this.virtualFile = getVirtualFileByPath()) != null);
		} else {
			return file.exists();
		}
	}
	
	public boolean isDirectory() {
		if (isVirtual) {
			return exists() || virtualFile.isDirectory();
		} else {
			return file.isDirectory();
		}
	}
	
	public boolean isFile() {
		if (isVirtual) {
			return !virtualFile.isDirectory();
		} else {
			return file.isFile();
		}
	}
	
	public long lastModified() {
		if (isVirtual) {
			if (exists()) {
				return virtualFile.getLastModified();
			} else {
				return 0;
			}
		} else {
			return file.lastModified();
		}
	}
	
	public long length()  {
		if (isVirtual) {
			if (exists()) {
				return virtualFile.getDataSize();
			} else {
				return 0;
			}
		} else {
			return file.length();
		}
	}
	
	public boolean createNewFile() throws IOException {
		if (isVirtual) {
			if (exists()) {
				return false;
			} else {
				try {
					this.virtualFile = new VirtualFile(this.filePath, false);
					return true;
				} catch (FileNotFoundException e) {
					return false;
				}
			}
		} else {
			return file.createNewFile();
		}
	}
	
	public boolean delete() {
		if (isVirtual) {
			boolean exists = exists();
			if (exists) {
				virtualFile.delete();
			}
			return exists;
		} else {
			return file.delete();
		}
	}
	
	public void deleteOnExit() {
		if (isVirtual && exists()) {
			virtualFile.deleteOnExit();
		} else {
			file.deleteOnExit();
		}
	}
	
	public String[] list() {
		if (isVirtual) {
			if (exists() && virtualFile.isDirectory()) {
				VirtualFile[] files = virtualFile.getChildren();
				String[] paths = new String[files.length];
				for (int i = 0; i < files.length; i++) {
					paths[i] = files[i].getPath();
				}
				return paths;
			} else {
				return new String[] {};
			}
		} else {
			return file.list();
		}
	}
	
	
	public GlowFile[] listFiles() {
		if (isVirtual) {
			if (exists() && virtualFile.isDirectory()) {
				VirtualFile[] files = virtualFile.getChildren();
				GlowFile[] glowFiles = new GlowFile[files.length];
				for (int i = 0; i < files.length; i++) {
					glowFiles[i] = new GlowFile(files[i]);
				}
				return glowFiles;
			} else {
				return new GlowFile[] {};
			}
		} else {
			File[] files = file.listFiles();
			GlowFile[] glowFiles = new GlowFile[files.length];
			for (int i = 0; i < files.length; i++) {
				glowFiles[i] = new GlowFile(files[i]);
			}
			return glowFiles;
		}
	}

	
	public boolean mkdir() {// come back
		if (isVirtual) {
			boolean doesNotExist = !exists();
			if (doesNotExist) {
				try {
						this.virtualFile = new VirtualFile(this.filePath, true);
						doesNotExist = true;
					} catch (IllegalArgumentException | FileNotFoundException e) {
						doesNotExist = false;
					} 
				} 
			return doesNotExist;
		} else {
			return file.mkdir();
		}
	}
	
	public boolean mkdirs() {// come back
		if (isVirtual) {
			boolean doesNotExist = !exists();
			if (doesNotExist) {
				this.virtualFile = VirtualDisk.getPrimaryDisk().mkdirs(this.filePath);
			}
			return doesNotExist;
		} else {
			return file.mkdirs();
		}
	}
	
	//possibly incorrect implementation
	public boolean renameTo(GlowFile dest) {
		this.filePath = dest.getAbsolutePath();
		if (isVirtual) {
			boolean exists = exists();
			if (exists) {
				VirtualFile destination;
				try {
					destination = getVirtualFileByPath(dest.getParent());
					exists = true;
				} catch (FileNotFoundException e) {
					GlowFile parent = dest.getParentFile();
					parent.mkdirs();
					destination = parent.getVirtualFile();
				}
				dest.delete();
				destination.addChild(this.virtualFile);
			}
			return exists;
		} else {
			return file.renameTo(dest.getFile());
		}
	}
	
	public boolean setLastModified(long time) {
		if (isVirtual) {
			boolean exists = exists();
				if (exists) {
					virtualFile.setLastModified(time);
				}
			return exists;
		} else {
			return file.setLastModified(time);
		}
	}
	
	public boolean setReadOnly() {
		return setWriteable(false);
	}
	
	public boolean setWriteable(boolean writeable) {
		if (isVirtual) {
			boolean exists = exists();
			if (exists) {
				virtualFile.setWriteable(writeable);
			}
			return exists();
		} else {
			return file.setWritable(writeable);
		}
	}
	
	public long getTotalSpace() {
		if (isVirtual) {
			return VirtualDisk.getPrimaryDisk().getSize();
		} else {
			return file.getTotalSpace();
		}
	}
		
	public long getFreeSpace() {
		if (isVirtual) {
			return VirtualDisk.getPrimaryDisk().getFreeSpace();
		} else {
			return file.getFreeSpace();
		}
	}
	
	public long getUsableSpace() {
		if (isVirtual) {
			return VirtualDisk.getPrimaryDisk().getUnreservedSpace();
		} else {
			return file.getUsableSpace();
		}
	}	
	
}
