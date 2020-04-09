package com.kilpatrickaudio.kaUpdate.dfuUpdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.kilpatrickaudio.kaUpdate.main.ProgressIndicator;

/**
 * This class handles the DFU update process.
 * 
 * @author andrew
 *
 */
public class DfuUpdater {
	Logger logger;
	ProgressIndicator pi;
	enum OSType {
		MAC_OS,
		WINDOWS,
		LINUX,
		UNKNOWN
	};
	OSType osType;
	
	/**
	 * Creates a new DFU updater to handle running external dfu-util instance.
	 * 
	 * @param pi the ProgressIndicator display
	 */
	public DfuUpdater(ProgressIndicator pi) {
		logger = Logger.getLogger(this.getClass());
		this.pi = pi;
		
		logger.info("initializing DFU update system - detecting OS:");
		
		String osName = System.getProperty("os.name").toLowerCase();
		logger.info("  OS type: " + osName);
		if(osName.startsWith("mac os x")) {
			logger.info("  macOS detected - using embedded dfu-util");
			osType = OSType.MAC_OS;
		}
		else if(osName.startsWith("windows")) {
			logger.info("  Windows detected - using embedded dfu-util");
			osType = OSType.WINDOWS;
		}
		else if(osName.startsWith("linux")) {
			logger.info("  Linux detected - dfu-util must be available on system");
			osType = OSType.LINUX;
		}
		else {
			logger.info("  OS version could not be determined - DFU updates disabled");
			osType = OSType.UNKNOWN;
		}
	}
	
	/**
	 * Updates a DFU-enabled module.
	 * 
	 * @param dfuFilename the DFU filename to use
	 * @throws DfuUpdateException if there is a problem with the update
	 */
	public void updateModule(String dfuFilename) throws DfuUpdateException {
		logger.info("dfu file: " + dfuFilename);
		pi.setPercentComplete(0);
		String dfuUtilExecPath;
		switch(osType) {
		case LINUX:
			dfuUtilExecPath = "dfu-util";
			break;
		case MAC_OS:
/*			
			Class<?> noparams[] = {};
			try {
				Class<?> c = Class.forName("com.apple.eio.FileManager");
				Object obj = c.newInstance();
				Method m = c.getDeclaredMethod("getPathToApplicationBundle", noparams);
				dfuUtilExecPath = (String)m.invoke(obj);
			} catch (ClassNotFoundException e) {
				throw new DfuUpdateException("error getting DFU path: " + e.getMessage());
			} catch (InstantiationException e) {
				throw new DfuUpdateException("error getting DFU path: " + e.getMessage());
			} catch (IllegalAccessException e) {
				throw new DfuUpdateException("error getting DFU path: " + e.getMessage());
			} catch (NoSuchMethodException e) {
				throw new DfuUpdateException("error getting DFU path: " + e.getMessage());
			} catch (SecurityException e) {
				throw new DfuUpdateException("error getting DFU path: " + e.getMessage());
			} catch (IllegalArgumentException e) {
				throw new DfuUpdateException("error getting DFU path: " + e.getMessage());
			} catch (InvocationTargetException e) {
				throw new DfuUpdateException("error getting DFU path: " + e.getMessage());
			}
//			dfuUtilExecPath += "/Contents/Java/dfu-util";
*/
			dfuUtilExecPath = "/Applications/KAUpdate.app/Contents/Java/dfu-util";
			logger.info("dfuUtilExecPath: " + dfuUtilExecPath);
			break;
		case WINDOWS:
			// XXX set path
			dfuUtilExecPath = "dfu-util";
			break;
		case UNKNOWN:
		default:
			throw new DfuUpdateException("unsupported OS type: " + osType.name());
		}
		
		// XXX add the option for recipies
        ProcessBuilder pb = new ProcessBuilder(dfuUtilExecPath, "-a 0", "-D", dfuFilename);
        pb.redirectErrorStream(true);
        logger.info("running dfu-util...");
		Process p;
		try {
			p = pb.start();
		} catch(IOException e) {
			throw new DfuUpdateException("error running dfu-util: " + e.getMessage());
		}

		// capture output
		BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		try {
			while((line = bri.readLine()) != null) {
				logger.info(line);
				int percentPos = line.lastIndexOf('%');
				if(percentPos != -1 && percentPos >= 3) {
					String percentStr = line.substring(percentPos - 3, percentPos).trim();
					pi.setPercentComplete(Integer.parseInt(percentStr));
				}
			}
		} catch (IOException e) {
			// this will happen when the process dies
		}
		
		logger.info("update complete.");
	}
}
