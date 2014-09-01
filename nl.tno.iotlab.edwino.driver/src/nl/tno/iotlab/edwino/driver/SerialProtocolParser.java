package nl.tno.iotlab.edwino.driver;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import nl.tno.iotlab.edwino.driver.SerialProtocolParser.Config;
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
public class SerialProtocolParser {
    private static final Logger log = LoggerFactory.getLogger(SerialProtocolParser.class);

    @Meta.OCD(description = "Protocol parser for the Edwino gateway through a serial connection")
    public static interface Config {
        @Meta.AD(deflt = "(port=COM9)", description = "Target filter to select the correct serial port")
        public String port_filter();
    }

    private final AtomicBoolean running;

    public SerialProtocolParser() {
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

    private final Map<String, EdwinoDriver> drivers = new HashMap<String, EdwinoDriver>();

    @Reference(optional = true, dynamic = true, multiple = true)
    public void addEdwinoDriver(EdwinoDriver driver, Map<String, Object> properties) {
        final String key = parse(properties);
        if (drivers.containsKey(key)) {
            log.warn("Found 2 drivers on the same address: {}", key);
        }
        drivers.put(key, driver);
    }

    public void removeEdwinoDriver(EdwinoDriver driver, Map<String, Object> properties) {
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

                            // TODO: do some parsing, for now some dummy data
                            final String address = "01"; // TODO
                            final Measurable<Temperature> temp1 = Measure.valueOf(20.4, SI.CELSIUS);
                            final Measurable<Temperature> temp2 = Measure.valueOf(9.8, SI.CELSIUS);
                            final Measurable<Dimensionless> humidity = Measure.valueOf(62, NonSI.PERCENT);
                            final Measurable<Dimensionless> light = Measure.valueOf(2, NonSI.PERCENT);

                            final EdwinoDriver edwinoDriver = drivers.get(address);
                            if (edwinoDriver != null) {
                                edwinoDriver.update(now, temp1, temp2, humidity, light);
                            }
                        }
                    } catch (final InterruptedException e) {
                    }
                }
            }
        }.start();
    }

    @Deactivate
    public void deactivate() {
        running.set(false);
    }
}
