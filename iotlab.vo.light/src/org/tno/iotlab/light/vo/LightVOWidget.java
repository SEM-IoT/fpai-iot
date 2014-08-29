package org.tno.iotlab.light.vo;

import java.util.Date;
import java.util.Locale;

import org.flexiblepower.ui.Widget;
import org.tno.iotlab.light.vo.LightVOImpl.LightVoState;

public class LightVOWidget implements Widget {

    public static class Update {
        private final boolean error;
        private final double light;
        private final String address;
        private final Date timestamp;

        public Update() {
            error = false;
            light = -9999.2;
            address = "-2";
            timestamp = new Date();
        }

        public Update(LightVoState state) {
            error = false;
            light = state.getLight();
            address = state.getAddress();
            timestamp = state.timestamp();
        }

    }

    private final LightVOImpl lightVO;

    public LightVOWidget(LightVOImpl lightVO) {
        this.lightVO = lightVO;
    }

    public Update update(Locale locale) {
        LightVoState state = null;
        state = lightVO.getState();
        if (state != null) {
            return new Update(state);
        } else {
            return new Update();
        }
    }

    @Override
    public String getTitle(Locale locale) {
        return "IoTLab Light VO Module";
    }

    public LightVOImpl getInrgMicrochpManager() {
        return lightVO;
    }

}
