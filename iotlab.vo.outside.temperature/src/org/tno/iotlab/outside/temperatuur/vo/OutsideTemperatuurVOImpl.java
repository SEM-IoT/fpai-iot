package org.tno.iotlab.outside.temperatuur.vo;

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
import org.tno.iotlab.outside.temperatuur.vo.OutsideTemperatuurVOImpl.Config;
import org.tno.iotlab.outside.temperatuur.vo.OutsideTemperatuurVOImpl.VoState;
import org.tno.mqtt.connection.MqttConnection;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = VO.class, immediate = true)
public class OutsideTemperatuurVOImpl extends AbstractObservationProvider<VoState> implements VO, Runnable {

    private final static Logger logger = LoggerFactory.getLogger(OutsideTemperatuurVOImpl.class);

    final static class VoState {
        private final boolean isConnected;
        private final double temperature;
        private final String address;
        private final Date timestampComputer;

        VoState(double temperature, String address, Date timeStampMeasurement) {
            isConnected = false;
            this.temperature = temperature;
            this.address = address;
            timestampComputer = timeStampMeasurement;
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
        @Meta.AD(deflt = "OutsideTemperatuurVO")
        String resourceId();
    }

    private ScheduledFuture<?> scheduledFuture;
    private ServiceRegistration<?> observationProviderRegistration;
    private String address;
    private VoState latestState;
    private OutsideTemperatureVOWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;
    private final OutsideTemperatureRetriever outsideTemperatureRetriever = new OutsideTemperatureRetriever();

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            address = config.resourceId();

            widget = new OutsideTemperatureVOWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);

            observationProviderRegistration = new ObservationProviderRegistrationHelper(this).observationType(VoState.class)
                                                                                             .observationOf(address)
                                                                                             .observedBy(address)
                                                                                             .register();
            scheduledFuture = schedulerService.scheduleAtFixedRate(this, 0, 20, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Throwable e) {
            System.out.println("!!!!_---------------");
        }
    }

    @Deactivate
    public void deactivate() {
        scheduledFuture.cancel(false);
        observationProviderRegistration.unregister();
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
        logger.info("Fetchig new data");
        Map<Date, Double> temperatureList = outsideTemperatureRetriever.retreiveData();
        logger.debug(temperatureList.toString());
        Date now = new Date();
        for (Map.Entry<Date, Double> entry : temperatureList.entrySet()) {
            if (true) {
                // if (entry.getKey().getHours() == 21 && entry.getKey().getDay() == now.getDay() + 1) {
                logger.info(entry.getKey() + "   " + entry.getValue());
                publishNewData(entry.getValue(), entry.getKey());
                break;
            }
        }
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

    public void publishNewData(Double temperature, Date date) {
        latestState = new VoState(temperature, address, date);
        publish(new Observation<VoState>(timeService.getTime(), getState()));
        logger.info("Observation published: " + getState().temperature);
        mqttService.publishMqtt(temperature.toString(), "test/outsideTemp");
    }

    public VoState getState() {
        if (latestState != null) {
            return latestState;
        }
        return new VoState(-999.0, "-1", new Date());
    }

    @Override
    public void sendNewData(String newData) {
        // TODO Auto-generated method stub

    }
}
