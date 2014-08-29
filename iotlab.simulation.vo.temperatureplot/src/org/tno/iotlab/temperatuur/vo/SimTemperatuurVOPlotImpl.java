package org.tno.iotlab.temperatuur.vo;

import java.text.NumberFormat;
import java.text.ParseException;
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
import org.tno.iotlab.temperatuur.vo.SimTemperatuurVOPlotImpl.TempVoState;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component(provide = VO.class, immediate = true)
public class SimTemperatuurVOPlotImpl extends AbstractObservationProvider<TempVoState> implements VO, Runnable {

    private final static Logger logger = LoggerFactory.getLogger(SimTemperatuurVOPlotImpl.class);

    final static class TempVoState {
        private final boolean isConnected;
        private final double temperature;
        private final String address;
        private final long timestapComputerInMillis;

        TempVoState(double temperature, String address) {
            isConnected = false;
            this.temperature = temperature;
            this.address = address;
            timestapComputerInMillis = new Date().getTime();
        }

        public double getTemperarture() {
            return temperature;
        }

        public String getAddress() {
            return address;
        }

        public long timestamp() {
            return timestapComputerInMillis;
        }
    }

    // @Meta.OCD
    // interface Config {
    // @Meta.AD(deflt = "SimAddress")
    // String resourceId();
    // }

    private ScheduledFuture<?> scheduledFuture;
    private ServiceRegistration<?> observationProviderRegistration;
    private String address;
    private TempVoState latestState;
    private SimTemperatureVOPlotWidget widget;
    private ServiceRegistration<Widget> widgetRegistration;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            // Config config = Configurable.createConfigurable(Config.class, properties);
            address = "SimAddress";
            // Register with protocol driver
            // driver.setvoList(this);
            // logger.error("driver connected");

            widget = new SimTemperatureVOPlotWidget(this);
            widgetRegistration = bundleContext.registerService(Widget.class, widget, null);

            observationProviderRegistration = new ObservationProviderRegistrationHelper(this).observationType(TempVoState.class)
                                                                                             .observationOf(address)
                                                                                             .observedBy(address)
                                                                                             .register();
            scheduledFuture = schedulerService.scheduleAtFixedRate(this, 0, 10, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Throwable e) {
            System.out.println("!!!!_---------------");
        }
    }

    @Deactivate
    public void deactivate() {
        scheduledFuture.cancel(false);
        observationProviderRegistration.unregister();
    }

    // private IoTProtocolDriver driver;
    //
    // @Reference
    // public void setvoList(IoTProtocolDriver driver) {
    // this.driver = driver;
    //
    // }

    // private MqttConnection mqttService;
    //
    // @Reference
    // public void setMqttService(MqttConnection mqttService) {
    // this.mqttService = mqttService;
    // }

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
        Double temp = 19 + Math.random() * 2;
        sendNewData(String.format("%.2f", temp));
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
    public void sendNewData(String temperature) {
        NumberFormat nf = NumberFormat.getInstance();
        try {
            Double temp = nf.parse(temperature).doubleValue();
            logger.debug("SimTemperature = " + temp);
            latestState = new TempVoState(temp, address);
            publish(new Observation<TempVoState>(timeService.getTime(), getState()));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            logger.error("Error converting input data", e);
        }

        // If mqtt service is connected
        // mqttService.publishMqtt(temperature, "test/temperature");
    }

    public TempVoState getState() {
        if (latestState != null) {
            return latestState;
        }
        return new TempVoState(-999.0, "-1");
    }
}
