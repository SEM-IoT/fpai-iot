package nl.tno.fpai.driver.plugwise.http;

import java.util.Map;

import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver.PlugWiseResourceState;

/**
 * Handler of updates from polling the Plugwise HTTP / XML API.
 */
public interface WebAPIHandler {
    /**
     * @param states
     *            Update of the resource states keyed by the resource id
     */
    void handle(Map<PlugwiseResourceId, PlugWiseResourceState> states);
}
