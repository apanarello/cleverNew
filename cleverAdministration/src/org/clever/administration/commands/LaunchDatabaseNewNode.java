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
public class LaunchDatabaseNewNode extends CleverCommand {
    
    public static final String FORMAT_HDFS = "format_hdfs";
    public static final String NAMENODE = "namenode";
    public static final String SECONDARY_NAMENODE = "secondary";
    public static final String DATANODE = "datanode";
    public static final String ZOOKEEPER = "zookeeper_quorum";
    public static final String MASTER = "master";
    public static final String REGIONSERVER = "regionserver";
    public static final String HELP = "Usage: launchDBNode [node]\nAccepted values for [node]:\n"+LaunchDatabaseNewNode.FORMAT_HDFS+"   "+LaunchDatabaseNewNode.NAMENODE+"   "+LaunchDatabaseNewNode.SECONDARY_NAMENODE+"   "+LaunchDatabaseNewNode.DATANODE+"   "+LaunchDatabaseNewNode.ZOOKEEPER+"   "+LaunchDatabaseNewNode.MASTER+"   "+LaunchDatabaseNewNode.REGIONSERVER;
    public static final String COMMAND_ERROR = "Error launching comand.\n"+LaunchDatabaseNewNode.HELP;
    
    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }

    @Override
    public void exec(CommandLine commandLine) {
        String args[] = commandLine.getArgs();
        if (args.length==2) {
            try {
                ArrayList params = new ArrayList();
                String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
                String agent = "HBaseManagerAgent";
                String method = null;
                if (args[1].compareTo(LaunchDatabaseNewNode.FORMAT_HDFS)==0)
                    method = "formatFS";
                else if (args[1].compareTo(LaunchDatabaseNewNode.NAMENODE)==0)
                    method = "startNamenode";
                else if (args[1].compareTo(LaunchDatabaseNewNode.SECONDARY_NAMENODE)==0)
                    throw new CleverException ("OPZIONE ANCORA NON SUPPORTATA");
                else if (args[1].compareTo(LaunchDatabaseNewNode.DATANODE)==0)
                    method = "startDatanode";
                else if (args[1].compareTo(LaunchDatabaseNewNode.ZOOKEEPER)==0)
                    method = "startZookeeperQuorum";
                else if (args[1].compareTo(LaunchDatabaseNewNode.MASTER)==0)
                    method = "startHBaseMaster";
                else if (args[1].compareTo(LaunchDatabaseNewNode.REGIONSERVER)==0)
                    method = "startRegionserver";
                else 
                    System.out.println(LaunchDatabaseNewNode.COMMAND_ERROR);
                if (method!=null)
                    ClusterManagerAdministrationTools.instance().execAdminCommand(this, target, agent, method, params, commandLine.hasOption("xml"));
            } catch ( CleverException ex ) {
                if(commandLine.hasOption("debug")) {
                    System.out.println(ex.getMessage());
                } else
                    System.out.println(ex);
                logger.error( ex );
            }
        } else
            System.out.println(LaunchDatabaseNewNode.COMMAND_ERROR);
    }

    @Override
    public void handleMessage(Object response) {
        System.out.println("Node successfully launched.");
    }

    @Override
    public void handleMessageError(CleverException e) {
        System.out.println(e.getMessage());
    }
    
}
