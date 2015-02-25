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
package org.clever.HostManager.HBaseLauncher;

import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.Agent;
import static org.clever.Common.Communicator.Agent.logger;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;

/**
 *
 * @author giovanni
 */
public class HBaseLauncherAgent extends Agent {

    private HBaseLauncherPlugin plugin;
    private Class cl;
    
    public HBaseLauncherAgent() {
        super();
        logger = Logger.getLogger("HBaseLauncherAgent");
    }
    
    @Override
    public void initialization() throws Exception {
        if(super.getAgentName().equals("NoName")) {
            super.setAgentName("HBaseLauncherAgent");
        }
        super.start();
        try {
            InputStream inxml = getClass().getResourceAsStream("/org/clever/HostManager/HBaseLauncher/configuration_hbaseLauncher.xml"); 
            FileStreamer fs = new FileStreamer();
            ParserXML pars = new ParserXML(fs.xmlToString(inxml));
            
            this.cl = Class.forName(pars.getElementContent("HBaseLauncherPlugin"));
            this.plugin = (HBaseLauncherPlugin) cl.newInstance();
            this.plugin.setOwner (this); //probabilmente non necessaria se l'agente non deve richiamare funzionalità di altri agenti
            this.plugin.setLogger(logger);
            this.plugin.setNetworkInterface(pars.getElementContent("networkInterface"));
            this.plugin.setIsIPv4Preferred(Boolean.parseBoolean(pars.getElementContent("prefereIPv4")));
            this.plugin.setisIPv6Used(Boolean.parseBoolean(pars.getElementContent("useIPv6")));
            String filesDir = pars.getElementContent("filesDirectory");
            if (filesDir!=null && !filesDir.isEmpty()) 
                this.plugin.setFilesDirectory(filesDir);
            else
                this.plugin.setFilesDirectory(null);
            
            logger.info("HBaseLauncher istantiated!");
            
        } catch (Exception e) {
            logger.error("Error initializing HBaseLauncher: " + e.getMessage());
        }
        
    }

    @Override
    public Class getPluginClass() {
        return this.cl;
    }

    @Override
    public Object getPlugin() {
        return this.plugin;
    }

    @Override
    public void shutDown() {
        try {
            this.plugin.shutdown();
        } catch (CleverException ex) {
            logger.warn("Problem occurred while shutting down HBaseLauncher plugin: "+ex.getMessage());
        } catch (IOException ex1) {
            logger.warn("Problem occurred while shutting down HBaseLauncher plugin: "+ex1.getMessage());
        }
    }
}
