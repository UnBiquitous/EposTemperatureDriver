#include <utility/ostream.h>
#include <uart.h>
#include <sensor.h>

__USING_SYS

/**
	This is a simple driver that listens to the UART for a temperature reading request
		denoted by a 'T' character and responds with the sensor data through the
		same channel.
*/
class UosUartTemperatureDriver {
	private:
		OStream cout;
		UART uart;
		Temperature_Sensor sensor;
	public:
		void run(){
			while(true){
				char c = uart.get();
				if (c == 'T'){
					int sample = sensor.sample();
					uart.put(sample);
				}
			}
		}
};

int main(){
	UosUartTemperatureDriver driver;
	driver.run();
	return 0;
}
