package nl.tno.iotlab.edwino.driver;

import java.util.Date;
import java.util.Map;

import javax.measure.Measurable;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Temperature;

import nl.tno.iotlab.edwino.driver.EdwinoDriver.Config;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationProvider;
import org.flexiblepower.observation.ext.AbstractObservationProvider;
import org.osgi.framework.BundleContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class,
           immediate = true,
           provide = { ObservationProvider.class, EdwinoDriver.class })
public class EdwinoDriver extends AbstractObservationProvider<EdwinoDriver.State> {
    static class State {
        private final Measurable<Temperature> temperature1, temperature2;
        private final Measurable<Dimensionless> humidity;
        private final Measurable<Dimensionless> light;

        public State(Measurable<Temperature> temperature1,
                     Measurable<Temperature> temperature2,
                     Measurable<Dimensionless> humidity,
                     Measurable<Dimensionless> light) {
            this.temperature1 = temperature1;
            this.temperature2 = temperature2;
            this.humidity = humidity;
            this.light = light;
        }

        public Measurable<Temperature> getTemperature1() {
            return temperature1;
        }

        public Measurable<Temperature> getTemperature2() {
            return temperature2;
        }

        public Measurable<Dimensionless> getHumidity() {
            return humidity;
        }

        public Measurable<Dimensionless> getLight() {
            return light;
        }
    }

    @Meta.OCD(description = "Driver for reading out the Edwino gateway through a serial connection")
    public static interface Config {
        @Meta.AD(deflt = "01", description = "Address of the Edwino device that we want to represent")
        public String address();

        @Meta.AD(deflt = "true", description = "Enables parsing of the first temperature sensor")
        public boolean temperature1_active();

        @Meta.AD(deflt = "true", description = "Enables parsing of the second temperature sensor")
        public boolean temperature2_active();

        @Meta.AD(deflt = "true", description = "Enables parsing of the humidity sensor")
        public boolean humidity_active();

        @Meta.AD(deflt = "true", description = "Enables parsing of the light sensor")
        public boolean light_active();
    }

    private boolean temp1Active, temp2Active, humidityActive, lightActive;;

    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {
        final Config config = Configurable.createConfigurable(EdwinoDriver.Config.class, properties);
        temp1Active = config.temperature1_active();
        temp2Active = config.temperature2_active();
        humidityActive = config.humidity_active();
        lightActive = config.light_active();
    }

    public void update(Date now,
                       Measurable<Temperature> temp1,
                       Measurable<Temperature> temp2,
                       Measurable<Dimensionless> humidity,
                       Measurable<Dimensionless> light) {
        publish(new Observation<State>(now, new State(temp1Active ? temp1 : null,
                                                      temp2Active ? temp2 : null,
                                                      humidityActive ? humidity : null,
                                                      lightActive ? light : null)));
    }
}
