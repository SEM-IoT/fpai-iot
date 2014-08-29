package org.tno.serial.driver;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.nl.iotlab.protocol.IoTProtocolDriver;
import org.tno.serial.driver.SerialDriverImpl.Config;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, immediate = true)
public class SerialDriverImpl {

    private final static Logger logger = LoggerFactory.getLogger(SerialDriverImpl.class);

    Thread serialThread;

    @Meta.OCD
    interface Config {
        @Meta.AD(deflt = "comport driver")
        String resourceId();

        @Meta.AD(deflt = "/dev/tty.SLAB_USBtoUART")
        String getcomport();
    }

    private Config config;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            serialThread = new Thread(new SerialDriver(config.getcomport(), this), "Serial Driver Thread");
            serialThread.start();
            logger.debug("Serial Driver Activated");

        } catch (Throwable e) {
            logger.error("Couldnt start serial driver");
        }
    }

    @Deactivate
    public void deactivate() {
    }

    public void receivedData(String data) {
        logger.debug("Driver Impl ontvangt: " + data);
        voLifeCycleManger.newDataReceived(data);
    }

    private IoTProtocolDriver voLifeCycleManger;

    @Reference
    public void setIotProtocolDriver(IoTProtocolDriver voLifeCycleManager) {
        voLifeCycleManger = voLifeCycleManager;
    }
}
