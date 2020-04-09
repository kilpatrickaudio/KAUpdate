package com.kilpatrickaudio.kaUpdate.moduleUpdate;

/**
 * This class handles chunk to flash to the module.
 * 
 * @author andrew
 *
 */
public class FlashChunk {
	int start;
	int data[];
	
	/**
	 * Creates a new flash chunk.
	 * 
	 * @param start the start offset
	 * @param size the size in bytes
	 */
	public FlashChunk(int start, int size) {
		this.start = start;
		data = new int[size];
		for(int i = 0; i < data.length; i ++) {
			data[i] = 0xff;
		}
	}
	
	/**
	 * Adds data to the chunk.
	 * 
	 * @param offset the offset
	 * @param newData the data
	 * @return the data that was not added because the chunk was full (it should be added to the next chunk)
	 * @throws FlashChunkException if offset is invalid
	 */
	public int addData(int offset, int newData[]) throws FlashChunkException {
		int remain = 0;
		if(offset > (data.length - 1)) {
			throw new FlashChunkException("offset past end of chunk");			 
		}
		// offset + data will go off the end of the chunk
		if((offset + newData.length) > data.length) {
			remain = newData.length - (getLength() - offset);
			System.arraycopy(newData, 0, data, offset, (getLength() - offset));
		}
		else {
			System.arraycopy(newData, 0, data, offset, newData.length);			
		}
		return remain;
	}
	
	/**
	 * Gets the chunk data.
	 * 
	 * @return the chunk data
	 */
	public int[] getData() {
		return data;
	}
	
	/**
	 * Gets the start address of the chunk.
	 * 
	 * @return the start address
	 */
	public int getStartAddr() {
		return start;
	}
	
	/**
	 * Gets the length of the chunk.
	 * 
	 * @return
	 */
	public int getLength() {
		return data.length;
	}
}
