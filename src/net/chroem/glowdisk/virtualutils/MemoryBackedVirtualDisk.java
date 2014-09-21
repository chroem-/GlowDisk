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

package net.chroem.glowdisk.virtualutils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;

import javax.activation.UnsupportedDataTypeException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.chroem.glowdisk.virtualutils.AllocatedSpaceMarker;


public class MemoryBackedVirtualDisk extends VirtualDisk{

	private JsonParser parser = new JsonParser();
	private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	
	protected MappedByteBuffer buffer;
	private final int size;
	
	public MemoryBackedVirtualDisk(File diskFile, int size) throws UnsupportedDataTypeException {
		super(size);
		if (size < 1) throw new IllegalArgumentException ("The disk must be at least one byte long!");
		if (!diskFile.getName().endsWith(".gldsk")) throw new UnsupportedDataTypeException("GlowDisk requires files to be in the .gldsk format!");
		this.size = size;
		try {
			try {
				diskFile.createNewFile();
				FileOutputStream output = new FileOutputStream(diskFile);
				byte[] blankData = new byte[4096];
				Arrays.fill(blankData, Byte.MIN_VALUE);
				int largeIterations =  size / 4096;
				int smallIterations = size % 4096;
				for (int i = 0; i < largeIterations; i++) {
					output.write(blankData);
				}
				for (int i = 0; i < smallIterations; i++){
					output.write(Byte.MIN_VALUE);
				}
				output.close();
			} catch (FileNotFoundException | SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			try {
				@SuppressWarnings("resource")
				FileChannel channel = new RandomAccessFile(diskFile, "rw").getChannel();
				this.buffer = channel.map(MapMode.READ_WRITE, 0, diskFile.length());
				channel.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	
	public MemoryBackedVirtualDisk(File diskFile) throws FileNotFoundException, IOException{
		super((int) diskFile.length() - 1024);
		this.size = (int) diskFile.length() - 1024;
		FileChannel channel;
		channel = new RandomAccessFile(diskFile, "rw").getChannel();
		this.buffer = channel.map(MapMode.READ_WRITE, 0, diskFile.length());
		channel.close();
		
		for (int i = 0; i < 64; i++) {
			if (buffer.getLong(i * 16) != Long.MIN_VALUE) {
				long startIndex = buffer.getLong(i * 16);
				long endIndex = buffer.getLong((i * 16) + 8);
				AllocatedSpaceMarker.forceAddNewAllocatedZoneToParent(super.manifest, (int) startIndex, (int) endIndex);
			} else {
				break;
			}
			InputStream input = getInputStream(super.manifest);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int len;
			while ((len = input.read(buffer)) > -1) {
				output.write(buffer, 0, len);
			}
			input.close();
			JsonObject root = parser.parse(output.toString()).getAsJsonObject();
			try {
				super.getRoot().constructChildrenFromJson(root.get("children").getAsJsonArray());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	@Override
	public OutputStream getOutputStream(VirtualFile file) throws IOException{
		return new MemoryFileOutputStream(file);
	}

	@Override
	public InputStream getInputStream(VirtualFile file) {
		return new MemoryFileInputStream(file);
	}
	
	@Override
	public void updateFileManifest() {
		try {
			OutputStream output =  new ManifestOutputStream();
			String json = gson.toJson(super.getRoot());
			output.write(json.getBytes());
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class MemoryFileOutputStream extends OutputStream {
		private final MappedByteBuffer buffer;
		private final VirtualFile file;
		
		private int fileIndex;
		private int diskIndex;
		
		//private final int totalSegments;
		private AllocatedSpaceMarker currentSegment;
		private int currentSegmentNumber;
		private int indexInSegment;
		
		private int dataSize;
		
		
		//unused
		private MemoryFileOutputStream() {
			this.buffer = null;
			this.file = null;
			//this.totalSegments = 0;
		}
		
		public MemoryFileOutputStream(VirtualFile file) throws IOException {
			if (!file.getWriteable()) {
				throw new IOException("This virtual file has been marked read only!");
			}
			file.setLastModified(System.currentTimeMillis());
			
			this.file = file;
			this.buffer = ((MemoryBackedVirtualDisk) file.getContainingDisk()).buffer;
			
		//	this.totalSegments = file.dataSegments.size();
			if (!file.hasData) {
				file.changeSize(file.getContainingDisk().getDefaultSegmentSize());
			}
			this.currentSegment = file.dataSegments.get(0);
			this.currentSegmentNumber = 0;
			this.indexInSegment = 0;
			this.dataSize = 0;
		}
		
		
		
		@Override
		public void write(int b) throws IOException {
			int bufferIndex = getBufferIndex();
			if (indexIsValid(bufferIndex)) {
				buffer.put(bufferIndex, (byte) b);
			} else if (advanceToNextSegment()){
				buffer.put(getBufferIndex(), (byte) b);
			} else {
				this.file.changeSize(this.file.getContainingDisk().getDefaultSegmentSize());
				write(b);
			}
			this.indexInSegment++;
			this.dataSize++;
		}
		
		@Override
		public void write(byte[] b) throws IOException {
			int bufferIndex = getBufferIndex();
			int sizeWritten = 0;
			if (indexIsValid(bufferIndex) && indexIsValid(bufferIndex + b.length)) {
				buffer.position(bufferIndex);
				buffer.put(b);
				sizeWritten = b.length;
			} else if (available() > 0) {
				int available = available();
				buffer.position(bufferIndex);
				buffer.put(Arrays.copyOf(b, available));
				indexInSegment += available; //special case (splits array into two different arrays for writing across separate segments)
				dataSize += available;
				write(Arrays.copyOfRange(b, available + 1, b.length));
			} else if (advanceToNextSegment()) {
				write(b);
			} else {
				this.file.changeSize(this.file.getContainingDisk().getDefaultSegmentSize());
				write(b);
			}

			indexInSegment += sizeWritten;
			this.dataSize += sizeWritten;
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException{
			int bufferIndex = getBufferIndex();
			int sizeWritten = 0;
			if (indexIsValid(bufferIndex) && indexIsValid(bufferIndex + len)) {
				buffer.position(bufferIndex);
				buffer.put(b, off, len);
				sizeWritten = b.length;
			} else if (available() > 0) {
				int available = available();
				buffer.position(bufferIndex);
				buffer.put(Arrays.copyOf(b, off + available));// possibly incorrect implementation (TEST!)
				indexInSegment += available; //special case (splits array into two different arrays for writing across separate segments)
				dataSize += available;
				write(Arrays.copyOfRange(b, off + available + 1, len));
			} else if (advanceToNextSegment()) {
				write(b);
			} else {
				this.file.changeSize(this.file.getContainingDisk().getDefaultSegmentSize());
				write(b, off, len);
				
			}
			indexInSegment += sizeWritten;
			this.dataSize += sizeWritten;
		}
		
		@Override 
		public void flush() throws IOException {	
		}
		
		@Override
		public void close() throws IOException {
			file.changeSize(this.dataSize - file.getAllocatedSize() + 1);  //gets rid of the extra preallocated space
			super.close();
			file.getContainingDisk().updateFileManifest();
		}
		
		private boolean indexIsValid(int index) {
			return (this.currentSegment.endIndex >= index && this.currentSegment.beginIndex <= index && index + 1 < size );
		}
		
		private boolean advanceToNextSegment() {
			if (currentSegmentNumber + 1 < file.dataSegments.size()) {
				this.currentSegmentNumber++;
				this.currentSegment = file.dataSegments.get(currentSegmentNumber);
				this.indexInSegment = 0;
				//this.buffer.position(this.currentSegment.beginIndex);
				return true;
			} else {
				return false;
			}
			
		}
		
		private int getBufferIndex() {
			return currentSegment.beginIndex + indexInSegment;
		}
		
		
		private int available() throws IOException {
			return currentSegment.getSize() - 1 - indexInSegment;
		}
		
	}
	
	public class MemoryFileInputStream extends InputStream {
		private final MappedByteBuffer buffer;
		private final VirtualFile file;
		
		private AllocatedSpaceMarker currentSegment;
		private int currentSegmentNumber;
		private int indexInSegment;

		
		public MemoryFileInputStream(VirtualFile file) {
			this.file = file;
			this.buffer = ((MemoryBackedVirtualDisk) file.getContainingDisk()).buffer;
			
			this.currentSegment = file.dataSegments.get(0);
			this.currentSegmentNumber = 0;
			this.indexInSegment = 0;
		}
		
		@Override
		public int read() throws IOException {
			int bufferIndex = getBufferIndex();
			byte storedByte;
			if (indexIsValid(bufferIndex)) {
				storedByte = buffer.get(bufferIndex);
			} else if (advanceToNextSegment()){
				storedByte = buffer.get(getBufferIndex());
			} else {
				storedByte = -1;
			}
			this.indexInSegment++;
			return storedByte;
		}
		
		@Override
		public int read(byte[] b) throws IOException {
			int bufferIndex = getBufferIndex();
			int sizeRead;
			if (indexIsValid(bufferIndex) && indexIsValid(bufferIndex + b.length)) {
				buffer.position(bufferIndex);
				buffer.get(b);
				sizeRead = b.length;
			} else if ((sizeRead = available()) > 0) {
				buffer.position(bufferIndex);
				buffer.get(b);
			} else if (advanceToNextSegment()) {
				sizeRead = read(b);
			} else {
				sizeRead = -1;
			}
			indexInSegment += sizeRead;
			return sizeRead;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException{
			int bufferIndex = getBufferIndex();
			int sizeRead;
			if (indexIsValid(bufferIndex) && indexIsValid(bufferIndex + len)) {
				buffer.position(bufferIndex);
				buffer.get(b, off, len);
				sizeRead = b.length;
			} else if (available() > 0) {
				buffer.position(bufferIndex);
				buffer.get(b, off, len);
				sizeRead = available();
			} else if (advanceToNextSegment()) {
				sizeRead = read(b, off, len);
			} else {
				sizeRead = -1;
			}
			indexInSegment += sizeRead;
			return sizeRead;
		}
		
		@Override 
		public long skip(long n) throws IOException {
			int bufferIndex = getBufferIndex();
			int sizeSkipped;
			if (indexIsValid(bufferIndex) && indexIsValid( (bufferIndex + (int) n))) {
				sizeSkipped = (int) n;
			} else if (available() > 0) {
				sizeSkipped = available();
			} else if (advanceToNextSegment()) {
				sizeSkipped = (int) skip(n);
			} else {
				sizeSkipped = -1;
			}
			indexInSegment += sizeSkipped;
			return sizeSkipped;
		}
		
		@Override
		public int available() throws IOException {
			return currentSegment.getSize() - 1 - indexInSegment;
		}
		
		@Override
		public void close() throws IOException {
		}
		
		
		
		
		
		
		
		
		@Override
		public void mark(int readlimit) {
			
		}
		
		@Override
		public void reset() throws IOException {
			
		}
		
		@Override
		public boolean markSupported() {
			return false;
		}
		
		private boolean indexIsValid(int index) {
			return (this.currentSegment.endIndex >= index && this.currentSegment.beginIndex <= index && index + 1 < size);
		}
		
		private boolean advanceToNextSegment() {
			if (currentSegmentNumber + 1 < file.dataSegments.size()) {
				this.currentSegmentNumber++;
				this.currentSegment = file.dataSegments.get(currentSegmentNumber);
				this.indexInSegment = 0;
				//this.buffer.position(this.currentSegment.beginIndex);
				return true;
			} else {
				return false;
			}
			
		}
		
		private int getBufferIndex() {
			return currentSegment.beginIndex + indexInSegment;
		}
		
	}
	
	public class ManifestOutputStream extends MemoryFileOutputStream {
		
		public ManifestOutputStream() throws IOException {
			super(VirtualDisk.getPrimaryDisk().manifest);
		}
		
		@Override
		public void write(byte[] b) {
			try {
				super.write(b);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void write(byte[] b, int off, int len) {
			try {
				super.write(b, off, len);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		@Override
		public void close() throws IOException {
			super.file.changeSize(super.dataSize - super.file.getAllocatedSize() + 1);  //gets rid of the extra preallocated space
			for (int i = 0; i < super.file.dataSegments.size(); i++) {
				AllocatedSpaceMarker marker = super.file.dataSegments.get(i);
				long startIndex = marker.beginIndex;
				long endIndex = marker.endIndex;
				buffer.putLong(i * 16, startIndex);
				buffer.putLong((i * 16) + 8, endIndex);
			}
			for (int i = super.file.dataSegments.size(); i < 64; i++) {
				buffer.putLong(i * 16, Long.MIN_VALUE);
				buffer.putLong((i * 16) + 8, Long.MIN_VALUE);
			}
		}
		
	}
	
}
