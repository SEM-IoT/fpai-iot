package org.tno.serial.driver;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialDriver implements SerialPortEventListener, Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SerialDriverImpl resourceDriver;
    private SerialPort serialPort;
    private String portName = "COM23"; // "/dev/ttyUSB0";
    private ByteBuffer readByteBuffer;
    private final int START_CHARACTER = 47; // "/"
    private final int FINISH_CHARACTER = 13; // "/r";

    protected int maxBufferSize = 1024;

    public SerialDriver(String portName, SerialDriverImpl resourceDriver) {
        this.portName = portName;
        this.resourceDriver = resourceDriver;
    }

    public void init() {

        logger.info("Initializing Servial Port Driver");

        Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();

        int count = 0;

        while (portList.hasMoreElements()) {
            count++;
            CommPortIdentifier commPortIdentifier = portList.nextElement();
            logger.debug("Port found: " + commPortIdentifier.getName());

            if (commPortIdentifier.getName().equals(portName)) {
                try {
                    serialPort = (SerialPort) commPortIdentifier.open("p1meter", 2000);
                    logger.info("Serial Port Used: " + portName);
                } catch (PortInUseException e) {
                    logger.error(e.toString(), e);
                }

                try {
                    serialPort.addEventListener(this);
                } catch (TooManyListenersException e) {
                    logger.error(e.toString(), e);
                }

                serialPort.notifyOnDataAvailable(true);

                try {
                    serialPort.setSerialPortParams(57600,
                                                   SerialPort.DATABITS_8,
                                                   SerialPort.STOPBITS_1,
                                                   SerialPort.PARITY_EVEN);
                } catch (UnsupportedCommOperationException e) {
                    logger.error(e.toString(), e);
                }
            }
        }

        readByteBuffer = ByteBuffer.allocate(maxBufferSize);

        logger.info("Finished initializing SmartMeterDevice. Found " + count + " devices");
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {

        if (serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {

            try {
                int nextByte, index = 0;

                while ((nextByte = serialPort.getInputStream().read()) != -1) {
                    readByteBuffer.put((byte) nextByte);
                    index++;

                    // Test voor pi!
                    byte bob[] = new byte[1];
                    bob[0] = ((byte) nextByte);
                    String ontvangen = new String(bob);
                    logger.info(ontvangen);

                    if (nextByte == FINISH_CHARACTER) {
                        String data = new String(readByteBuffer.array());
                        String cleanData = data.substring(0, index);
                        resourceDriver.receivedData(cleanData);
                        index = 0;
                        readByteBuffer.clear();
                    }
                }
            } catch (IOException e) {
                logger.error(e.toString(), e);
            }
        }
    }

    @Override
    public void run() {
        init();
    }
}
