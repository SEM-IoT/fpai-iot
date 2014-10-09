package org.tno.iotlab.moist.vo;

import java.util.Date;
import java.util.Locale;

import org.flexiblepower.ui.Widget;
import org.tno.iotlab.moist.vo.MoistVOImpl.MoistVoState;

public class MoistVOWidget implements Widget {

    public static class Update {
        private final boolean error;
        private final double moist;
        private final String address;
        private final Date timestamp;

        public Update() {
            error = false;
            moist = -9999.2;
            address = "-2";
            timestamp = new Date();
        }

        public Update(MoistVoState state) {
            error = false;
            moist = state.getMoist();
            address = state.getAddress();
            timestamp = state.timestamp();
        }

    }

    private final MoistVOImpl moistVO;

    public MoistVOWidget(MoistVOImpl temperatureVO) {
        moistVO = temperatureVO;
    }

    public Update update(Locale locale) {
        MoistVoState state = null;
        state = moistVO.getState();
        if (state != null) {
            return new Update(state);
        } else {
            return new Update();
        }
    }

    @Override
    public String getTitle(Locale locale) {
        return "IoTLab Moist VO Module";
    }

    public MoistVOImpl getInrgMicrochpManager() {
        return moistVO;
    }

}
