package org.tno.iotlab.temperatuur.vo;

import java.util.Date;
import java.util.Locale;

import org.flexiblepower.ui.Widget;
import org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl.TempVoState;

public class TemperatureVOWidget implements Widget {

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

        public Update(TempVoState state) {
            error = false;
            temperature = state.getTemperarture();
            address = state.getAddress();
            timestamp = state.timestamp();
        }

    }

    private final TemperatuurVOImpl temperatureVO;

    public TemperatureVOWidget(TemperatuurVOImpl temperatureVO) {
        this.temperatureVO = temperatureVO;
    }

    public Update update(Locale locale) {
        TempVoState state = null;
        state = temperatureVO.getState();
        if (state != null) {
            return new Update(state);
        } else {
            return new Update();
        }
    }

    @Override
    public String getTitle(Locale locale) {
        return "IoTLab Temperature VO Module";
    }

    public TemperatuurVOImpl getInrgMicrochpManager() {
        return temperatureVO;
    }

}
