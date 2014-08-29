package org.tno.nl.iotlab.protocol;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentCreator {

    private final static Logger logger = LoggerFactory.getLogger(ComponentCreator.class);

    private final ConfigurationAdmin configurationAdmin;

    // private List<VO> voList

    public ComponentCreator(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void createRD(String resourceId, String PID) {
        // create a configuration
        Configuration rdConfig;
        try {
            rdConfig = configurationAdmin.createFactoryConfiguration(PID, null);
            // set the properties
            Hashtable<String, Object> rdProps = new Hashtable<String, Object>();
            rdProps.put("resourceId", resourceId);
            rdProps.put("updateFrequency", 5);
            rdProps.put("powerWhenCloudy", 200);
            rdProps.put("powerWhenSunny", 1500);
            rdConfig.update(rdProps);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void createVO(String resourceId, String[] PID) {
        // create a configuration

        logger.info("Create new Virtual Object for resource id: " + resourceId);
        logger.info("Create new Virtual Object for pid: " + PID);

        for (String element : PID) {

            Configuration rmConfig;
            try {
                rmConfig = configurationAdmin.createFactoryConfiguration(element, null);
                // set the properties
                Hashtable<String, Object> rmProps = new Hashtable<String, Object>();
                rmProps.put("resourceId", resourceId);
                rmConfig.update(rmProps);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logger.debug("Error in vo config creation", e);
            }
        }
    }
}
