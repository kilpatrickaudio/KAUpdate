package com.kilpatrickaudio.kaUpdate.midi;

/**
 * This interface represents methods for handling received MIDI messages
 * 
 * @author andrew
 *
 */
public interface MIDIReceiveHandler {
	/**
	 * A normal message was received.
	 * 
	 * @param data the data as an array of ints
	 */
	public void messageReceived(int data[]);
	
	/**
	 * A SYSEX message was received.
	 * 
	 * @param data the data as an array of ints.
	 */
	public void sysexMessageReceived(int data[]);
	
	/**
	 * A debug text message was received.
	 * 
	 * @param text the text
	 */
	public void debugTextReceived(String text);
	
}
