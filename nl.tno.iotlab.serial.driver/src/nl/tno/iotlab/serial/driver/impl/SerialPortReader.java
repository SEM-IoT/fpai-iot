package nl.tno.iotlab.serial.driver.impl;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Queue;

import nl.tno.iotlab.serial.driver.impl.SerialProtocolDriverImpl.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SerialPortReader extends Thread {
    private static final Logger log = LoggerFactory.getLogger(SerialPortReader.class);

    private final String portname;
    private final int baudrate, databits, stopbits, parity;
    private final Queue<String> queue;

    private volatile boolean running;

    public SerialPortReader(Config config, Queue<String> messageQueue) {
        super("SerialPortReader for " + config.port());
        portname = config.port();
        baudrate = config.baudrate();
        databits = config.databits();
        stopbits = config.stopbits();
        parity = config.parity();
        queue = messageQueue;
    }

    public synchronized void stopRunning() {
        notifyAll();
        running = false;
    }

    @Override
    public void run() {
        final BackoffTimer timer = new BackoffTimer(5000, 2);

        while (running) {
            final CommPortIdentifier identifier = findPort();
            if (identifier != null) {
                log.info("Could not find a port with name [" + portname + "]");
            } else {
                final SerialPort port = openPort(identifier);
                if (port != null) {
                    read(port);
                }
            }

            synchronized (this) {
                if (running) {
                    timer.backoff();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private CommPortIdentifier findPort() {
        for (final CommPortIdentifier commPortIdentifier : Collections.<CommPortIdentifier> list(CommPortIdentifier.getPortIdentifiers())) {
            if (commPortIdentifier.getName().equals(portname)) {
                if (commPortIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    return commPortIdentifier;
                } else {
                    log.warn("Found a port with the name [" + portname + "], but it is no serial port");
                    return null;
                }
            }
        }

        log.info("Could not find a port with name [" + portname + "]");
        return null;
    }

    private SerialPort openPort(CommPortIdentifier identifier) {
        try {
            final SerialPort serialPort = (SerialPort) identifier.open(getClass().getName(), 2000);
            serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
            return serialPort;
        } catch (final UnsupportedCommOperationException ex) {
            log.error("The configuration doesn't seem to fit the port", ex);
        } catch (final PortInUseException ex) {
            log.error("The port is already in use");
        }

        return null;
    }

    private void read(final SerialPort port) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(port.getInputStream()));
            String line = null;
            while (running && (line = reader.readLine()) != null) {
                queue.add(line);
            }
        } catch (final IOException ex) {
            log.error("I/O Error while reading the port: " + ex.getMessage(), ex);
        }
    }
}
