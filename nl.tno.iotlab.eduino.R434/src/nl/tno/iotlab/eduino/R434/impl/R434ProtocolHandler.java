package nl.tno.iotlab.eduino.R434.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import nl.tno.iotlab.eduino.R434.R434Driver;
import nl.tno.iotlab.eduino.api.EduinoProtocolHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class R434ProtocolHandler implements EduinoProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(R434ProtocolHandler.class);

    private final Map<String, R434Driver> drivers = new HashMap<String, R434Driver>();

    @Reference(optional = true, dynamic = true, multiple = true)
    public void addR434Driver(R434Driver driver, Map<String, Object> properties) {
        final String key = parseR434DriverKey(properties);
        if (drivers.containsKey(key)) {
            log.warn("Found a second driver with the same key {}", key);
        }
        drivers.put(key, driver);
    }

    public void removeR434Driver(R434Driver driver, Map<String, Object> properties) {
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
            String address = tokenizer.nextToken();
            R434Driver driver = drivers.get(address);

            if (driver != null) {
                List<String> state = new ArrayList<String>(tokenizer.countTokens());
                while (tokenizer.hasMoreElements()) {
                    state.add(tokenizer.nextToken());
                }
                driver.updateState(state);
            }
        }
    }
}
