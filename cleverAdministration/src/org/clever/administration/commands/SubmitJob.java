/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clever.administration.commands;

/**
 *
 * @author cristina
 */

import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;

public class SubmitJob extends CleverCommand {
    
    public Options getOptions(){
        Options options = new Options();
        options.addOption("src", true, "The source path");
        options.addOption("dst", true, "The destination path");
        return options;
    }
     
    @Override
    public void exec( CommandLine commandLine ){
        Object returnResponse;
        
        try {
            String source=commandLine.getOptionValue("src");
            String dest=commandLine.getOptionValue("dst");
            ArrayList params = new ArrayList();
            params.add(source);
            params.add(dest);
                        
            String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);

            if (!target.equals("")) {
                returnResponse=(String)ClusterManagerAdministrationTools.instance().execSyncAdminCommand(this, target, "HadoopNamenodeAgent", "submitJob", params, commandLine.hasOption("xml"));
                System.out.println(returnResponse);
                
            }
        }
        catch (CleverException ex) {
            logger.error(ex);
        }
    }

    @Override
    public void handleMessage( Object response ){
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    @Override
    public void handleMessageError(CleverException response) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    
    
}
