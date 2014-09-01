package nl.tno.iotlab.eduino.protocol;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import nl.tno.iotlab.eduino.driver.EduinoDriver;
import nl.tno.iotlab.eduino.protocol.EduinoProtocolParser.Config;
import nl.tno.iotlab.serial.driver.SerialProtocolDriver;

import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, immediate = true)
public class EduinoProtocolParser {
    private static final Logger log = LoggerFactory.getLogger(EduinoProtocolParser.class);

    @Meta.OCD(description = "Protocol parser for the Edwino gateway through a serial connection")
    public static interface Config {
        @Meta.AD(deflt = "(port=COM9)", description = "Target filter to select the correct serial port")
        public String port_filter();
    }

    private final AtomicBoolean running;

    public EduinoProtocolParser() {
        running = new AtomicBoolean();
    }

    private SerialProtocolDriver serialPort;

    @Reference
    public void setSerialProtocolDriver(SerialProtocolDriver serialPort) {
        this.serialPort = serialPort;
    }

    private TimeService timeService;

    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }

    private final Map<String, EduinoDriver> drivers = new HashMap<String, EduinoDriver>();

    @Reference(optional = true, dynamic = true, multiple = true)
    public void addEdwinoDriver(EduinoDriver driver, Map<String, Object> properties) {
        final String key = parse(properties);
        if (drivers.containsKey(key)) {
            log.warn("Found 2 drivers on the same address: {}", key);
        }
        drivers.put(key, driver);
    }

    public void removeEdwinoDriver(EduinoDriver driver, Map<String, Object> properties) {
        final String key = parse(properties);
        if (drivers.get(key) == driver) {
            drivers.remove(key);
        }
    }

    private String parse(Map<String, Object> properties) {
        return String.valueOf(properties.get("address"));
    }

    @Activate
    public void activate(BundleContext context) {
        running.set(true);
        new Thread(getClass().getName()) {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        final String line = serialPort.readMessage();
                        if (line != null) {
                            final Date now = timeService.getTime();

                            // 64368: APP Receiving type-S 24.6C 0L 0PIR 0.00V 0lost from 02...
                            final String[] parts = line.split(" ");
                            if (parts.length == 11 && parts[3].equals("type-S")) {
                                // For now we just support the S type
                                final String address = parts[11].substring(0, 2);

                                final EduinoDriver driver = getDriver(address);
                                if (driver != null) {
                                    driver.update(now, parts);
                                }
                            }
                        }
                    } catch (final InterruptedException e) {
                    }
                }
            }

        }.start();
    }

    EduinoDriver getDriver(String address) {
        // TODO: auto-create a new driver for new addresses?
        return drivers.get(address);
    }

    @Deactivate
    public void deactivate() {
        running.set(false);
    }
}
