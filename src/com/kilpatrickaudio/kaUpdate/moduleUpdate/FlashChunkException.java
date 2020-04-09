package com.kilpatrickaudio.kaUpdate.moduleUpdate;

/**
 * This class represents a flash chunk exception.
 * 
 * @author andrew
 *
 */
@SuppressWarnings("serial")
public class FlashChunkException extends Exception {
	
	/**
	 * Creates a new flash chunk exception.
	 * 
	 * @param msg the error message
	 */
	public FlashChunkException(String msg) {
		super(msg);
	}
}
