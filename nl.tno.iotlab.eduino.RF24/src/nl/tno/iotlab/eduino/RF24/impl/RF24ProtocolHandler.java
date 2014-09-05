package nl.tno.iotlab.eduino.RF24.impl;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import nl.tno.iotlab.eduino.RF24.RF24Driver;
import nl.tno.iotlab.eduino.api.EduinoProtocolHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class RF24ProtocolHandler implements EduinoProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(RF24ProtocolHandler.class);

    private final Map<String, RF24Driver> drivers = new HashMap<String, RF24Driver>();

    @Reference(optional = true, dynamic = true, multiple = true)
    public void addRF24Driver(RF24Driver driver, Map<String, Object> properties) {
        final String key = parseR434DriverKey(properties);
        if (drivers.containsKey(key)) {
            log.warn("Found a second driver with the same key {}", key);
        }
        drivers.put(key, driver);
    }

    public void removeRF24Driver(RF24Driver driver, Map<String, Object> properties) {
        final String key = parseR434DriverKey(properties);
        if (drivers.get(key) == driver) {
            drivers.remove(key);
        }
    }

    private String parseR434DriverKey(Map<String, Object> properties) {
        return String.valueOf(properties.get("address"));
    }

    @Override
    public void updateState(Date time, StringTokenizer tokenizer) {
        if (tokenizer.countTokens() > 1) {
            final String address = tokenizer.nextToken();
            final RF24Driver driver = drivers.get(address);

            if (driver != null) {
                final ByteBuffer buffer = ByteBuffer.allocate(tokenizer.countTokens());
                while (tokenizer.hasMoreElements()) {
                    final String value = tokenizer.nextToken();
                    try {
                        final int x = Integer.valueOf(value);
                        if (x > 255 || x < 0) {
                            buffer.put((byte) 0);
                        } else {
                            buffer.put((byte) x);
                        }
                    } catch (final NumberFormatException ex) {
                        buffer.put((byte) 0);
                    }
                }

                buffer.flip();
                driver.updateState(buffer);
            }
        }
    }
}
