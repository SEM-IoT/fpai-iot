package nl.tno.iotlab.eduino.api;

import java.util.Date;
import java.util.StringTokenizer;

public interface EduinoProtocolHandler {
    void updateState(Date time, StringTokenizer tokenizer);
}
