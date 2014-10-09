package org.tno.serial.driver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.flexiblepower.protocol.rxtx.Connection;
import org.flexiblepower.protocol.rxtx.ConnectionFactory;
import org.flexiblepower.protocol.rxtx.SerialConnectionOptions;
import org.flexiblepower.protocol.rxtx.SerialConnectionOptions.Baudrate;
import org.flexiblepower.protocol.rxtx.SerialConnectionOptions.Databits;
import org.flexiblepower.protocol.rxtx.SerialConnectionOptions.Parity;
import org.flexiblepower.protocol.rxtx.SerialConnectionOptions.Stopbits;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.nl.iotlab.protocol.IoTProtocolDriver;
import org.tno.serial.driver.SerialDriverImpl.Config;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, immediate = true)
public class SerialDriverImpl {

    private final static Logger log = LoggerFactory.getLogger(SerialDriverImpl.class);

    Thread serialThread;

    @Meta.OCD
    interface Config {

        @Meta.AD(deflt = "/dev/tty.SLAB_USBtoUART")
        String port_name();

        @Meta.AD(deflt = "01,02,03")
        List<String> addresses();

        @Meta.AD(deflt = "org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl-org.tno.iotlab.temperatuur.vo.HumidityVOImpl-org.tno.iotlab.light.vo.LightVOImpl, " + "       org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl-org.tno.iotlab.temperatuur.vo.HumidityVOImpl-org.tno.iotlab.moist.vo.MoistVOImpl,,"
                         + "       org.tno.iotlab.temperatuur.vo.TemperatuurVOImpl-org.tno.iotlab.temperatuur.vo.HumidityVOImpl-org.tno.iotlab.moist.vo.MoistVOImpl")
                List<String>
                pids();

    }

    private Config config;
    private Connection connection;
    private ConnectionFactory connectionFactory;
    private volatile boolean running;
    private IoTProtocolDriver voLifeCycleManger;
    private final int FINISH_CHARACTER = 13;

    @Reference
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) throws IOException {

        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            voLifeCycleManger = new IoTProtocolDriver(config.addresses(), config.pids(), configurationAdmin);

            connection = connectionFactory.openSerialConnection(config.port_name(),
                                                                new SerialConnectionOptions(Baudrate.B57600,
                                                                                            Databits.D8,
                                                                                            Stopbits.S1,
                                                                                            Parity.None));
            running = true;
        } catch (Exception e) {
            System.err.println("afdsfad");
        }

        new Thread("IoT Protocol Driver") {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder(1024);
                Reader reader = new InputStreamReader(connection.getInputStream());
                try {
                    while (running) {
                        int nextChar = reader.read();
                        if (nextChar < 0) {
                            break;
                        } else if (nextChar == FINISH_CHARACTER) { // FIND THE RIGHT stop character!
                            String datagram = sb.toString();
                            receivedData(datagram);
                        } else {
                            sb.append((char) nextChar);
                        }
                    }
                } catch (IOException ex) {
                    log.error("I/O error while reading from smart meter", ex);
                } finally {
                    connection.close();
                }
            }
        }.start();
    }

    ConfigurationAdmin configurationAdmin;

    @Reference
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Deactivate
    public void deactivate() {
        running = false;
    }

    public void receivedData(String data) {
        log.debug("Driver Impl ontvangt: " + data);
        voLifeCycleManger.newDataReceived(data);
    }

}
