package com.kilpatrickaudio.kaUpdate.main;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This class handles logger output and redirects it so it can be displayed in the GUI.
 *
 * @author andrew
 */
public class UpdateTextAppender extends AppenderSkeleton {
	KAUpdateGUI parent;
    
	/**
	 * Creates a new text appender.
	 * 
	 * @param parent the parent used for handling the actual messages.
	 */
	public UpdateTextAppender(KAUpdateGUI parent) {
		this.parent = parent;
		setThreshold(Level.DEBUG);
		setLayout(new PatternLayout("%r: %m%n"));
	}

	@Override
	public void close() {
		parent.debug("log closed.");
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	@Override
	protected void append(LoggingEvent le) {
		parent.debug(this.layout.format(le));		
	}
}