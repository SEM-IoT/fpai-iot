package nl.tno.iotlab.serial.driver;

public interface SerialProtocolDriver {
    String pollMessage();

    String readMessage() throws InterruptedException;
}
