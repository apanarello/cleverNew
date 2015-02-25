/*
 * The MIT License
 *
 * Copyright 2013 giovanni.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.clever.ClusterManager.HelloWorldLauncher;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import static org.clever.Common.Communicator.Agent.logger;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Communicator.Notification;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;

/**
 *
 * @author giovanni
 */
public class HelloWorldLauncherAgent extends CmAgent {

    private Class cl;
    private HelloWorldLauncherPlugin helloWorldLauncherPlugin;
    
    public HelloWorldLauncherAgent() {
        super();
        logger = Logger.getLogger("HelloWorldLauncherAgent");
    }
    
    @Override
    public void initialization() throws Exception {
        if (super.getAgentName().equals("NoName")) {
            super.setAgentName("HelloWorldLauncherAgent");
        }
        super.start();
        try {
            logger.info("Reading configuration HelloWorldLauncher");
            InputStream inxml = getClass().getResourceAsStream("/org/clever/ClusterManager/HelloWorldLauncher/configuration_helloWorldLauncher.xml");
            FileStreamer fs = new FileStreamer();
            ParserXML pars = new ParserXML (fs.xmlToString(inxml));
            logger.debug("inxml: " + inxml);
            this.cl = Class.forName(pars.getElementContent("HelloWorldLauncherPlugin"));
            this.helloWorldLauncherPlugin = (HelloWorldLauncherPlugin) this.cl.newInstance();
            this.helloWorldLauncherPlugin.setOwner(this);
            this.helloWorldLauncherPlugin.setLogger(logger);
            logger.info("HelloWorldLauncher plugin instantiated !");
            //PROVA
            //logger.info(this.invoke("HelloWorldLauncherAgent", "tryDbSedna", true, new ArrayList()));
        } catch (Exception e) {
            logger.error("Error initializing HelloWorldLauncher : " + e.getMessage());
        }
    }
    
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Class getPluginClass() {
        return this.cl;
    }

    @Override
    public Object getPlugin() {
        return this.helloWorldLauncherPlugin;
    }

    @Override
    public void shutDown() {
        //non fa nulla;
    }

    @Override
    public void handleNotification(Notification notification) throws CleverException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
