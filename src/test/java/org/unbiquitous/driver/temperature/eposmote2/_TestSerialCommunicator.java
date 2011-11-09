package org.unbiquitous.driver.temperature.eposmote2;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * This is an Integration test. For such to work you must have an EPOSMoteII deployed on your machine. 
 * 
 * @author Fabricio Buzeto
 *
 */
public class _TestSerialCommunicator {

	private static final String USB_PORT = "/dev/ttyUSB0";// LINUX USB PORT

	@Test public void shouldSendAndReceiveChars(){
		SerialCommunicator comm = new SerialCommunicator(USB_PORT); 
		comm.open();
		comm.send('T');
		Integer temp = comm.receive();
		assertNotNull("Should return a temperature",temp);
		comm.close();
	}
	
}
