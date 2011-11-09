package org.unbiquitous.driver.temperature.eposmote2;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.NotifyException;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.driverManager.UosDriver;
import br.unb.unbiquitous.ubiquitos.uos.driverManager.UosEventDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.Notify;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class TemperatureDriver implements UosDriver,UosEventDriver {

	private static final Logger logger = Logger.getLogger(TemperatureDriver.class);
	
	private final UpDriver driver;
	private Gateway gateway;
	private String instanceId;
	private SerialCommunicator communicator;
	private Set<NetworkDevice> listeners = new HashSet<NetworkDevice>();
	private Integer lastTemp;
	private boolean running;

	public TemperatureDriver() {
		driver = new UpDriver("org.unbiquitous.driver.temperature");
		driver.addService("sense");
	}

	public void sense(ServiceCall req, ServiceResponse res,
			UOSMessageContext uosMessageContext) {
		Integer temperature;
		synchronized (communicator) {
			communicator.send('T');
			temperature = communicator.receive();
		}
		res.addParameter("temperature", temperature.toString());
	}

	public UpDriver getDriver() {
		return driver;
	}

	public void init(Gateway gateway, String instanceId) {
		this.gateway = gateway;
		this.instanceId = instanceId;
		this.communicator = new SerialCommunicator("/dev/ttyUSB0"); //TODO: Must receive as parameter
		communicator.open();
		running = true;
		Thread sensor = new Thread(){
			public void run() {
				while (running) {
					Integer temp;
					synchronized (communicator) {
						communicator.send('T');
						temp = communicator.receive();
					}
					if (temp != lastTemp) {
						lastTemp = temp;
						Notify temperatureChange = new Notify("temperature_change", driver.getName(),TemperatureDriver.this.instanceId);
						temperatureChange.addParameter("temperature", lastTemp.toString());
						notifyListeners(temperatureChange);
					}
				}
			}
			private void notifyListeners(Notify temperatureChange) {
				if (listeners != null){
					for (NetworkDevice nd : listeners) {
						UpDevice device = new UpDevice("DummyDevice");
						device.addNetworkInterface(nd.getNetworkDeviceName(),nd.getNetworkDeviceType());
						try {
							TemperatureDriver.this.gateway.sendEventNotify(temperatureChange, device);
						} catch (NotifyException e) {logger.error("Not posible to send event", e);}
					}
				}
			};
		};
		sensor.start();
	}

	public void destroy() {
		running = false;
		if (communicator != null)
			communicator.close();
	}

	public void setCommunicator(SerialCommunicator communicator) {
		this.communicator = communicator;
	}

	public void registerListener(ServiceCall serviceCall, ServiceResponse serviceResponse, UOSMessageContext messageContext) {
		listeners.add(messageContext.getCallerDevice());
	}

	public void unregisterListener(ServiceCall serviceCall,ServiceResponse serviceResponse, UOSMessageContext messageContext) {
		listeners.remove(messageContext.getCallerDevice());
	}

}
