package org.tno.iotlab.outside.temperatuur.vo;

import java.util.Date;
import java.util.Locale;

import org.flexiblepower.ui.Widget;
import org.tno.iotlab.outside.temperatuur.vo.OutsideTemperatuurVOImpl.VoState;

public class OutsideTemperatureVOWidget implements Widget {

    public static class Update {
        private final boolean error;
        private final double temperature;
        private final String address;
        private final Date timestamp;

        public Update() {
            error = false;
            temperature = -9999.2;
            address = "-2";
            timestamp = new Date();
        }

        public Update(VoState state) {
            error = false;
            temperature = state.getTemperarture();
            address = state.getAddress();
            timestamp = state.timestamp();
        }

    }

    private final OutsideTemperatuurVOImpl temperatureVO;

    public OutsideTemperatureVOWidget(OutsideTemperatuurVOImpl temperatureVO) {
        this.temperatureVO = temperatureVO;
    }

    public Update update(Locale locale) {
        VoState state = null;
        state = temperatureVO.getState();
        if (state != null) {
            return new Update(state);
        } else {
            return new Update();
        }
    }

    @Override
    public String getTitle(Locale locale) {
        return "IoTLab Outside Temp VO";
    }

    public OutsideTemperatuurVOImpl getInrgMicrochpManager() {
        return temperatureVO;
    }

}
