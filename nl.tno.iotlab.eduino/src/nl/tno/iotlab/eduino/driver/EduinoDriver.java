package nl.tno.iotlab.eduino.driver;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Temperature;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import nl.tno.iotlab.eduino.driver.EduinoDriver.Config;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ext.AbstractObservationProvider;
import org.flexiblepower.observation.ext.ObservationProviderRegistrationHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, immediate = true, provide = EduinoDriver.class)
public class EduinoDriver extends AbstractObservationProvider<EduinoDriver.State> {
    private static final Logger log = LoggerFactory.getLogger(EduinoDriver.State.class);

    static class State {
        private final Measurable<Temperature> temperature;
        private final Measurable<Dimensionless> light;
        private final Measurable<Dimensionless> motion;
        private final Measurable<ElectricPotential> battery;

        public State(Measurable<Temperature> temperature,
                     Measurable<Dimensionless> light,
                     Measurable<Dimensionless> motion,
                     Measurable<ElectricPotential> battery) {
            this.temperature = temperature;
            this.light = light;
            this.motion = motion;
            this.battery = battery;
        }

        public Measurable<Temperature> getTemperature() {
            return temperature;
        }

        public Measurable<Dimensionless> getLight() {
            return light;
        }

        public Measurable<Dimensionless> getMotion() {
            return motion;
        }

        public Measurable<ElectricPotential> getBattery() {
            return battery;
        }
    }

    @Meta.OCD(description = "Driver for reading out the Edwino gateway through a serial connection")
    public static interface Config {
        @Meta.AD(deflt = "01", description = "Address of the Edwino device that we want to represent")
        public String address();
    }

    private ServiceRegistration<?> observationProviderRegistration;

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {
        final Config config = Configurable.createConfigurable(Config.class, properties);
        observationProviderRegistration = new ObservationProviderRegistrationHelper(this, context).observationOf("Eduino @ " + config.address())
                                                                                                  .observedBy(getClass().getName())
                                                                                                  .observationType(State.class)
                                                                                                  .register();
    }

    @Deactivate
    public void deactivate() {
        observationProviderRegistration.unregister();
    }

    public void update(Date now, String[] parts) {
        // 64368: APP Receiving type-S 24.6C 0L 0PIR 0.00V 0lost from 02...
        // 0 1 2 3 4 5 6 78 9 10 11

        final String sTemperature = parts[4];
        final String sLight = parts[5];
        final String sMotion = parts[6];
        final String sBattery = parts[8];

        try {
            final double nTemp = Double.parseDouble(sTemperature.substring(0, sTemperature.length() - 1));
            final Measurable<Temperature> temp = Measure.valueOf(nTemp, SI.CELSIUS);
            final int nLgiht = Integer.parseInt(sLight.substring(0, sLight.length() - 1));
            final Measurable<Dimensionless> light = Measure.valueOf(nLgiht, Unit.ONE);
            final int nMotion = Integer.parseInt(sMotion.substring(0, sMotion.length() - 1));
            final Measurable<Dimensionless> motion = Measure.valueOf(nMotion, Unit.ONE);
            final double nBattery = Double.parseDouble(sBattery.substring(0, sBattery.length() - 1));
            final Measurable<ElectricPotential> battery = Measure.valueOf(nBattery, SI.VOLT);

            publish(new Observation<State>(now, new State(temp, light, motion, battery)));
        } catch (final Exception ex) {
            log.warn("Could not parse line: " + Arrays.toString(parts), ex);
        }
    }
}
