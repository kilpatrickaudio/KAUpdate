package com.kilpatrickaudio.kaUpdate.moduleUpdate;

import com.kilpatrickaudio.kaUpdate.main.ProgressIndicator;

/**
 * This interface represents a module updater.
 * 
 * @author andrew
 *
 */
public interface ModuleUpdater {

	/**
	 * Updates a module.
	 * 
	 * @param pi the process indicator
	 * @throws ModuleUpdateException if there is a problem updating the module
	 */
	public void updateModule(ProgressIndicator pi) throws ModuleUpdateException;
}
