package nl.tno.iotlab.eduino.RF24;

import java.nio.ByteBuffer;

public interface RF24Driver {
    void updateState(ByteBuffer buffer);
}
