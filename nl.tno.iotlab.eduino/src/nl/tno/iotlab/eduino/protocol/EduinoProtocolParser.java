package nl.tno.iotlab.eduino.protocol;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import nl.tno.iotlab.eduino.api.EduinoProtocolHandler;
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
        @Meta.AD(deflt = "(port=COM9)",
                 description = "Name of the serial port to read out the Eduino gateway (e.g. COM9 or /dev/ttyX)")
        public String port_filter();
    }

    private final AtomicBoolean running;

    public EduinoProtocolParser() {
        running = new AtomicBoolean();
    }

    private SerialProtocolDriver serialProtocolDriver;

    @Reference
    public void setSerialProtocolDriver(SerialProtocolDriver serialProtocolDriver) {
        this.serialProtocolDriver = serialProtocolDriver;
    }

    private TimeService timeService;

    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }

    private final Map<String, EduinoProtocolHandler> protocolHandlers = new HashMap<String, EduinoProtocolHandler>();

    @Reference(optional = true, dynamic = true, multiple = true)
    public void addEdwinoDriver(EduinoProtocolHandler protocolHandler, Map<String, Object> properties) {
        final String key = parse(properties);
        if (protocolHandlers.containsKey(key)) {
            log.warn("Found 2 protocol handler for the same type: {}", key);
        }
        protocolHandlers.put(key, protocolHandler);
    }

    public void removeEdwinoDriver(EduinoProtocolHandler protocolHandler, Map<String, Object> properties) {
        final String key = parse(properties);
        if (protocolHandlers.get(key) == protocolHandler) {
            protocolHandlers.remove(key);
        }
    }

    private String parse(Map<String, Object> properties) {
        return String.valueOf(properties.get("type"));
    }

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {
        running.set(true);
        new Thread(getClass().getName()) {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        final String line = serialProtocolDriver.readMessage();
                        if (line != null) {
                            final Date now = timeService.getTime();
                            final StringTokenizer tok = new StringTokenizer(line, " ");
                            if (tok.countTokens() > 2) {
                                final String timeSinceStart = tok.nextToken(":").trim();
                                try {
                                    Long.valueOf(timeSinceStart);
                                    final String protocolId = tok.nextToken();
                                    final EduinoProtocolHandler handler = getHandler(protocolId);
                                    if (handler != null) {
                                        handler.updateState(now, tok);
                                    }
                                } catch (final NumberFormatException ex) {
                                    // Ignore line
                                }
                            }
                        }
                    } catch (final InterruptedException ex) {
                        // Will probably be because we are shutting down...
                    }
                }
            }
        }.start();
    }

    @Deactivate
    public void deactivate() {
        running.set(false);
    }

    EduinoProtocolHandler getHandler(String type) {
        return protocolHandlers.get(type);
    }
}
