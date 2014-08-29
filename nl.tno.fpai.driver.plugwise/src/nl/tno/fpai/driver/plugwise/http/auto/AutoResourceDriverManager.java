package nl.tno.fpai.driver.plugwise.http.auto;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver;
import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver.PlugWiseControlParameters;
import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver.PlugWiseResourceState;
import nl.tno.fpai.driver.plugwise.http.PlugwiseResourceId;
import nl.tno.fpai.driver.plugwise.http.WebAPIHandler;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ext.ObservationProviderRegistrationHelper;
import org.flexiblepower.ral.ResourceDriver;
import org.flexiblepower.ral.ext.AbstractResourceDriver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Component which manages {@link ResourceDriver}s for plugwise appliances in response to state updates received.
 */
@Component(designateFactory = AutoResourceDriverManager.Config.class)
public class AutoResourceDriverManager implements WebAPIHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private BundleContext bundleContext;
    private Config config;

    /** map of the drivers and their registrations keyed by the Plugwise identifier */
    private final Map<PlugwiseResourceId, DriverAndRegistrations> drivers = new HashMap<PlugwiseResourceId, DriverAndRegistrations>();

    /**
     * Activates the component.
     * 
     * @param context
     *            see {@link #bundleContext}
     * @param properties
     *            see {@link Config}
     */
    @Activate
    public void activate(BundleContext context, Map<String, Object> properties) {
        bundleContext = context;
        config = Configurable.createConfigurable(Config.class, properties);
    }

    /**
     * Deactivates the component and any drivers that were managed by it.
     */
    @Deactivate
    public void deactivate() {
        for (DriverAndRegistrations reg : drivers.values()) {
            reg.unregister();
        }

        drivers.clear();
    }

    @Override
    public void handle(Map<PlugwiseResourceId, PlugWiseResourceState> current) {
        // handle added resources
        Set<PlugwiseResourceId> added = new HashSet<PlugwiseResourceId>(current.keySet());
        added.removeAll(drivers.keySet());
        for (PlugwiseResourceId resourceId : added) {
            addedResource(resourceId);
        }

        // handle removed resources
        Set<PlugwiseResourceId> removed = new HashSet<PlugwiseResourceId>(drivers.keySet());
        removed.removeAll(current.keySet());
        for (PlugwiseResourceId resourceId : removed) {
            removedResource(resourceId);
        }

        // emit state updates
        for (Entry<PlugwiseResourceId, PlugWiseResourceState> state : current.entrySet()) {
            drivers.get(state.getKey()).driver.publish(state.getValue());
        }

        logger.info("Handled Plugwise updates; processing {} new resources, {} removals and {} states in total",
                    added.size(),
                    removed.size(),
                    current.size());
    }

    private void addedResource(PlugwiseResourceId resourceId) {
        PlugWiseResourceDriverImpl driver = new PlugWiseResourceDriverImpl();

        // provide meta data
        Dictionary<String, Object> properties = new Hashtable<String, Object>();

        String fpaiResourceId = config.resourceIdPrefix() + resourceId.getId()
                                + "-"
                                + resourceId.getName()
                                            .trim()
                                            .replaceAll("\\s", "-")
                                            .replaceAll("[^\\d\\w]", "")
                                            .toLowerCase();
        properties.put("resourceId", fpaiResourceId);

        // register as driver
        String[] ifaces = new String[] { ResourceDriver.class.getName(), PlugWiseResourceDriver.class.getName() };
        ServiceRegistration<?> driverReg = bundleContext.registerService(ifaces, driver, properties);

        // register as observation provider
        ServiceRegistration<?> obsProvReg = new ObservationProviderRegistrationHelper(driver).observationOf(fpaiResourceId)
                                                                                             .observedBy(this.getClass()
                                                                                                             .getName())
                                                                                             .observationType(PlugWiseResourceState.class)
                                                                                             .register();

        // keep track of the lot
        drivers.put(resourceId, new DriverAndRegistrations(driver, driverReg, obsProvReg));
    }

    private void removedResource(PlugwiseResourceId resourceId) {
        DriverAndRegistrations driverAndRegistrations = drivers.remove(resourceId);

        if (driverAndRegistrations != null) {
            driverAndRegistrations.unregister();
        }
    }

    private static class PlugWiseResourceDriverImpl extends
                                                   AbstractResourceDriver<PlugWiseResourceState, PlugWiseControlParameters> implements
                                                                                                                           PlugWiseResourceDriver {
        protected void publish(PlugWiseResourceState state) {
            publish(new Observation<PlugWiseResourceState>(state.getTime(), state));
        }

        @Override
        public void setControlParameters(PlugWiseControlParameters controlParameters) {
            // nothing to control in this version
        }
    }

    private class DriverAndRegistrations {
        /** The driver */
        public PlugWiseResourceDriverImpl driver;

        /** registration as driver */
        public ServiceRegistration<?> driverRegistration;

        /** registration as observation provider */
        public ServiceRegistration<?> observationProviderRegistration;

        public DriverAndRegistrations(PlugWiseResourceDriverImpl driver,
                                      ServiceRegistration<?> driverRegistration,
                                      ServiceRegistration<?> observationProviderRegistration) {
            this.driver = driver;
            this.driverRegistration = driverRegistration;
            this.observationProviderRegistration = observationProviderRegistration;
        }

        public void unregister() {
            unregister(driverRegistration);
            unregister(observationProviderRegistration);
        }

        private void unregister(ServiceRegistration<?> reg) {
            if (reg == null) {
                return;
            }

            try {
                reg.unregister();
            } catch (Exception e) {
                logger.error("Failed to unregister: " + reg, e);
            }
        }
    }

    /**
     * Configuration of the {@link AutoResourceDriverManager}.
     */
    @OCD(name = "Plugwise auto resource driver manager config")
    public interface Config {
        /**
         * @return The prefix used when generating a resource id.
         */
        @AD(deflt = "plugwise-plug-", description = "The prefix used when generating a resource id")
        String resourceIdPrefix();
    }
}
