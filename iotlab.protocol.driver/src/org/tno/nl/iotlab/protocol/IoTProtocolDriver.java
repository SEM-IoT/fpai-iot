package org.tno.nl.iotlab.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.iotlab.common.vo.VO;
import org.tno.nl.iotlab.protocol.IoTProtocolDriver.Config;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(provide = IoTProtocolDriver.class, immediate = true, designateFactory = Config.class)
public class IoTProtocolDriver implements IoTProtocol, Runnable {

    private final static Logger logger = LoggerFactory.getLogger(IoTProtocolDriver.class);
    private ScheduledFuture<?> scheduledFuture;
    private DataDecoder dataDecoder;
    private Config config;

    @Meta.OCD
    interface Config {
        @Meta.AD(deflt = "01,02,03")
        List<String> addresses();

        @Meta.AD(deflt = "org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl-org.tno.iotlab.temperatuur.vo.HumidityVOImpl-org.tno.iotlab.light.vo.LightVOImpl, " + "       org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl-org.tno.iotlab.temperatuur.vo.HumidityVOImpl-org.tno.iotlab.moist.vo.MoistVOImpl,,"
                + "       org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl-org.tno.iotlab.temperatuur.vo.HumidityVOImpl-org.tno.iotlab.moist.vo.MoistVOImpl")
        List<String>
        pids();
    }

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {

        Config config = Configurable.createConfigurable(Config.class, properties);

        try {
            if (configurationAdmin != null) {
                dataDecoder = new DataDecoder(configurationAdmin, config.addresses(), config.pids(), this);
            }
            scheduledFuture = schedulerService.scheduleAtFixedRate(this, 0, 30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void run() {
    }

    public void newDataReceived(String id) {
        dataDecoder.newData(id);
    }

    // URL base for appliance operations
    private String applianceUrlBase;
    private TimeService timeService;

    @Override
    public String readSingleValue(String address) {
        return null;
    }

    private ScheduledExecutorService schedulerService;

    @Reference
    public void setSchedulerService(ScheduledExecutorService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }

    private final List<VO> voList = new ArrayList<VO>();

    public void setvoList(VO newVO) {

        boolean unique = true;
        for (VO vo : voList) {
            if (vo.getName().equals(newVO.getName())) {
                unique = false;
            }
        }

        // if (unique) {
        voList.add(newVO);
        logger.error("new VO added to list" + newVO.getName());
        // }

        logger.debug("The list of vo's has size: " + voList.size());
    }

    public List<VO> getVoList() {
        return voList;
    }

    @Override
    public String toString() {
        return "IoT lab protocol driver";
    }

    @Override
    public String[] readValues(String[] addresses) {
        return null;
    }

    private ConfigurationAdmin configurationAdmin;

    @Reference
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

}
