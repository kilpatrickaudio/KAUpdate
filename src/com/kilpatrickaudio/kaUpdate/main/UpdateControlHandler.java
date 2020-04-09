package com.kilpatrickaudio.kaUpdate.main;

/**
 * This interface represents a control handler that can take care of updating.
 * 
 * @author andrew
 *
 */
public interface UpdateControlHandler {

	/**
	 * Causes a MIDI update to be performed.
	 * 
	 * @param midiDevInName the name of the MIDI input device to use
	 * @param midiDevOutName the name of the MIDI output device to use
	 * @param hexFilename the name of the hex file to use
	 */
	public void performMIDIUpdate(String midiDevInName, String midiDevOutName, String hexFilename);
	
	/**
	 * Causes a DFU update to be performed.
	 * 
	 * @param dfuFilename the name of the DFU file to use
	 */
	public void performDfuUpdate(String dfuFilename);
	
	/**
	 * Causes the application to be closed.
	 */
	public void closeApplication();
}
