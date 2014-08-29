package org.tno.iotlab.service.warme.trui;

import java.util.Date;
import java.util.Locale;

import org.flexiblepower.ui.Widget;
import org.tno.iotlab.service.warme.trui.WarmeTruiServiceImpl.ServiceStateImpl;

public class WarmeTruiServiceWidget implements Widget {

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

        public Update(ServiceStateImpl state) {
            error = false;
            temperature = state.getTemperarture();
            address = state.getAddress();
            timestamp = state.timestamp();
        }

    }

    private final WarmeTruiServiceImpl temperatureVO;

    public WarmeTruiServiceWidget(WarmeTruiServiceImpl temperatureVO) {
        this.temperatureVO = temperatureVO;
    }

    public Update update(Locale locale) {
        ServiceStateImpl state = null;
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

    // public WarmeTruiServiceImpl getInrgMicrochpManager() {
    // return temperatureVO;
    // }

}
