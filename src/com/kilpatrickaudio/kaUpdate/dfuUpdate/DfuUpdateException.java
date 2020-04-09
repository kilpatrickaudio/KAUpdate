package com.kilpatrickaudio.kaUpdate.dfuUpdate;

/**
 * This class represents a DFU update exception.
 * 
 * @author andrew
 *
 */
@SuppressWarnings("serial")
public class DfuUpdateException extends Exception {

	/**
	 * Creates a DFU update exception.
	 * 
	 * @param msg the error message
	 */
	public DfuUpdateException(String msg) {
		super(msg);
	}
}
