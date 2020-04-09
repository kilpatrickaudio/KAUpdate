package com.kilpatrickaudio.kaUpdate.moduleUpdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

/**
 * This class implements a parser for Intel HEX files used for firmware updates.
 * 
 * @author andrew
 *
 */
public class HexParser {
       
	/**
	 * Parses the hex file and returns the parsed instruction words.
	 * 
	 * @param hexFile the file to parse
	 * @return the instruction works, address and lengths
	 * 
	 * Return format:
	 * - for each record:
	 *   - byte 0-3: absolute 32 bit address
	 *   - byte 4-5: data length
	 *   - byte 6-n: data...
	 */
	public static int[] parseHexRecords(File hexFile) {
        Logger logger = Logger.getLogger(HexParser.class);
        boolean debug = false;
		int extAddr = 0;
		int prog[] = new int[(1024*1024)];  // a program can be up to 1MB long
		int progCount = 0;
		
		try {
			logger.info("starting to parse HEX file...");
			BufferedReader in = new BufferedReader(new FileReader(hexFile));
			String line = "";
			while((line = in.readLine()) != null) {
				line = line.trim();
				if(line.length() == 0) continue;  // skip blank lines
				if(debug) logger.debug("line: " + line);
				if(line.charAt(0) != ':') {
					in.close();
					throw new HexParseException("line didn't start with a colon: " + line);
				}
				line = line.substring(1);
				if(line.length() < 10) {
					in.close();
					throw new HexParseException("line is too short: " + line);
				}
				if((line.length() & 0x01) == 1) {
					in.close();
					throw new HexParseException("line is an odd length: " + line);
				}
				int lineWords = line.length() >> 1;
				int lineData[] = new int[lineWords];
				for(int i = 0; i < line.length(); i += 2) {
					int hexWord = Integer.parseInt(line.substring(i, i + 1), 16) << 4;
					hexWord |= Integer.parseInt(line.substring(i + 1, i + 2), 16);					
					lineData[i >> 1] = hexWord;
				}				
				int byteCount = lineData[0];
				int address = (lineData[1] << 8) | lineData[2];
				int recordType = lineData[3];
				int data[] = new int[byteCount];
				System.arraycopy(lineData, 4, data, 0, byteCount);
				int checksum = lineData[lineWords - 1];
				// print out the record
                if(debug) logger.debug("byte count: " + byteCount);
                if(debug) logger.debug("address: 0x" + Integer.toHexString(address));
                if(debug) logger.debug("recordType: " + recordType);
                if(debug) logger.debug("data:");
                if(debug) {
                    for(int i = 0; i < data.length; i ++) {
                        System.out.print("0x" + data[i] + " ");
                    }                	
                }
                if(debug) logger.debug("");
                if(debug) logger.debug("checksum: 0x" + Integer.toHexString(checksum));					
			
				// test the checksum
				int chk = 0;
				for(int i = 0; i < lineData.length - 1; i ++) {
					chk = (chk + lineData[i]) & 0xff;
				}
				chk = ((chk ^ 0xff) + 0x01) & 0xff;
				if(debug) logger.debug("calculated checksum: " + Integer.toHexString(chk));
				if(chk != checksum) {
					in.close();
					throw new HexParseException("checksum mismatch: 0x" + Integer.toHexString(checksum) +
							" - 0x" + Integer.toHexString(chk) + " - \n  line: " + line); 
				}
				//
				// parse the record types
				//
				// data record
				if(recordType == 0) {
					int absAddr = extAddr | address;
					// 32 bit address - big endian
					prog[progCount ++] = (absAddr >> 24) & 0xff;
					prog[progCount ++] = (absAddr >> 16) & 0xff;
					prog[progCount ++] = (absAddr >> 8) & 0xff;
					prog[progCount ++] = absAddr & 0xff;
					// data length - big endian
					prog[progCount ++] = (data.length >> 8);
					prog[progCount ++] = (data.length & 0xff);
					// data segment
					for(int i = 0; i < data.length; i ++) {
						prog[progCount ++] = data[i];
					}
					if(debug) logger.debug("DATA - absAddr: 0x" + Integer.toHexString(absAddr) + 
						" - data length: " + data.length);						
				}
				// end of file record
				else if(recordType == 1) {
					if(debug) logger.debug("END OF FILE");		
				}
				// extended segment adddress
				else if(recordType == 2) {
					extAddr = ((data[0] << 8 | data[1])) << 4;
					if(debug) logger.debug("EXTENDED SEGMENT ADDRESS - addr: 0x" + Integer.toHexString(extAddr));						
				}
				// start segment address
				else if(recordType == 3) {
					in.close();
					throw new HexParseException("START SEGMENT ADDRESS - record type not supported");
				}
				// extended linear address record
				else if(recordType == 4) {
					extAddr = ((data[0] << 8 | data[1]) << 16);
					if(debug) logger.debug("EXTENDED LINEAR ADDRESS - addr: 0x" + Integer.toHexString(extAddr));		
				}
				// start linear address
				else if(recordType == 5) {
					in.close();
					throw new HexParseException("START LINEAR ADDRESS - record type not supported");
				}
				// unknown record type
				else {
					in.close();
					throw new HexParseException("BAD RECORD TYPE - record type not supported: " + recordType);					
				}
				if(debug) logger.debug("");
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (HexParseException e) {
			e.printStackTrace();
			return null;
		}
		int retData[] = new int[progCount];
		System.arraycopy(prog, 0, retData, 0, progCount);
		logger.info("parsing done.");
		return retData;
	}
	
	/**
	 * Creates data chunkified into n byte chunks.
	 * 
	 * @param prog the program data
	 * @param chunksize the length of each desired chunk in bytes
	 * @return a LinkedHashMap of addresses and chunks
	 */
	public static LinkedHashMap<Integer,FlashChunk> creatChunkyData(int prog[], int chunkSize) {
        Logger logger = Logger.getLogger(HexParser.class);
        boolean debug = false;
		LinkedHashMap<Integer,FlashChunk> flashMap;
		
		// pack the program into chunkSize sized chunks
		logger.debug("chunkifying data into " + chunkSize + " byte chunks...");
		flashMap = new LinkedHashMap<Integer, FlashChunk>();		
		
		// traverse the entire program
		for(int i = 0; i < prog.length;) {
			// address - first 4 bytes, big endian
			int addr = prog[i++] << 24;
			addr |= (prog[i++] << 16);
			addr |= (prog[i++] << 8);
			addr |= prog[i++];
			// segment length - 2 bytes, big endian
			int len = (prog[i++] << 8) | (prog[i++]);
			// data
			int data[] = new int[len];
			if(len > 0) {
				System.arraycopy(prog, i, data, 0, len);
				i += len;
			}
			if(debug) logger.debug("addr: 0x" + Integer.toHexString(addr) + 
				" - len: 0x" + Integer.toHexString(len));
//				// print hex digits
//				for(int j = 0; j < data.length; j ++) {
//					System.out.print("0x" + data[j]);
//				}
//				logger.debug("\n")
			
			int chunkAddr = addr & (~(chunkSize - 1));
			int offset = addr & (chunkSize - 1);
			FlashChunk chunk = null;
			// if the chunk exists, get it
			if(flashMap.containsKey(new Integer(chunkAddr))) {
				if(debug) logger.debug("chunk exists: 0x" + Integer.toHexString(chunkAddr) +
					" - offset: 0x" + Integer.toHexString(offset));
				chunk = flashMap.get(new Integer(chunkAddr));
			}
			// create a new chunk
			else {
				if(debug) logger.debug("chunk is new: 0x" + Integer.toHexString(chunkAddr) +
					" - offset: 0x" + Integer.toHexString(offset));
				chunk = new FlashChunk(chunkAddr, chunkSize);
				flashMap.put(new Integer(chunkAddr), chunk);
			}
			try {
				// add the data to the chunk
				int remain = chunk.addData(offset, data);
				
				// we have data that crosses a chunk boundary
				if(remain != 0) {
					if(debug) logger.debug("REMAIN: data remaining to add to new chunk: " + remain + " bytes");						
					int newChunkAddr = chunkAddr + chunkSize;
//					logger.debug("newChunkAddr: " + Integer.toHexString(newChunkAddr));
					// create a new chunk to handle the remaining data
					chunk = new FlashChunk(newChunkAddr, chunkSize);
					flashMap.put(new Integer(newChunkAddr), chunk);
					int remainData[] = new int[remain];
					System.arraycopy(data, data.length - remain, remainData, 0, remain);
					chunk.addData(0, remainData);
				}
			} catch (FlashChunkException e) {
				e.printStackTrace();
				return null;
			}			
		}			
		logger.debug("chunkifying done.");
		return flashMap;
	}
	
}
