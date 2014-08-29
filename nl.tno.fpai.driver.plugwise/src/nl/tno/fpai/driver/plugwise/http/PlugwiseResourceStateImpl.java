package nl.tno.fpai.driver.plugwise.http;

import java.util.Date;

import javax.measure.Measurable;
import javax.measure.quantity.Power;

import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver.PlugWiseResourceState;

class PlugwiseResourceStateImpl implements PlugWiseResourceState {
    private final Measurable<Power> demand;
    private final Date timestamp;
    private final boolean connected;

    public PlugwiseResourceStateImpl(Measurable<Power> demand, Date timestamp, boolean connected) {
        this.demand = demand;
        this.timestamp = timestamp;
        this.connected = connected;
    }

    @Override
    public Measurable<Power> getDemand() {
        return demand;
    }

    @Override
    public Date getTime() {
        return timestamp;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String toString() {
        return String.format("[Plugwise Resource State @ %s : demand=%s, connected=%s]", timestamp, demand, connected);
    }
}
