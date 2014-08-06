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

/**
 *
 * @author giovanni
 */
public class StopDatabaseNode extends CleverCommand {

    public static final String NAMENODE = "namenode";
    public static final String SECONDARY_NAMENODE = "secondary";
    public static final String DATANODE = "datanode";
    public static final String ZOOKEEPER = "zookeeper_quorum";
    public static final String MASTER = "master";
    public static final String REGIONSERVER = "regionserver";
    public static final String HELP = "Usage: stopDBNode [node]\nAccepted values for [node]:\n"+StopDatabaseNode.NAMENODE+"   "+StopDatabaseNode.SECONDARY_NAMENODE+"   "+StopDatabaseNode.DATANODE+"   "+StopDatabaseNode.ZOOKEEPER+"   "+StopDatabaseNode.MASTER+"   "+StopDatabaseNode.REGIONSERVER;
    public static final String COMMAND_ERROR = "Error launching comand.\n"+StopDatabaseNode.HELP;
    
    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }

    @Override
    public void exec(CommandLine commandLine) {
        String args[] = commandLine.getArgs();
        if (args.length==1) {
            try {
                ArrayList params = new ArrayList();
                String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
                String agent = "HBaseManagerAgent";
                String method = null;
                if (args[0].compareTo(StopDatabaseNode.NAMENODE)==0)
                    method = "stopNamenode";
                else if (args[0].compareTo(StopDatabaseNode.SECONDARY_NAMENODE)==0)
                    throw new CleverException ("OPZIONE ANCORA NON SUPPORTATA");
                else if (args[0].compareTo(StopDatabaseNode.DATANODE)==0)
                    method = "stopDatanode";
                else if (args[0].compareTo(StopDatabaseNode.ZOOKEEPER)==0)
                    method = "stopZookeeperQuorum";
                else if (args[0].compareTo(StopDatabaseNode.MASTER)==0)
                    method = "stopHBaseMaster";
                else if (args[0].compareTo(StopDatabaseNode.REGIONSERVER)==0)
                    method = "stopRegionserver";
                else 
                    System.out.println(StopDatabaseNode.COMMAND_ERROR);
                if (method!=null)
                    ClusterManagerAdministrationTools.instance().execSyncAdminCommand(this, target, agent, method, params, commandLine.hasOption("xml"));
            } catch ( CleverException ex ) {
                if(commandLine.hasOption("debug")) {
                    System.out.println(ex.getMessage());
                } else
                    System.out.println(ex);
                logger.error( ex );
            }
        } else
            System.out.println(StopDatabaseNode.COMMAND_ERROR);
    }

    @Override
    public void handleMessage(Object response) {
        System.out.println("Node successfully stopped.");
    }

    @Override
    public void handleMessageError(CleverException e) {
        System.out.println(e.getMessage());
    }
    
}
