package nl.tno.iotlab.serial.driver.impl;

public class BackoffTimer {
    private final long initialDuration;
    private final double factor;

    private long currentDuration;

    public BackoffTimer(long initialDuration, double factor) {
        this.initialDuration = initialDuration;
        this.factor = factor;
        currentDuration = initialDuration;
    }

    public void reset() {
        currentDuration = initialDuration;
    }

    public synchronized void wakeup() {
        notifyAll();
    }

    public synchronized void backoff() {
        try {
            wait(currentDuration);
        } catch (final InterruptedException e) {
        }

        currentDuration = (long) (currentDuration * factor);
    }
}
