package org.tno.iotlab.light.vo;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ext.AbstractObservationProvider;
import org.flexiblepower.observation.ext.ObservationProviderRegistrationHelper;
import org.flexiblepower.time.TimeService;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.iotlab.common.vo.VO;
import org.tno.iotlab.common.vo.VOControlParameters;
import org.tno.iotlab.light.vo.LightVOImpl.Config;
import org.tno.iotlab.light.vo.LightVOImpl.LightVoState;
import org.tno.mqtt.connection.MqttConnection;
import org.tno.nl.iotlab.protocol.IoTProtocolDriver;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = VO.class, immediate = true)
public class LightVOImpl extends AbstractObservationProvider<LightVoState> implements VO {

    private final static Logger logger = LoggerFactory.getLogger(LightVOImpl.class);

    final static class LightVoState {
        private final boolean isConnected;
        private final double light;
        private final String address;
        private final Date timestampComputer;

        LightVoState(double light, String address) {
            isConnected = false;
            this.light = light;
            this.address = address;
            timestampComputer = new Date();
        }

        public double getLight() {
            return light;
        }

        public String getAddress() {
            return address;
        }

        public Date timestamp() {
            return timestampComputer;
        }
    }

    @Meta.OCD
    interface Config {
        @Meta.AD(deflt = "LightVO")
        String resourceId();
    }

    private ScheduledFuture<?> scheduledFuture;
    private ServiceRegistration<?> observationProviderRegistration;
    private String address;
    private LightVoState latestState;
    private LightVOWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            address = config.resourceId();
            // Register with protocol driver
            driver.setvoList(this);
            logger.error("driver connected");

            widget = new LightVOWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);

            observationProviderRegistration = new ObservationProviderRegistrationHelper(this).observationType(LightVoState.class)
                                                                                             .observationOf(address)
                                                                                             .observedBy(address)
                                                                                             .register();
        } catch (Throwable e) {
            System.out.println("!!!!_---------------");
        }
    }

    @Deactivate
    public void deactivate() {
        scheduledFuture.cancel(false);
        observationProviderRegistration.unregister();
    }

    private IoTProtocolDriver driver;

    @Reference
    public void setvoList(IoTProtocolDriver driver) {
        this.driver = driver;

    }

    private ScheduledExecutorService schedulerService;

    @Reference
    public void setSchedulerService(ScheduledExecutorService schedulerService) {
        this.schedulerService = schedulerService;
    }

    private TimeService timeService;

    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }

    public void setControlParameters(VOControlParameters resourceControlParameters) {
        // No ControlParameters
    }

    @Override
    public String getName() {
        return address;
    }

    @Override
    public String getAddress() {
        return address;
    }

    private MqttConnection mqttService;

    @Reference
    public void setMqttService(MqttConnection mqttService) {
        this.mqttService = mqttService;
    }

    @Override
    public void sendNewData(String newData) {
        String light = newData.substring(newData.indexOf("V") - 4, newData.indexOf("V"));
        logger.debug("Light (V) = " + light);

        Integer lightPercentage = (int) (100 - ((Double.parseDouble(light) / 3.3) * 100));

        latestState = new LightVoState(lightPercentage, address);
        publish(new Observation<LightVoState>(timeService.getTime(), getState()));
        mqttService.publishMqtt(lightPercentage.toString(), "test/light");
    }

    public LightVoState getState() {
        if (latestState != null) {
            return latestState;
        }
        return new LightVoState(-999.0, "-1");
    }
}
