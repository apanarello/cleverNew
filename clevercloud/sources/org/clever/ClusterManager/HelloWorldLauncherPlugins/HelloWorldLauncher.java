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
package org.clever.ClusterManager.HelloWorldLauncherPlugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.clever.ClusterManager.Federation.FederationListenerAgent;
import org.clever.Common.Federation.FederationReply;
import org.clever.ClusterManager.HelloWorldLauncher.HelloWorldLauncherAgent;
import org.clever.ClusterManager.HelloWorldLauncher.HelloWorldLauncherPlugin;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Communicator.CmAgent;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.Common.smack.packet.Presence;
import org.clever.Common.smackx.muc.Occupant;
import org.apache.log4j.Logger;

/**
 *
 * @author giovanni
 */
public class HelloWorldLauncher implements HelloWorldLauncherPlugin {

    Agent owner;
    Logger logger;
    final String agentName = "FederationListenerAgent";
    final String federationNode = "Federation";
    final String CMtag = "CM";
    final String domainAttribute = "domain";
    
    @Override
    public void setOwner(Agent owner) {
        this.owner = owner;
    }
    
    @Override
    public void setLogger (Logger logger) {
        this.logger = logger;
    }

    @Override
    public String launchHelloWorld() {
        String result = null;
        try {
            result = (String) ((CmAgent)this.owner).remoteInvocation("clever1.dominioA.it","HelloWorldAgent", "runHelloWorld", true, new ArrayList());
            ((HelloWorldLauncherAgent)this.owner).getLogger().debug("risultato del lancio del metodo runHelloWorld: " + result);
        } catch (CleverException ex) {
            ((HelloWorldLauncherAgent)this.owner).getLogger().error(ex);
        }
        return result;
    }

    @Override
    public Object forwardHelloWorld(Integer hop) {
        if (hop.intValue()==0)
            return this.launchHelloWorld();
        else {
            ((HelloWorldLauncherAgent)this.owner).getLogger().debug("HOP = "+hop+": Inoltro HelloWorld ad un altro CM federato...");
            ArrayList params = new ArrayList<Object>();
            params.add(new Integer (hop.intValue() - 1));
            try {
                ArrayList<Object> params1 = new ArrayList<Object>();
                params1.add("dominioA.it");
                params1.add("HelloWorldLauncherAgent");
                params1.add("forwardHelloWorld");
                params1.add(true);
                params1.add(params);
                return this.owner.invoke("FederationListenerAgent", "forwardCommandToDomain", true, params1);
            } catch (CleverException ex) {
                ((HelloWorldLauncherAgent)this.owner).getLogger().error("Error forwarding command: "+ex.getMessage());
            }
        }
        return null; //Se arrivo qui è stata lanciata l'eccezione
    }

    @Override
    public String tryDbSedna() {
        String result = "RISULTATO DI tryDbSedna()";
        try {
            ArrayList<Object> params = new ArrayList<Object>();
            boolean exist = false;
            //Provo se esiste l'HadoopNamenodeAgent
            params.clear();
            exist = false;
            params.add("HadoopNamenodeAgent");
            params.add("");
            exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "checkAgentNode", true, params)).booleanValue();
            if (exist)
                result += "\nHadoopNamenodeAgent trovato";
            else
                result += "\nHadoopNamenodeAgent non trovato";
            //Provo se esiste l'il nodo HadoopStorage
            params.clear();
            exist = false;
            params.add("HadoopNamenodeAgent");
            params.add("/HadoopStorage");
            exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "checkAgentNode", true, params)).booleanValue();
            if (exist)
                result += "\nHadoopStorage trovato";
            else {
                result += "\nHadoopStorage non trovato";
                // creo il nodo HadoopStorage
                params.clear();
                params.add("HadoopNamenodeAgent");
                String nodo = "<HadoopStorage/>";
                params.add(nodo);
                params.add("into");
                params.add("");
                this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
                result += "\nHadoopStorage inserito";
                //Provo ora se esiste l'il nodo HadoopStorage
                params.clear();
                exist = false;
                params.add("HadoopNamenodeAgent");
                params.add("/HadoopStorage");
                exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "checkAgentNode", true, params)).booleanValue();
                if (exist)
                    result += "\nHadoopStorage trovato stavolta";
                else {
                    result += "\nHadoopStorage non trovato neanche stavolta";
                    return result;
                }
            }
            //Provo se esiste l'il nodo File dentro HadoopStorage
            params.clear();
            exist = false;
            params.add("HadoopNamenodeAgent");
            params.add("/HadoopStorage/File");
            exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
            if (exist)
                result += "\nFile trovato dentro HadoopStorage";
            else {
                result += "\nFile non trovato dentro HadoopStorage";
                // creo il nodo HadoopStorage
                params.clear();
                params.add("HadoopNamenodeAgent");
                String nodo = "<File/>";
                params.add(nodo);
                params.add("into");
                params.add("/HadoopStorage");
                this.owner.invoke("DatabaseManagerAgent", "insertNode", true, params);
                result += "File inserito in HadoopStorage";
                //Provo ora se esiste l'il nodo File dentro HadoopStorage
                params.clear();
                exist = false;
                params.add("HadoopNamenodeAgent");
                params.add("/HadoopStorage/File");
                exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
                if (exist)
                    result += "\nFile trovato stavolta dentro HadoopStorage";
                else {
                    result += "\nFile non trovato neanche stavolta dentro HadoopStorage";
                    return result;
                }
            }
            //Cancello il nodo File da HadoopStorage
            params.clear();
            params.add("HadoopNamenodeAgent");
            params.add("/HadoopStorage/File");
            this.owner.invoke("DatabaseManagerAgent", "deleteNode", true, params);
            result += "\nCancellato nodo File da HadoopStorage";
            //Provo ora se esiste l'il nodo File dentro HadoopStorage
            params.clear();
            exist = false;
            params.add("HadoopNamenodeAgent");
            params.add("/HadoopStorage/File");
            exist = ((Boolean)this.owner.invoke("DatabaseManagerAgent", "existNode", true, params)).booleanValue();
            if (exist)
                result += "\nFile trovato stavolta dentro HadoopStorage";
            else
                result += "\nFile non trovato neanche stavolta dentro HadoopStorage";
            
            
            
            
            //Stampo il risultato
            params.clear();
            params.add("HadoopNamenodeAgent");
            params.add("");
            params.add("");
            result += "\n" + (String)this.owner.invoke("DatabaseManagerAgent", "getContentNodeXML", true, params);
        } catch (CleverException ex) {
            ((HelloWorldLauncherAgent)this.owner).getLogger().error("Errore nella prova del DatabaseManager: "+ex.getMessage());
        }
        return result;
    }
    
    //inutile
    public FederationReply forwardCommand (final String target, final String agent, final String command, final boolean hasReply, final List params) throws CleverException{
        return null;
    }

    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    
    
    
}
