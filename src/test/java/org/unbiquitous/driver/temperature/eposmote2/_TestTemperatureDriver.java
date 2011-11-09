package org.unbiquitous.driver.temperature.eposmote2;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.NotifyException;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.Notify;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class _TestTemperatureDriver {

	TemperatureDriver driver;
	
	@Before public void setUp(){
		driver = new TemperatureDriver();
	}
	
	@After public void tearDown(){
		driver.destroy();
	}
	
	private ServiceResponse callSense() {
		ServiceCall req = new ServiceCall("org.unbiquitous.driver.temperature", "sense");
		ServiceResponse res = new ServiceResponse();
		driver.sense(req, res, (UOSMessageContext) null);
		return res;
	}
	
	private static class DummyCommunicator extends SerialCommunicator{
		public DummyCommunicator() {super(null);}
		public DummyCommunicator(int value) {
			this();
			this.value = value;
		}
		int value = 0;
		boolean send = false;
		boolean open = false;
		public void open() {open = true;}
		public void close() {open = false;}
		public void send(char c) {send = c == 'T';}
		public Integer receive() {
			if (open && send){
				send = false;
				return value;
			}
			return 0;
		}
	}
	
	@Test public void shouldGetTheTemperatureFromSerial(){
		driver.setCommunicator(new DummyCommunicator(45));
		ServiceResponse res = callSense();
		assertEquals("45", res.getResponseData().get("temperature"));
	}

	
	@Test public void shouldGetTheTemperatureFromSerialAfterATemperatureRequest(){
		driver.setCommunicator(new DummyCommunicator(23));
		ServiceResponse res = callSense();
		assertEquals("23", res.getResponseData().get("temperature"));
	}
	
	@Test public void shouldInformProperDriverSettings(){
		UpDriver uDriver = driver.getDriver();
		assertNotNull("Should have a driver",uDriver);
		assertEquals("org.unbiquitous.driver.temperature",uDriver.getName());
		assertEquals(1,uDriver.getServices().size());
		assertEquals("sense",uDriver.getServices().get(0).getName());
	}
	
	@Test public void shouldInformAListenerAboutChanges() throws NotifyException, InterruptedException{
		Gateway gateway = mock(Gateway.class);
		DummyCommunicator comm = new DummyCommunicator(42);
		
		// Init gateway
		driver.setCommunicator(comm);
		driver.init(gateway, "me");
		
		//Mock context
		UOSMessageContext ctx = mock(UOSMessageContext.class);
		when(ctx.getCallerDevice()).thenReturn(mock(NetworkDevice.class));
		
		//Register for the event
		ServiceCall call = new ServiceCall(null, "registerListener");
		call.addParameter("event", "temperature_change");
		driver.registerListener(call, new ServiceResponse(), ctx);
		
		Thread.sleep(300); // wait to receive the first notification
		verify(gateway).sendEventNotify(argThat(new BaseMatcher<Notify>() {
			public boolean matches(Object arg0) {
				Notify not = (Notify) arg0;
				return not.getParameter("temperature").equals("42");
			}

			public void describeTo(Description arg0) {}
		}), any(UpDevice.class));
	}
	
}
