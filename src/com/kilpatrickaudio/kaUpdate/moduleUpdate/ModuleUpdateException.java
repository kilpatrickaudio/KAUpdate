package com.kilpatrickaudio.kaUpdate.moduleUpdate;

/**
 * This class represents a module update exception.
 * 
 * @author andrew
 *
 */
@SuppressWarnings("serial")
public class ModuleUpdateException extends Exception {

	/**
	 * Creates a module update exception.
	 * 
	 * @param msg the error message
	 */
	public ModuleUpdateException(String msg) {
		super(msg);
	}
}
