package com.kilpatrickaudio.kaUpdate.midi;

import java.util.LinkedList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiDevice.Info;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;

import org.apache.log4j.Logger;

/**
 * This class implements MIDI input and output routines.
 * 
 * @author andrew
 *
 */
public class MIDIHandler implements Receiver {
	Logger logger;
	MidiDevice inputDevice = null;
	MidiDevice outputDevice = null;
	Transmitter in = null;
	Receiver out = null;
	MIDIReceiveHandler mrh = null;

	public static final int MIDI_CMD_DEBUG = 0x01;
	public static final int MIDI_CMD_FIRMWARE_LOAD_CMD = 0x04;
	public static final int MIDI_CMD_FORMWARE_LOAD_OK = 0x05;
	public static final int MIDI_CMD_ALIVE_STATUS = 0x7f;

	int retData[] = null;
	boolean gotResponse = false;

	/**
	 * Creates a new MIDI handler to manage MIDI ports.
	 * 
	 * @throws MidiUnavailableException if there is an error querying MIDI devices.
	 */
	public MIDIHandler() throws MidiUnavailableException {
		logger = Logger.getLogger(this.getClass());
		logger.info(getDeviceNamePrintout());
/*		
		Thread t = new Thread() {
			public void run() {
				closeMIDIPorts();
			}
		};
		Runtime.getRuntime().addShutdownHook(t);
*/		
	}

	/**
	 * Creates a new MIDI handler and also opens ports at the same time.
	 * 
	 * @param inDevName the input device name
	 * @param outDevName the output device name
	 * @throws MidiUnavailableException if there is a problem opening the ports
	 */
	public MIDIHandler(String inDevName, String outDevName) throws MidiUnavailableException {
		logger = Logger.getLogger(this.getClass());
		logger.info(getDeviceNamePrintout());
		openMIDIPorts(inDevName, outDevName);
/*		
		Thread t = new Thread() {
			public void run() {
				closeMIDIPorts();
			}
		};
		Runtime.getRuntime().addShutdownHook(t);
*/
	}

	/**
	 * Sets the handler that will handle messages received on the input port.
	 * 
	 * @param handler the handler
	 */
	public void setReceiveHandler(MIDIReceiveHandler handler) {
		mrh = handler;
	}
	
	/**
	 * Gets a list of MIDI input device names.
	 * 
	 * @return a list of MIDI input device names
	 * @throws MidiUnavailableException if there is a problem accessing the MIDI system
	 */
	public static LinkedList<String> getInputDeviceNames()
			throws MidiUnavailableException {
		LinkedList<String> inputNames = new LinkedList<String>();
		Info midiDevices[] = CoreMidiDeviceProvider.getMidiDeviceInfo();
		for (int i = 0; i < midiDevices.length; i++) {
			MidiDevice dev = MidiSystem.getMidiDevice(midiDevices[i]);
			// -1 = unlimited number of ports
			if (dev.getMaxTransmitters() != 0) {
				inputNames.addLast(midiDevices[i].getName());
			}
		}
		return inputNames;
	}

	/**
	 * Gets a list if MIDI output device names.
	 * 
	 * @return a list of MIDI output device names
	 * @throws MidiUnavailableException if there is a problem accessing the MIDI system
	 */
	public static LinkedList<String> getOutputDeviceNames()
			throws MidiUnavailableException {
		LinkedList<String> outputNames = new LinkedList<String>();
		Info midiDevices[] = CoreMidiDeviceProvider.getMidiDeviceInfo();
		for (int i = 0; i < midiDevices.length; i++) {
			MidiDevice dev = MidiSystem.getMidiDevice(midiDevices[i]);
			// -1 = unlimited number of ports
			if (dev.getMaxReceivers() != 0) {
				outputNames.addLast(midiDevices[i].getName());
			}
		}
		return outputNames;
	}

	/**
	 * 
	 * Gets a printable list of MIDI devices as a String.
	 * 
	 * @return a printable list of MIDI devices
	 * @throws MidiUnavailableException
	 */
	public static String getDeviceNamePrintout()
			throws MidiUnavailableException {
		String msg;

		// print a list of valid midi devices
		LinkedList<String> inputNames = MIDIHandler.getInputDeviceNames();
		LinkedList<String> outputNames = MIDIHandler.getOutputDeviceNames();
		msg = "\nMIDI inputs:\n";
		for (int i = 0; i < inputNames.size(); i++) {
			msg += "dev: " + i + " - " + inputNames.get(i) + "\n";
		}
		msg += "\nMIDI outputs:\n";
		for (int i = 0; i < outputNames.size(); i++) {
			msg += "dev: " + i + " - " + outputNames.get(i) + "\n";
		}
		return msg;
	}

	/**
	 * Opens MIDI input and output ports.
	 * 
	 * @param inDevName the input device name
	 * @param outDevName the output device name
	 * @throws MidiUnavailableException if there is an problem opening the ports
	 */
	public void openMIDIPorts(String inDevName, String outDevName)
			throws MidiUnavailableException {
		// get device by searching names
		Info midiDevices[] = CoreMidiDeviceProvider.getMidiDeviceInfo();
		int inputDevNum = -1;
		int outputDevNum = -1;
		if(!inDevName.equals("")) {
			inputDevNum = -2;
		}
		if(!outDevName.equals("")) {
			outputDevNum = -2;
		}
		for(int i = 0; i < midiDevices.length; i++) {
			if(midiDevices[i].getName().toLowerCase().trim().equals(inDevName.toLowerCase().trim())
					&& MidiSystem.getMidiDevice(midiDevices[i])
							.getMaxTransmitters() != 0) {
				inputDevNum = i;
			}
			if(midiDevices[i].getName().toLowerCase().trim().equals(outDevName.toLowerCase().trim())
					&& MidiSystem.getMidiDevice(midiDevices[i])
							.getMaxReceivers() != 0) {
				outputDevNum = i;
			}
		}

		if(inputDevNum == -2) {
			throw new MidiUnavailableException("MIDI input not found: " + inDevName);
		}
		
		if(outputDevNum == -2) {
			throw new MidiUnavailableException("MIDI output not found: " + outDevName);
		}
		
		if(inputDevNum >= 0) {
			logger.info("opening MIDI in port: "
					+ midiDevices[inputDevNum].getName());
			inputDevice = MidiSystem.getMidiDevice(midiDevices[inputDevNum]);
			inputDevice.open();
			in = inputDevice.getTransmitter();
			in.setReceiver(this);
		}

		if(outputDevNum >= 0) {
			logger.info("opening MIDI out port: "
					+ midiDevices[outputDevNum].getName());
			outputDevice = MidiSystem.getMidiDevice(midiDevices[outputDevNum]);
			outputDevice.open();
			out = outputDevice.getReceiver();
		} else {
			out = new FakeReceiver();
		}
	}

	/**
	 * Closes any open MIDI ports.
	 */
	public void closeMIDIPorts() {
		logger.info("closing MIDI ports...");

		if (in != null) {
			in.close();
			in = null;
		}
		if (inputDevice != null) {
			inputDevice.close();
			inputDevice = null;
		}
		if (out != null) {
			out.close();
			out = null;
		}
		if (outputDevice != null) {
			outputDevice.close();
			outputDevice = null;
		}
	}

	/**
	 * Sends SYSEX message and waits for a response.
	 * 
	 * @param data the data to send
	 * @param len the length of the data to send
	 * @param timeout the receive timeout in ms
	 * @throws InvalidMidiDataException  if there is an error with the MIDI ports
	 */
	public int[] sendSysexMessageRxResponse(int data[], int len, int timeout) throws InvalidMidiDataException {
		if(out == null) {
			throw new InvalidMidiDataException("output port is not enabled");
		}
		waitForSysexMessage(10);  // flush buffer
		byte dataBytes[] = new byte[len];
		for (int i = 0; i < len; i++) {
			dataBytes[i] = (byte) data[i];
		}
		SysexMessage msg = new SysexMessage();
		msg.setMessage(dataBytes, dataBytes.length);
		out.send(msg, -1);
		gotResponse = false;
		retData = null;

		long lastTime = System.currentTimeMillis();
		while ((System.currentTimeMillis() - lastTime) < timeout) {
			if(gotResponse) {
				return retData;
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Sends SYSEX message and returns immediately.
	 * 
	 * @param data the data to send
	 * @param len the length of data to send
	 * @throws InvalidMidiDataException if there was an error
	 */
	public void sendSysexMessage(int data[], int len)
			throws InvalidMidiDataException {
		if(out == null) {
			throw new InvalidMidiDataException("output port is not enabled");
		}
		waitForSysexMessage(10);  // flush buffer
		byte dataBytes[] = new byte[len];
		for (int i = 0; i < len; i++) {
			dataBytes[i] = (byte) data[i];
		}
		SysexMessage msg = new SysexMessage();
		msg.setMessage(dataBytes, dataBytes.length);
		out.send(msg, -1);
	}

	/**
	 * Wait for a SYSEX message without sending something first.
	 * 
	 * @param timeout the receive timeout in ms
	 * @return an array containing the received data, or null if no data was
	 *         received
	 * @throws InvalidMidiDataException
	 */
	public int[] waitForSysexMessage(int timeout)
			throws InvalidMidiDataException {
		gotResponse = false;
		retData = null;

		long now = System.currentTimeMillis();
		while ((System.currentTimeMillis() - now) < timeout) {
			if (gotResponse) {
				return retData;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Sends a MIDI message with one byte.
	 * 
	 * @param status the status byte
	 * @throws InvalidMidiDataException if the message could not be sent
	 */
	public void sendMessage(int status) throws InvalidMidiDataException {
		if (out == null) {
			throw new InvalidMidiDataException("output port is not enabled");
		}
		ShortMessage msg = new ShortMessage();
		msg.setMessage(status);
		out.send(msg, -1);
	}

	/**
	 * Sends a MIDI message with two bytes.
	 * 
	 * @param status the status byte
	 * @param data0 the data0 byte
	 * @throws InvalidMidiDataException if the message could not be sent
	 */
	public void sendMessage(int status, int data0) throws InvalidMidiDataException {
		if (out == null) {
			throw new InvalidMidiDataException("output port is not enabled");
		}
		ShortMessage msg = new ShortMessage();
		msg.setMessage(status, data0, 0);
		out.send(msg, -1);
	}

	/**
	 * Sends a MIDI message with three bytes.
	 * 
	 * @param status the status byte
	 * @param data0 the data0 byte
	 * @param data1 the data1 byte
	 * @throws InvalidMidiDataException if the message could not be sent
	 */
	public void sendMessage(int status, int data0, int data1)
			throws InvalidMidiDataException {
		if (out == null) {
			throw new InvalidMidiDataException("output port is not enabled");
		}
		ShortMessage msg = new ShortMessage();
		msg.setMessage(status, data0, data1);
		out.send(msg, -1);
	}
	
	@Override
	public void close() {
		logger.info("MIDI output closing.");
		if(out != null) {
			out.close();
			out = null;
		}
		if(outputDevice != null) {
			outputDevice.close();
			outputDevice = null;
		}
		logger.info("MIDI ports closed.");
	}

	/**
	 * Event handler for messages received by the MIDI port.
	 */
	@Override
	public void send(MidiMessage message, long timeStamp) {
		byte msgData[] = message.getMessage();
		if(msgData.length > 0) {
			retData = new int[msgData.length];
			for(int i = 0; i < msgData.length; i++) {
				retData[i] = (msgData[i] & 0xff);
			}
		}
		// sysex messages
		if (message.getStatus() == 0xf0) {
			if (message.getLength() < 3) {
				logger.warn("SYSEX message is too short - length: "
						+ message.getLength());
				return;
			}
			gotResponse = true;
			if (mrh != null) {
				mrh.sysexMessageReceived(retData);
			}
		} else {
			gotResponse = true;
			if (mrh != null)
				mrh.messageReceived(retData);
		}
	}
}
