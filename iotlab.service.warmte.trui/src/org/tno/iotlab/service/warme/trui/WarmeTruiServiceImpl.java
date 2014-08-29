package org.tno.iotlab.service.warme.trui;

import java.security.Provider.Service;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ext.AbstractObservationProvider;
import org.flexiblepower.observation.ext.ObservationProviderRegistrationHelper;
import org.flexiblepower.time.TimeService;
import org.flexiblepower.ui.Widget;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.iotlab.common.service.ServiceState;
import org.tno.iotlab.common.vo.VOState;
import org.tno.iotlab.service.warme.trui.WarmeTruiServiceImpl.Config;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = Service.class, immediate = true)
public class WarmeTruiServiceImpl extends AbstractObservationProvider<ServiceState> implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(WarmeTruiServiceImpl.class);

    final static class ServiceStateImpl implements ServiceState {
        private final boolean isConnected;
        private final double temperature;
        private final String address;
        private final Date timestampComputer;

        ServiceStateImpl(double temperature, String address, Date timeStampMeasurement) {
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
        @Meta.AD(deflt = "Warme Truien Service")
        String resourceId();
    }

    private ScheduledFuture<?> scheduledFuture;
    private ServiceRegistration<?> observationProviderRegistration;
    private String address;
    private ServiceStateImpl latestState;
    private WarmeTruiServiceWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;
    private final DataListner listner = new DataListner();

    private MqttClient mqttClient;
    private MqttConnectOptions conOpt;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            address = config.resourceId();

            widget = new WarmeTruiServiceWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);

            mqttSetup();
            mqttSubscribeTopic("test/+");

            observationProviderRegistration = new ObservationProviderRegistrationHelper(this).observationType(VOState.class)
                                                                                             .observationOf(address)
                                                                                             .observedBy(address)
                                                                                             .register();
            scheduledFuture = schedulerService.scheduleAtFixedRate(this, 0, 60, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Throwable e) {
            logger.error("Error in activation of Warme Truien Service", e);
        }
    }

    private void mqttSetup() {
        try {
            mqttClient = new MqttClient("tcp://192.168.1.3:1883", "test");
            mqttClient.setCallback(listner);

        } catch (MqttException e) {
            logger.error("Error in mqtt setup: ", e);
        }

    }

    private void mqttSubscribeTopic(String topic) {
        try {
            mqttClient.connect();
            mqttClient.subscribe(topic);
            mqttClient.disconnect();
            logger.info("Service is subscribed to topic: " + topic);
        } catch (MqttSecurityException e) {
            logger.error("Error in mqtt Subscribe: ", e);
        } catch (MqttException e) {
            logger.error("Error in mqtt Subscribe: ", e);
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
        System.out.println("Wakker");
    }

    public void publishNewData(Double temperature, Date date) {
        latestState = new ServiceStateImpl(temperature, address, date);
        publish(new Observation<ServiceStateImpl>(timeService.getTime(), getState()));
        publishMqtt(temperature.toString());
    }

    public void publishMqtt(String input) {
        try {
            mqttClient.connect();
        } catch (MqttSecurityException e) {
            logger.error("Exception in mqtt com", e);
        } catch (MqttException e) {
            logger.error("Exception in mqtt com", e);
        }
        MqttMessage message = new MqttMessage(input.getBytes());
        MqttTopic topic = mqttClient.getTopic("test/1");

        // Wait until the message has been delivered to the server
        try {
            MqttDeliveryToken token = topic.publish(message);
            token.waitForCompletion();
            logger.info("Message received by mqtt bus");
            mqttClient.disconnect();
        } catch (MqttSecurityException e) {
            logger.error("Exception in mqtt com", e);
        } catch (MqttException e) {
            logger.error("Exception in mqtt com", e);
        }
    }

    public ServiceStateImpl getState() {
        if (latestState != null) {
            return latestState;
        }
        return new ServiceStateImpl(-999.0, "-1", new Date());
    }
}
