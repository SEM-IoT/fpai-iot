package nl.tno.iotlab.serial.driver.simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import nl.tno.iotlab.serial.driver.SerialProtocolDriver;
import nl.tno.iotlab.serial.driver.simulation.SimulatedSerialPort.Config;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, immediate = true, provide = SerialProtocolDriver.class)
public class SimulatedSerialPort implements SerialProtocolDriver {
    private static final Logger log = LoggerFactory.getLogger(SimulatedSerialPort.class);

    @Meta.OCD(description = "Serial protocol simulation")
    public static interface Config {
        @Meta.AD(name = "Port name",
                 deflt = "COM9",
                 description = "Reference to the serial port on your computer (as simulated)")
        public String port();
    }

    private final AtomicBoolean running;
    private final BlockingQueue<String> messages;

    private BufferedReader reader;

    public SimulatedSerialPort() {
        messages = new LinkedBlockingQueue<String>();
        running = new AtomicBoolean();
    }

    @Activate
    public void activate(BundleContext context) {
        try {
            final InputStream input = context.getBundle().getEntry("simulation.log").openStream();
            reader = new BufferedReader(new InputStreamReader(input));

            running.set(true);

            new Thread(getClass().getName()) {
                @Override
                public synchronized void run() {
                    try {
                        final long lastTime = 0;
                        while (running.get()) {
                            String line;
                            line = reader.readLine();
                            if (line != null) {
                                final long millis = Long.parseLong(line.substring(0, line.indexOf(':')));
                                final long diff = millis - lastTime;
                                if (diff > 10) {
                                    wait(diff / 10);
                                }
                                messages.put(line);
                            } else {
                                running.set(false);
                            }
                        }
                    } catch (final IOException ex) {
                        log.error("I/O error while reading file", ex);
                    } catch (final InterruptedException ex) {
                        log.error("Interrupted while reading file", ex);
                    }
                };
            }.start();
        } catch (final IOException ex) {
            log.error("Could not open simulation input", ex);
        }
    }

    @Deactivate
    public void deactivate() throws IOException {
        reader.close();
        running.set(false);
    }

    @Override
    public String pollMessage() {
        return messages.poll();
    }

    @Override
    public String readMessage() throws InterruptedException {
        return messages.take();
    }
}
