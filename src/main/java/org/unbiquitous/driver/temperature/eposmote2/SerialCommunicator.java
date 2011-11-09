package org.unbiquitous.driver.temperature.eposmote2;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.util.Enumeration;

public class SerialCommunicator {

	private String portName;
	private int timeout;
	private int baudRate;
	private int dataBits;
	private int stopbits;
	private int parity;

	private SerialPort port;
	
	public SerialCommunicator(String portName) {
		timeout = 5000;
		baudRate = 9600;
		dataBits = SerialPort.DATABITS_8;
		stopbits = SerialPort.STOPBITS_1;
		parity = SerialPort.PARITY_NONE;
		this.portName = portName;
	}
	
	public void open() {
		port = null;
		Enumeration<CommPortIdentifier> pList = CommPortIdentifier.getPortIdentifiers();
		while (pList.hasMoreElements()) {
			CommPortIdentifier cpi = (CommPortIdentifier) pList.nextElement();
			if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL &&
					cpi.getName().equals(portName)) {
				try {
					port = (SerialPort) cpi.open("conn", timeout);
					port.setSerialPortParams(baudRate, dataBits, stopbits, parity);
				} catch (Exception e) { throw new RuntimeException(e);}
			}
		}
	}

	public void send(char c) {
		try {
			port.getOutputStream().write(c);
		} catch (Exception e) { throw new RuntimeException(e);}
	}

	public Integer receive() {
		try {
			return port.getInputStream().read();
		} catch (Exception e) { throw new RuntimeException(e);}
	}

	public void close() {
		port.close();
		port = null;
	}

}
