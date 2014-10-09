package org.tno.nl.iotlab.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.iotlab.common.vo.VO;

import aQute.bnd.annotation.component.Reference;

public class IoTProtocolDriver {

    private final static Logger logger = LoggerFactory.getLogger(IoTProtocolDriver.class);
    private ScheduledFuture<?> scheduledFuture;
    private final List<String> addresses = new ArrayList<String>();
    private final List<String> configAddresses;
    private final List<String> configPIDs;
    private final ComponentCreator componentCreator;
    private String latestMessageNumber = "";
    private Boolean listen = true;

    ConfigurationAdmin configurationAdmin;

    @Reference
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public IoTProtocolDriver(List<String> configAddresses,
                             List<String> configPIDs,
                             ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
        componentCreator = new ComponentCreator(configurationAdmin);
        this.configAddresses = configAddresses;
        this.configPIDs = configPIDs;
    }

    public void newDataReceived(String id) {
        newData(id);
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

    public void newData(String data) {

        String[] subData = data.split(" ");

        for (String sub : subData) {

            // Only listen to a message once! TODO: fix in firmware eduinos
            if (sub.contains("#")) {
                if (sub.equals(latestMessageNumber)) {
                    listen = false;
                    logger.debug("Duplicated message, dont listen!");
                } else {
                    listen = true;
                    latestMessageNumber = sub;
                }
            }

            if (sub.contains("...") && listen) {

                String cleanAddress = sub.substring(0, sub.indexOf("."));
                // Create VO if not there yet
                createVO(cleanAddress, data);

                // Send the data to the right VO
                for (VO vo : getVoList()) {
                    if (vo.getName().equals(cleanAddress)) {
                        vo.sendNewData(data);
                    }
                }
            }
        }
    }

    private void createVO(String address, String data) {
        if (!addresses.contains(address)) {
            logger.debug("Decoder tries to make new VO for address: " + address);
            componentCreator.createVO(address, findPID(address));
            addresses.add(address);
        }
    }

    private String[] findPID(String address) {

        int index = -1, i = 0;
        for (String config : configAddresses) {
            if (config.equals(address)) {
                index = i;
            }
            i++;
        }

        logger.debug("Found index: " + index);
        if (index > -1) {
            String pid = configPIDs.get(index);
            logger.debug("Found pid at index: " + pid);
            return pid.split("-");
        } else {
            logger.error("PID not found!, no vo will be created");
        }

        return null;
    }
}
