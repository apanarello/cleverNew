/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clever.administration.commands;

import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;
import org.clever.Common.Federation.FederationReply;

/**
 *
 * @author giovanni
 */
public class HelloWorldForwardCommand extends CleverCommand{

    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }
    
    @Override
    public void exec(CommandLine commandLine) {
        try {
            ArrayList params = new ArrayList<Object>();
            String[] args = commandLine.getArgs();
            if (args.length==1)
                params.add(new Integer (1));
            else
                params.add(new Integer (Integer.parseInt(args[1])));
            String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
            ClusterManagerAdministrationTools.instance().execAdminCommand(this, target, "HelloWorldLauncherAgent", "forwardHelloWorld", params, commandLine.hasOption("xml"));
        } catch ( CleverException ex ) {
            if(commandLine.hasOption("debug")) {
                ex.printStackTrace();
            } else
                System.out.println(ex);
            logger.error( ex );
        }
    }

    @Override
    public void handleMessage(Object response) {
        System.out.println("HelloWorldForward command successfully launched");
        System.out.println(((String)response));
        //System.out.println(((FederationReply)response).getReply().toString());
    }

    @Override
    public void handleMessageError(CleverException e) {
        System.out.println("Failed launching HelloWorldForward command");
        System.out.println(e);
    }
    
}
