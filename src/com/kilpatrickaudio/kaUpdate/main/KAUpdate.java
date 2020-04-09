package com.kilpatrickaudio.kaUpdate.main;

import java.util.Properties;

import javax.sound.midi.MidiUnavailableException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.kilpatrickaudio.kaUpdate.dfuUpdate.DfuUpdateException;
import com.kilpatrickaudio.kaUpdate.dfuUpdate.DfuUpdater;
import com.kilpatrickaudio.kaUpdate.main.KAUpdateGUI.UpdateMode;
import com.kilpatrickaudio.kaUpdate.midi.MIDIHandler;
import com.kilpatrickaudio.kaUpdate.moduleUpdate.ModuleUpdateException;
import com.kilpatrickaudio.kaUpdate.moduleUpdatePIC18.ModuleUpdatePIC18;
import com.kilpatrickaudio.kaUpdate.moduleUpdatePIC32.ModuleUpdatePIC32;

/**
 * This class implements the top level of the KAUpdate program.
 * 
 * @author andrew
 *
 */
public class KAUpdate implements UpdateControlHandler {
    Logger logger;
    KAUpdateGUI gui;
    MIDIHandler midi;
    DfuUpdater dfuUpdater;
    String midiInDevName;
    String midiOutDevName;
    String updateFilename = null;
    enum UpdateState { 
    	IDLE, 
    	START_MIDI, 
    	UPDATING_MIDI, 
    	START_DFU, 
    	UPDATING_DFU,
    	DONE 
    };
    UpdateState updateState = UpdateState.IDLE;
    boolean running = true;
    // program version
    String versionString = "1.08";
    
	/**
	 * Main KAUpdate launcher.
	 *
	 * @param args command line args
	 * @throws MidiUnavailableException 
	 */
	public KAUpdate(String args[]) throws MidiUnavailableException {
        logger = Logger.getLogger(this.getClass());
        BasicConfigurator.configure();
        midi = new MIDIHandler();

		// gui mode
		gui = new KAUpdateGUI(this, midi, versionString);
        gui.setVisible(true);
        
        // display java version for debugging
        String[] pro = { "java.version", "java.vm.version", "java.runtime.version" };
        Properties properties = System.getProperties();
        for (int i = 0; i < pro.length; i++) {
        	logger.info(pro[i] + " " + properties.getProperty(pro[i]));
        }

        // set up the DFU stuff
        dfuUpdater = new DfuUpdater(gui);
        
        try {
            // parse command line args
            if(args.length > 0) {
            	String mode = args[0].toUpperCase();
            	// MIDI mode
            	if(mode.equals("MIDI")) {
            		if(args.length < 4) {
            			throw new IllegalArgumentException("MIDI command line args invalid - " +
        					"usage: MIDI midiInPort midiOutPort updateFilename");
            		}
            		logger.info("command line set mode to: MIDI");
            		gui.setUpdateMode(UpdateMode.MIDI);
            		gui.setMidiInPort(args[1]);
            		gui.setMidiOutPort(args[2]);
            		gui.setUpdateFilename(args[3]);
            	}
            	// DFU mode
            	else if(mode.equals("DFU")) {
            		if(args.length < 2) {
            			throw new IllegalArgumentException("DFU command line args invalid - " +
        					"usage: DFU updateFilename");
            		}
            		logger.info("command line set mode to: DFU");
            		gui.setUpdateMode(UpdateMode.DFU);
            		gui.setUpdateFilename(args[1]);
            	}
            	else {
            		throw new IllegalArgumentException("command line mode unknown: " + mode);
            	}
            }
            
            // run the main loop
            run();
            midi.closeMIDIPorts();            
        } catch(IllegalArgumentException e) {
        	logger.error("error: " + e.getMessage());
        }
        
        logger.info("exiting.");
        System.exit(0);
	}
     
	public void run() {
		logger.info("To update your module:");
		logger.info(" See module-specific instructions at: www.kilpatrickaudio.com");
		logger.info(" Select the update type based on the product:");
		logger.info("  - DFU mode - CARBON, etc.");
		logger.info("  - MIDI mode: PHENOL, K4815, K2579, etc.");
		logger.info(" For DFU mode:  select update file");
		logger.info(" For MIDI mode: select MIDI ports and update file");
		logger.info(" Press Update to start the process");
			
        // main loop    
        while(running) {
        	try {
        		// start the update process for MIDI
        		if(updateState == UpdateState.START_MIDI) {
        			updateMIDI();
        		}
        		else if(updateState == UpdateState.START_DFU) {
        			updateDfu();
        		}
        		// stop the update progress
        		else if(updateState == UpdateState.DONE) {
        			gui.setControlsEnabled(true);
        			updateState = UpdateState.IDLE;
        		}
				Thread.sleep(50);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				updateState = UpdateState.DONE;
			} catch (ModuleUpdateException e) {
				logger.error(e.getMessage());				
				updateState = UpdateState.DONE;
			} catch (DfuUpdateException e) {
				logger.error(e.getMessage());				
				updateState = UpdateState.DONE;
			}
        }
	}
	
	/**
	 * Perform a MIDI update.
	 * 
	 * @throws ModuleUpdateException if there is an error updating the module 
	 */
	private void updateMIDI() throws ModuleUpdateException {
		gui.setControlsEnabled(false);
		updateState = UpdateState.UPDATING_MIDI;        				
		if(updateFilename.length() < 6) {
			throw new ModuleUpdateException("hex filename is invalid: " + updateFilename);
		}
		String suffix = updateFilename.substring(updateFilename.length() - 6, updateFilename.length());
		suffix = suffix.toUpperCase();
		logger.debug("suffix: " + suffix);
		if(!suffix.startsWith("HEX")) {
			throw new ModuleUpdateException("hex filename is invalid: " + updateFilename);
		}
		int devID = Integer.parseInt(suffix.substring(3, 5), 16);
		logger.info("deviceID: " + Integer.toHexString(devID));
		String chipType = suffix.substring(5, 6);
		if(chipType.equals("A")) {
			logger.info("chip type: PIC18F4520");
			ModuleUpdatePIC18 mu = new ModuleUpdatePIC18(midi, midiInDevName, midiOutDevName, 
					updateFilename, devID);
			try {
				mu.updateModule(gui);
			} catch (ModuleUpdateException e) {
				logger.error(e.getMessage());
				logger.error("updated FAILED!");
				gui.setPercentComplete(0);
				midi.closeMIDIPorts();  // for good measure
			}
			updateState = UpdateState.DONE;
		}
		else if(chipType.equals("B") || chipType.equals("C")) {
			logger.info("chip type: PIC32MX");
			updateState = UpdateState.DONE;
			ModuleUpdatePIC32 mu = new ModuleUpdatePIC32(midi, midiInDevName, midiOutDevName, 
					updateFilename, devID);
			try {
				mu.updateModule(gui);
			} catch(ModuleUpdateException e) {
				logger.error(e.getMessage());
				logger.error("update FAILED!");
				gui.setPercentComplete(0);
				midi.closeMIDIPorts();  // for good measure
			}
			updateState = UpdateState.DONE;
		}
		else {
			throw new ModuleUpdateException("unknown chip type: " + chipType);
		}		
	}
	
	/**
	 * Performs a DFU update.
	 * 
	 * @throws DfuUpdateException if there is an error updating the module
	 */
	private void updateDfu() throws DfuUpdateException {
		gui.setControlsEnabled(false);
		updateState = UpdateState.UPDATING_DFU;
		// start the update
		dfuUpdater.updateModule(updateFilename);
		updateState = UpdateState.DONE;
	}
	
	@Override
	public void performMIDIUpdate(String midiInDevName, String midiOutDevName, String hexFilename) {
		this.midiInDevName = midiInDevName;
		this.midiOutDevName = midiOutDevName;
		this.updateFilename = hexFilename;
		logger.debug("triggering update - inDev: " + midiInDevName + 
				" - outDev: " + midiOutDevName + " - hexFilename: " + hexFilename);
		updateState = UpdateState.START_MIDI;
	}

	@Override
	public void performDfuUpdate(String dfuFilename) {
		this.updateFilename = dfuFilename;
		logger.debug("triggering update - dfuFilename: " + dfuFilename);
		updateState = UpdateState.START_DFU;
	}
	
	@Override
	public void closeApplication() {
		running = false;
	}
	
	/**
	 * Main!
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		try {
			new KAUpdate(args);
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
	}


}
