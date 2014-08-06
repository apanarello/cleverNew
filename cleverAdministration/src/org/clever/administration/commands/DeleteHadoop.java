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


public class DeleteHadoop extends CleverCommand{
    
    @Override
    public Options getOptions(){
        Options options = new Options();
        options.addOption("pt", true, "The path of the file");
        return options;
    }
     
    @Override
    public void exec( CommandLine commandLine ){
        Object returnResponse;
        
        try {
            String path=commandLine.getOptionValue("pt");
            ArrayList params = new ArrayList();
            params.add(path);
                
            String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);

            if (!target.equals("")) {
                returnResponse=(String)ClusterManagerAdministrationTools.instance().execSyncAdminCommand(this, target, "HadoopNamenodeAgent", "deleteFile", params, commandLine.hasOption("xml"));
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

    
    