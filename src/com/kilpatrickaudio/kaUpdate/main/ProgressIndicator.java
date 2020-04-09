package com.kilpatrickaudio.kaUpdate.main;

/**
 * This interface represents a control that can show a progress percentage.
 * 
 * @author andrew
 *
 */
public interface ProgressIndicator {

	/**
	 * Sets the percentage conplete.
	 * 
	 * @param percent the percentage
	 */
	public void setPercentComplete(int percent);
}
