package com.kilpatrickaudio.kaUpdate.moduleUpdatePIC18;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import com.kilpatrickaudio.kaUpdate.main.ProgressIndicator;
import com.kilpatrickaudio.kaUpdate.midi.MIDIHandler;
import com.kilpatrickaudio.kaUpdate.moduleUpdate.FlashChunk;
import com.kilpatrickaudio.kaUpdate.moduleUpdate.HexParser;
import com.kilpatrickaudio.kaUpdate.moduleUpdate.ModuleUpdateException;
import com.kilpatrickaudio.kaUpdate.moduleUpdate.ModuleUpdater;

import org.apache.log4j.Logger;

/**
 * This class implements the update methods used for communicating with 
 * the Kilpatrick Audio PIC18 bootloader.
 * 
 * @author andrew
 *
 */
public class ModuleUpdatePIC18 implements ModuleUpdater {
	Logger logger;
	MIDIHandler midi;
	String inDevName;
	String outDevName;
	String hexFilename;
	int devID;
	
	/**
	 * Updates a PIC18 module.
	 * 
	 * @param midi the MIDI subsystem
	 * @param inDev the MIDI input port dev number
	 * @param outDev the MIDI output port dev number
	 * @param hexFilename the hex filename
	 * @param devID the device ID to use for restart command
	 */
	public ModuleUpdatePIC18(MIDIHandler midi, String inDevName, String outDevName, String hexFilename, int devID) {
		logger = Logger.getLogger(this.getClass());
		this.midi = midi;
		this.inDevName = inDevName;
		this.outDevName = outDevName;
		this.hexFilename = hexFilename;
		this.devID = devID;
	}

	/**
	 * Updates a PIC18 module.
	 * 
	 * @param pi the progress indicator to send updates to
	 * @throws ModuleUpdateException if there is a problem with the update
	 */
	public void updateModule(ProgressIndicator pi) throws ModuleUpdateException {
		pi.setPercentComplete(0);
		
		// open and parse the HEX file
		File hexFile = new File(hexFilename);
		if(hexFile.canRead()) {
			logger.info("hex file: " + hexFile.getAbsolutePath());
		}
		else {
			throw new ModuleUpdateException("hex file is not readable: " + hexFile.getAbsolutePath());
		}
		int prog[] = HexParser.parseHexRecords(hexFile);
		if(prog == null) {
			throw new ModuleUpdateException("hex file parse error");
		}

		// convert the data into chunks
		LinkedHashMap<Integer,FlashChunk> flashMap = HexParser.creatChunkyData(prog, 64);
		int chunksTotal = flashMap.size();
		int chunksCompleted = 0;
		logger.info("chunks - total: " + chunksTotal);
		// check chunk addresses
		Iterator<Integer> iter = flashMap.keySet().iterator();
		int chunkCount = 0;
		while(iter.hasNext()) {
			Integer key = iter.next();
			FlashChunk chunk = flashMap.get(key);
			int addr = chunk.getStartAddr();
			if(addr == 0x00) {
				throw new ModuleUpdateException("application code starts at 0x0000! - cannot program");
			}
			logger.debug("chunk " + chunkCount + " - addr: " + Integer.toHexString(addr));
			chunkCount ++;
		}
		
		// program the chunks over MIDI
		try {
			midi.openMIDIPorts(inDevName, outDevName);
		} catch (MidiUnavailableException e1) {
			throw new ModuleUpdateException(e1.getMessage());
		}
		
		logger.info("sending reset packet to device");
		int resetBuf[] = new int[11];
		resetBuf[0] = 0xf0;
		resetBuf[1] = 0x00;
		resetBuf[2] = 0x01;
		resetBuf[3] = 0x72;
		resetBuf[4] = 0x7e;
		resetBuf[5] = devID;
		resetBuf[6] = 0x4b;
		resetBuf[7] = 0x49;
		resetBuf[8] = 0x4c;
		resetBuf[9] = 0x4c;
		resetBuf[10] = 0xf7;
		try {
			midi.sendSysexMessage(resetBuf, resetBuf.length);
		} catch (InvalidMidiDataException e1) {
			throw new ModuleUpdateException(e1.getMessage());			
		}
		
		logger.info("waiting for device to come online...");		
		try {
			int waitCount = 0;
			int retMsg[];
			while((retMsg = midi.waitForSysexMessage(1000)) == null) {
				if(waitCount > 10) {
					throw new ModuleUpdateException("device did not come online");
				}
				waitCount ++;
			}
			if(retMsg.length != 6) {
				throw new ModuleUpdateException("device alive message was incorrect length: " + retMsg.length);
			}
			if(retMsg[0] == 0xf0 && retMsg[1] == 0x00 && retMsg[2] == 0x01 &&
					retMsg[3] == 0x72 && retMsg[4] == 0x7f && retMsg[5] == 0xf7) {
				logger.debug("device alive msg is correct");
			}
			else {
				throw new ModuleUpdateException("device online message is incorrect");
			}
		} catch (InvalidMidiDataException e) {
			throw new ModuleUpdateException(e.getMessage());
		}
		logger.info("device detected");

		// load each chunk
		logger.info("loading chunks: " + flashMap.size() + " total");
		iter = flashMap.keySet().iterator();
		while(iter.hasNext()) {
			Integer key = iter.next();
			FlashChunk chunk = flashMap.get(key);
			int addr = chunk.getStartAddr();
			if(addr < 0x800 || addr > 0x7fff) {
				logger.warn("address out of range (probably config data) - skipping...");
				continue;
			}
			logger.info("loading chunk addr: 0x" + Integer.toHexString(addr) + " -> to device");
			int data[] = chunk.getData();
			
			// format data and address for sending over MIDI (4 bits per word)
			int sendData[] = new int[(4 + 64) * 2];
			int sendLen = 0;
			sendData[sendLen ++] = (addr >> 28) & 0x0f;
			sendData[sendLen ++] = (addr >> 24) & 0x0f;
			sendData[sendLen ++] = (addr >> 20) & 0x0f;
			sendData[sendLen ++] = (addr >> 16) & 0x0f;
			sendData[sendLen ++] = (addr >> 12) & 0x0f;
			sendData[sendLen ++] = (addr >> 8) & 0x0f;
			sendData[sendLen ++] = (addr >> 4) & 0x0f;
			sendData[sendLen ++] = addr & 0x0f;
			for(int i = 0; i < data.length; i ++) {
				sendData[sendLen ++] = (data[i] >> 4) & 0x0f;
				sendData[sendLen ++] = data[i] & 0x0f;
			}
			// create and send the MIDI message
			int msg[] = new int[sendData.length + 6];
			int msgLen = 0;
			msg[msgLen ++] = 0xf0;
			msg[msgLen ++] = 0x00;
			msg[msgLen ++] = 0x01;
			msg[msgLen ++] = 0x72;
			msg[msgLen ++] = 0x04;
			for(int i = 0; i < sendData.length; i ++) {
				msg[msgLen ++] = sendData[i];
			}
			msg[msgLen ++] = 0xf7;	
			int retData[] = null;
			try {
				retData = midi.sendSysexMessageRxResponse(msg, msgLen, 1000);
			} catch (InvalidMidiDataException e) {
				throw new ModuleUpdateException(e.getMessage());
			}
			int chksum = 0;
			for(int i = 0; i < data.length; i ++) {
				chksum += data[i];
			}
			chksum = (chksum & 0x7f);
			if(retData != null && retData[4] == 0x05) {
				if(retData[5] != chksum) {
					throw new ModuleUpdateException("flashed chunk addr: 0x" + 
							Integer.toHexString(addr) + 
							" BAD CHECKSUM: " + Integer.toHexString(retData[5]) + 
							" vs. " + Integer.toHexString(chksum) + " - FAILED!");
				}
				else {
					logger.info("flashed chunk addr: 0x" + 
							Integer.toHexString(addr) + " CHECKSUM OK");
					chunksCompleted ++;
					pi.setPercentComplete((int)((double)chunksCompleted / (double)chunksTotal * 100));
				}
			}
			else {
				throw new ModuleUpdateException("flashed chunk addr: 0x" + Integer.toHexString(addr) + " NO RESPONSE - FAILED!");
			}
		}
		midi.closeMIDIPorts();
		logger.info("update complete.");
	}
}
