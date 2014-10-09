package org.tno.nl.iotlab.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.iotlab.common.vo.VO;

public class IoTProtocolDriver {

    private final static Logger logger = LoggerFactory.getLogger(IoTProtocolDriver.class);
    private ScheduledFuture<?> scheduledFuture;
    private DataDecoder dataDecoder;

    public void newDataReceived(String id) {
        dataDecoder.newData(id);
    }

    private final List<VO> voList = new ArrayList<VO>();

    public void setvoList(VO newVO) {

        boolean unique = true;
        for (VO vo : voList) {
            if (vo.getName().equals(newVO.getName())) {
                unique = false;
            }
        }
        voList.add(newVO);
        logger.error("new VO added to list" + newVO.getName());
        logger.debug("The list of vo's has size: " + voList.size());
    }

    public List<VO> getVoList() {
        return voList;
    }
}
