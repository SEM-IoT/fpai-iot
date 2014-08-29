package org.tno.nl.iotlab.protocol;


public interface IoTProtocol {

    public abstract String readSingleValue(String address);

    public abstract String[] readValues(String[] addresses);
}
