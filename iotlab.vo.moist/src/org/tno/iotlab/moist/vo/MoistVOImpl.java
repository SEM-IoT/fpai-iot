package org.tno.iotlab.moist.vo;

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
import org.tno.iotlab.moist.vo.MoistVOImpl.Config;
import org.tno.iotlab.moist.vo.MoistVOImpl.MoistVoState;
import org.tno.mqtt.connection.MqttConnection;
import org.tno.nl.iotlab.protocol.IoTProtocolDriver;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = VO.class, immediate = true)
public class MoistVOImpl extends AbstractObservationProvider<MoistVoState> implements VO {

    private final static Logger logger = LoggerFactory.getLogger(MoistVOImpl.class);

    final static class MoistVoState {
        private final boolean isConnected;
        private final double moist;
        private final String address;
        private final Date timestampComputer;

        MoistVoState(double moist, String address) {
            isConnected = false;
            this.moist = moist;
            this.address = address;
            timestampComputer = new Date();
        }

        public double getMoist() {
            return moist;
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
        @Meta.AD(deflt = "MoistVO")
        String resourceId();
    }

    private ScheduledFuture<?> scheduledFuture;
    private ServiceRegistration<?> observationProviderRegistration;
    private String address;
    private MoistVoState latestState;
    private MoistVOWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            address = config.resourceId();
            // Register with protocol driver
            driver.setvoList(this);
            logger.error("driver connected");

            widget = new MoistVOWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);

            observationProviderRegistration = new ObservationProviderRegistrationHelper(this).observationType(MoistVoState.class)
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
        String moist = newData.substring(newData.indexOf("V") - 4, newData.indexOf("V"));
        logger.debug("Temperature = " + moist);

        Integer moistPercentage = (int) ((Double.parseDouble(moist) / 3.3) * 100);

        latestState = new MoistVoState(moistPercentage, address);
        publish(new Observation<MoistVoState>(timeService.getTime(), getState()));
        mqttService.publishMqtt(moistPercentage.toString(), "test/moist");
    }

    public MoistVoState getState() {
        if (latestState != null) {
            return latestState;
        }
        return new MoistVoState(-999.0, "-1");
    }
}
