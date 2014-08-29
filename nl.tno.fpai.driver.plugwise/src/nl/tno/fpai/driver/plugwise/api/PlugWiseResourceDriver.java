package nl.tno.fpai.driver.plugwise.api;

import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver.PlugWiseControlParameters;
import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver.PlugWiseResourceState;

import org.flexiblepower.ral.ResourceControlParameters;
import org.flexiblepower.ral.ResourceDriver;
import org.flexiblepower.ral.drivers.uncontrolled.UncontrolledState;

/**
 * Interface for Plugwise Resource Drivers.
 */
public interface PlugWiseResourceDriver extends ResourceDriver<PlugWiseResourceState, PlugWiseControlParameters> {
    /** The state of a Plugwise resource currently corresponds to the state of an uncontrolled resource. */
    interface PlugWiseResourceState extends UncontrolledState {
    }

    /** The Plugwise resources currently don't have any parameters for control. */
    interface PlugWiseControlParameters extends ResourceControlParameters {
    }
}
