package nl.tno.fpai.driver.plugwise.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Power;
import javax.measure.unit.SI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nl.tno.fpai.driver.plugwise.api.PlugWiseResourceDriver.PlugWiseResourceState;

import org.flexiblepower.time.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Component which polls a configured URL at a configurable interval (see {@link Config}) and emits the states polled to
 * one or more {@link WebAPIHandler}s.
 */
@Component(designateFactory = WebAPIProtocolDriver.Config.class, immediate = true, provide = {})
public class WebAPIProtocolDriver implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ScheduledExecutorService executor;
    private TimeService timeService;

    /** The future / handle for later cancellation (see {@link #deactivate()}). */
    private ScheduledFuture<?> pollTask;

    private Config config;
    private URL monitoredURL;

    private final List<WebAPIHandler> handlers = new CopyOnWriteArrayList<WebAPIHandler>();

    /**
     * @param executor
     *            The executor service to use for (periodic) tasks which need to be scheduled.
     */
    @Reference
    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    /**
     * @param timeService
     *            The service for getting the current time.
     */
    @Reference
    public void setTimeService(TimeService timeService) {
        this.timeService = timeService;
    }

    /**
     * Activates this component. Starts polling the configured url.
     * 
     * @param properties
     *            see {@link Config}.
     */
    @Activate
    public void activate(Map<String, Object> properties) {
        config = Configurable.createConfigurable(Config.class, properties);

        try {
            monitoredURL = new URL(config.monitoredURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Monitored URL (" + config.monitoredURL() + ") is ill-formated.");
        }

        pollTask = executor.scheduleAtFixedRate(this, config.pollInterval(), config.pollInterval(), TimeUnit.SECONDS);
    }

    /**
     * Deactivates the component including the polling activity.
     */
    @Deactivate
    public void deactivate() {
        pollTask.cancel(false);
        pollTask = null;
    }

    /**
     * Add a handler to send the polled info to.
     * 
     * @param handler
     *            Handler of information polled from the monitored Plugwise URL.
     */
    @Reference(dynamic = true, multiple = true)
    public void addWebAPIHandler(WebAPIHandler handler) {
        handlers.add(handler);
    }

    /**
     * @param handler
     *            The handler to remove
     */
    public void removeWebAPIHandler(WebAPIHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void run() {
        try {
            // poll the URL
            String xml = poll();
            // parse XML into XML document
            Document document = parse(xml);
            // parse into java objects
            final Map<PlugwiseResourceId, PlugWiseResourceState> states = parse(document);

            logger.info("Succesfully polled Plugwise XML api, delegating handling of {} states to {} handlers",
                        states.size(),
                        handlers.size());

            // delegate to handlers
            for (final WebAPIHandler handler : handlers) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        handler.handle(states);
                    }
                });
            }
        } catch (Exception e) {
            logger.warn("An exception occurred while polling data from the PlugWise XML interface", e);
        }
    }

    private String poll() throws IOException {
        URLConnection conn = monitoredURL.openConnection();

        byte[] buffer = new byte[conn.getContentLength()];
        int read = 0;

        InputStream is = conn.getInputStream();

        try {
            while ((read += is.read(buffer)) < buffer.length) {
                // read on
            }
        } finally {
            is.close();
        }

        return new String(buffer);
    }

    private Document parse(String xml) throws ParserConfigurationException, SAXException, IOException {
        // work-around bug in the way Plugwise serves it's XML files
        xml = xml.replace("standalone=\"true\"", "standalone=\"yes\"");
        xml = xml.replace("standalone=\"false\"", "standalone=\"no\"");

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private Map<PlugwiseResourceId, PlugWiseResourceState> parse(Document doc) {
        Map<PlugwiseResourceId, PlugWiseResourceState> states = new HashMap<PlugwiseResourceId, PlugWiseResourceState>();
        XPath xpath = XPathFactory.newInstance().newXPath();

        try {
            // fetch all items
            NodeList items = (NodeList) xpath.evaluate("items/appliance", doc, XPathConstants.NODESET);

            // parse the fields for each appliance
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);

                // skip over the summary appliance (with id "0")
                String id = (String) xpath.evaluate("id", item, XPathConstants.STRING);
                if ("0".equals(id)) {
                    continue;
                }

                // {prefix}-{id}-{name} (whitespace in name is replaced with dashes, non-digits and non-word removed)
                String name = (String) xpath.evaluate("name", item, XPathConstants.STRING);

                // connected if power state is on or false otherwise
                String powerstate = (String) xpath.evaluate("powerstate", item, XPathConstants.STRING);
                boolean connected = "on".equals(powerstate);

                // demand is current power (stripped of W and , replaced by .)
                String currentPower = (String) xpath.evaluate("powerusageround", item, XPathConstants.STRING);
                currentPower = currentPower.replace(",", ".").replaceAll(" W$", "");
                Measurable<Power> demand = Measure.valueOf(Double.parseDouble(currentPower), SI.WATT);

                // add to the list of states
                states.put(new PlugwiseResourceId(id, name),
                           new PlugwiseResourceStateImpl(demand, timeService.getTime(), connected));
            }
        } catch (XPathExpressionException e) {
            logger.error("Couldn't parse the XML document from PlugWise", e);
        } catch (NumberFormatException e) {
            logger.error("Couldn't parse the current power value in a PlugWise state XML document", e);
        }

        return states;
    }

    /**
     * Configuration for the Plugwise Web API Protocol driver.
     */
    @OCD(name = "Plugwise Web API Protocol driver config")
    public interface Config {
        /** @return The url to poll the PlugWise XML interface. */
        @AD(deflt = "http://localhost:8080/api/data.xml?type=appliances",
            description = "The url to poll the PlugWise XML interface")
        String monitoredURL();

        /** @return Interval in seconds at which to poll the PlugWise XML interface. */
        @AD(deflt = "10", description = "Interval in seconds at which to poll the PlugWise XML interface")
        int pollInterval();
    }
}
