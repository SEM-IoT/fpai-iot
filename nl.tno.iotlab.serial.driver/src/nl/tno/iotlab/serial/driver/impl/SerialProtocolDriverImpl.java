package nl.tno.iotlab.serial.driver.impl;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nl.tno.iotlab.serial.driver.SerialProtocolDriver;

import org.osgi.framework.BundleContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = SerialProtocolDriverImpl.Config.class, provide = SerialProtocolDriver.class)
public class SerialProtocolDriverImpl implements SerialProtocolDriver {
    @Meta.OCD(description = "Serial protocol driver")
    public static interface Config {
        @Meta.AD(name = "Port name",
                 deflt = "COM9",
                 description = "Reference to the serial port on your computer (e.g. COM9 or /dev/ttyS0)")
        public String port();

        @Meta.AD(name = "Baudrate",
                 deflt = "57600",
                 description = "The baudrate of the serial port (e.g. 9600 or 57600)",
                 optionValues = { "110",
                                 "300",
                                 "600",
                                 "1200",
                                 "2400",
                                 "4800",
                                 "9600",
                                 "14400",
                                 "19200",
                                 "28800",
                                 "38400",
                                 "56000",
                                 "57600",
                                 "115200" })
        public int baudrate();

        @Meta.AD(name = "Databits",
                 deflt = "8",
                 description = "Number of databits of the serial port",
                 optionValues = { "5", "6", "7", "8" })
        public int databits();

        @Meta.AD(name = "Stopbits",
                 deflt = "1",
                 description = "Number of stopbits of the serial port",
                 optionLabels = { "1", "2", "1.5" },
                 optionValues = { "1", "2", "3" })
        public int stopbits();

        @Meta.AD(name = "Parity",
                 deflt = "1",
                 description = "Parity of the serial port",
                 optionLabels = { "None", "Odd", "Even", "Mark", "Space" },
                 optionValues = { "0", "1", "2", "3", "4" })
        public int parity();
    }

    private Config config;
    private BlockingQueue<String> queue;
    private SerialPortReader thread;

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {
        config = Configurable.createConfigurable(SerialProtocolDriverImpl.Config.class, properties);
        queue = new LinkedBlockingQueue<String>();
        thread = new SerialPortReader(config, queue);
        thread.start();
    }

    @Deactivate
    public void deactivate() {
        thread.stopRunning();
    }

    @Override
    public String pollMessage() {
        return queue.poll();
    }

    @Override
    public String readMessage() throws InterruptedException {
        return queue.take();
    }
}
