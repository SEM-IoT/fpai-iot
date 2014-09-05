package nl.tno.iotlab.eduino.RF24.impl;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import nl.tno.iotlab.eduino.RF24.RF24Driver;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ext.AbstractObservationProvider;
import org.flexiblepower.observation.ext.ObservationProviderRegistrationHelper;
import org.flexiblepower.time.TimeService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component
public class RF24SensorBoard extends AbstractObservationProvider<RF24SensorBoard.State> implements RF24Driver {
    private static final Logger log = LoggerFactory.getLogger(RF24SensorBoard.class);

    @Meta.OCD(description = "Eduino Sensor Board with temperature and light sensor")
    public static interface Config {
        @Meta.AD(deflt = "02", description = "Address of the sensor board")
        public String address();
    }

    static class State {
        private final Measurable<Temperature> temperature;
        private final Measurable<Dimensionless> light;

        public State(Measurable<Temperature> temperature, Measurable<Dimensionless> light) {
            this.temperature = temperature;
            this.light = light;
        }

        public Measurable<Temperature> getTemperature() {
            return temperature;
        }

        public Measurable<Dimensionless> getLight() {
            return light;
        }
    }

    private TimeService timeService;

    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }

    private ServiceRegistration<?> serviceRegistration;
    private String address;

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {
        final Config config = Configurable.createConfigurable(Config.class, properties);
        address = config.address();
        serviceRegistration = new ObservationProviderRegistrationHelper(this).observationOf("Sensor board with address " + config.address())
                                                                             .observedBy(getClass().getName())
                                                                             .observationType(State.class)
                                                                             .register();
    }

    @Deactivate
    public void deactivate() {
        serviceRegistration.unregister();
    }

    @Override
    public void updateState(ByteBuffer buffer) {
        final Date now = timeService.getTime();

        final int nTemp = buffer.getShort();
        final int nLight = buffer.get();

        final Measurable<Temperature> temp = Measure.valueOf(nTemp, SI.DECI(SI.CELSIUS));
        final Measurable<Dimensionless> light = Measure.valueOf(nLight, Unit.ONE);

        log.debug("SensorBoard {} => temp={} and light={}", address, temp, light);
        publish(new Observation<State>(now, new State(temp, light)));
    }
}
