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
import org.jivesoftware.smackx.muc.Occupant;

/**
 *
 * @author giovanni
 */
public class ForceHBaseCheckpointCommand extends CleverCommand {

    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }

    @Override
    public void exec(CommandLine commandLine) {
        try {
            ArrayList params = new ArrayList();
            String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
            ClusterManagerAdministrationTools.instance().execAdminCommand(this, target, "HBaseManagerAgent", "forceCheckpoint", params, commandLine.hasOption("xml"));
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
        System.out.println("Available HMs checkpoint successfully made.");
        System.out.println("Number of HMs available to launch HBase, Hadoop and Zookeeper nodes: " + (String)response);
    }

    @Override
    public void handleMessageError(CleverException e) {
        System.out.println("Checkpoint failed.");
        System.out.println(e.getMessage());
    }
    
}
