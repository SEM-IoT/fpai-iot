package org.tno.iotlab.temperatuur.vo;

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
import org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl.Config;
import org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl.TempVoState;
import org.tno.mqtt.connection.MqttConnection;
import org.tno.nl.iotlab.protocol.IoTProtocolDriver;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = VO.class, immediate = true)
public class TemperatuurVOImpl extends AbstractObservationProvider<TempVoState> implements VO, Runnable {

    private final static Logger logger = LoggerFactory.getLogger(TemperatuurVOImpl.class);

    final static class TempVoState {
        private final boolean isConnected;
        private final double temperature;
        private final String address;
        private final Date timestampComputer;

        TempVoState(double temperature, String address) {
            isConnected = false;
            this.temperature = temperature;
            this.address = address;
            timestampComputer = new Date();
        }

        public double getTemperarture() {
            return temperature;
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
        @Meta.AD(deflt = "TemperatuurVO")
        String resourceId();
    }

    private ScheduledFuture<?> scheduledFuture;
    private ServiceRegistration<?> observationProviderRegistration;
    private String address;
    private TempVoState latestState;
    private TemperatureVOWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            address = config.resourceId();
            // Register with protocol driver
            driver.setvoList(this);
            logger.error("driver connected");

            widget = new TemperatureVOWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);

            observationProviderRegistration = new ObservationProviderRegistrationHelper(this).observationType(TempVoState.class)
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

    private MqttConnection mqttService;

    @Reference
    public void setMqttService(MqttConnection mqttService) {
        this.mqttService = mqttService;
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

    @Override
    public void sendNewData(String newData) {
        String temperature = newData.substring(newData.indexOf("C") - 4, newData.indexOf("C"));
        logger.debug("Temperature = " + temperature);
        latestState = new TempVoState(Double.parseDouble(temperature), address);
        publish(new Observation<TempVoState>(timeService.getTime(), getState()));
        mqttService.publishMqtt(temperature, "test/temperature");
    }

    public TempVoState getState() {
        if (latestState != null) {
            return latestState;
        }
        return new TempVoState(-999.0, "-1");
    }
}
