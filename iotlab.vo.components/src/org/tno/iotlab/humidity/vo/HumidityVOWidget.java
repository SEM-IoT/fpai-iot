package org.tno.iotlab.humidity.vo;

import java.util.Date;
import java.util.Locale;

import org.flexiblepower.ui.Widget;
import org.tno.iotlab.humidity.vo.HumidityVOImpl.HumVoState;

public class HumidityVOWidget implements Widget {

    public static class Update {
        private final boolean error;
        private final double humidity;
        private final String address;
        private final Date timestamp;

        public Update() {
            error = false;
            address = "-2";
            timestamp = new Date();
            humidity = -9999;
        }

        public Update(HumVoState state) {
            error = false;
            humidity = state.getHumidity();
            address = state.getAddress();
            timestamp = state.timestamp();
        }

    }

    private final HumidityVOImpl temperatureVO;

    public HumidityVOWidget(HumidityVOImpl temperatureVO) {
        this.temperatureVO = temperatureVO;
    }

    public Update update(Locale locale) {
        HumVoState state = null;
        state = temperatureVO.getState();
        if (state != null) {
            return new Update(state);
        } else {
            return new Update();
        }
    }

    @Override
    public String getTitle(Locale locale) {
        return "IoTLab Humidity VO Module";
    }

    public HumidityVOImpl getInrgMicrochpManager() {
        return temperatureVO;
    }

}
