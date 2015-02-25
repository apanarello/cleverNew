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
package org.clever.HostManager.HelloWorld;

import java.io.InputStream;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.FileStreamer;
import org.clever.Common.XMLTools.ParserXML;

/**
 *
 * @author giovanni
 */
public class HelloWorldAgent extends Agent {

    private HelloWorldPlugin helloWorldPlugin;
    private Class cl;
    
    public HelloWorldAgent() {
        super();
        logger = Logger.getLogger("HelloWorldAgent");
    }
    
    @Override
    public void initialization() throws CleverException {
        if(super.getAgentName().equals("NoName")) {
            super.setAgentName("HelloWorldAgent");
        }
        super.start();
        try {
            InputStream inxml = getClass().getResourceAsStream("/org/clever/HostManager/HelloWorld/configuration_helloWorld.xml"); 
            FileStreamer fs = new FileStreamer();
            ParserXML pars = new ParserXML(fs.xmlToString(inxml));
            
            this.cl = Class.forName(pars.getElementContent("HelloWorldPlugin"));
            this.helloWorldPlugin = (HelloWorldPlugin) cl.newInstance();
            this.helloWorldPlugin.setOwner (this); //probabilmente non necessaria se l'agente non deve richiamare funzionalità di altri agenti
            
            logger.info("HelloWorldPlugin istantiated!");
        } catch (Exception e) {
            logger.error("Error initializing HelloWorld: " + e.getMessage());
        }
    }
    
    @Override
    public Class getPluginClass() {
        return this.cl;
    }

    @Override
    public Object getPlugin() {
        return this.helloWorldPlugin;
    }

    @Override
    public void shutDown() {
        //non fa nulla
    }
    
}
