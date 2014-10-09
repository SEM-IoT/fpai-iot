package org.tno.iotlab.humidity.vo;

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
import org.tno.iotlab.humidity.vo.HumidityVOImpl.Config;
import org.tno.iotlab.humidity.vo.HumidityVOImpl.HumVoState;
import org.tno.mqtt.connection.MqttConnection;
import org.tno.nl.iotlab.protocol.IoTProtocolDriver;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = VO.class, immediate = true)
public class HumidityVOImpl extends AbstractObservationProvider<HumVoState> implements VO, Runnable {

    private final static Logger logger = LoggerFactory.getLogger(HumidityVOImpl.class);

    final static class HumVoState {
        private final boolean isConnected;
        private final double humidity;
        private final String address;
        private final Date timestampComputer;

        HumVoState(String address, double humidity) {
            isConnected = false;
            this.address = address;
            this.humidity = humidity;
            timestampComputer = new Date();
        }

        public String getAddress() {
            return address;
        }

        public Date timestamp() {
            return timestampComputer;
        }

        public double getHumidity() {
            return humidity;
        }
    }

    @Meta.OCD
    interface Config {
        @Meta.AD(deflt = "TemperatuurVO")
        String resourceId();
    }

    private ScheduledFuture<?> scheduledFuture;
    private ServiceRegistration<?> observationProviderRegistration;
    private String address;
    private HumVoState latestState;
    private HumidityVOWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            address = config.resourceId();
            // Register with protocol driver
            driver.setvoList(this);
            logger.error("driver connected");

            widget = new HumidityVOWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);

            observationProviderRegistration = new ObservationProviderRegistrationHelper(this).observationType(HumVoState.class)
                                                                                             .observationOf(address)
                                                                                             .observedBy(address)
                                                                                             .register();
            scheduledFuture = schedulerService.scheduleAtFixedRate(this, 0, 1, java.util.concurrent.TimeUnit.MINUTES);

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

    @Override
    public void run() {
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
        String humidity = newData.substring(newData.indexOf("L") - 2, newData.indexOf("L"));
        logger.debug("Humidity: " + humidity);
        latestState = new HumVoState(address, Double.parseDouble(humidity));
        publish(new Observation<HumVoState>(timeService.getTime(), getState()));
        mqttService.publishMqtt(humidity, "test/humidity");
    }

    public HumVoState getState() {
        if (latestState != null) {
            return latestState;
        }
        return new HumVoState("-1", -999.0);
    }
}
