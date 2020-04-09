package com.kilpatrickaudio.kaUpdate.moduleUpdate;

/**
 * This class represents a hex parse exception.
 * 
 * @author andrew
 *
 */
@SuppressWarnings("serial")
public class HexParseException extends Exception {
	
	/**
	 * Creates a new hex parse exception.
	 * 
	 * @param msg the error message
	 */
	public HexParseException(String msg) {
		super(msg);
	}
}
