
import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;
import org.clever.administration.commands.CleverCommand;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author giovanni
 */
public class ExistsFileCommand extends CleverCommand {
    
    private final String commandName = "existsFile";
    private final String agentName = "HadoopNamenodeAgent";
    private final String methodName = "existsFile";
    private final String[] argNames = {"remote path of the file", "user", "password"};
    
    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }
    
    @Override
    public void exec(CommandLine commandLine) {
        String[] args = commandLine.getArgs();
        if (args.length != this.argNames.length + 1) {
            System.out.println(this.getMenu());
        } else {
            try {
                ArrayList params = new ArrayList();
                for (int i=1; i<args.length; i++)
                    params.add(args[i]);
                String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
                ClusterManagerAdministrationTools.instance().execAdminCommand(this, target, this.agentName, this.methodName, params, commandLine.hasOption("xml"));
            } catch ( CleverException ex ) {
                if(commandLine.hasOption("debug")) {
                    ex.printStackTrace();
                } else
                    System.out.println(ex);
                    logger.error( ex );
            }
        }
    }

    @Override
    public void handleMessage(Object response) {
        System.out.println("\""+this.commandName+"\" command successfully launched");
        System.out.println(response);
    }

    @Override
    public void handleMessageError(CleverException e) {
        System.out.println("Failed launching \""+this.commandName+"\" command");
        System.out.println(e);
    }
    
    private String getMenu() {
        String menu = "Usage of \"" + this.commandName + "\" command:"
                + "\n"+this.commandName;
        for (String arg : this.argNames)
            menu += " ["+arg+"]";
        return menu;
    }
}
