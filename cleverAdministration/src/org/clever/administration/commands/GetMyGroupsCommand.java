/*
 *  Copyright (c) 2014 Giovanni Volpintesta
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
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
 * @author Giovanni Volpintesta
 */
public class GetMyGroupsCommand extends CleverCommand {
    
    private final String commandName = "getMyGroups";
    private final String agentName = "HadoopNamenodeAgent";
    private final String methodName = "getMyGroups";
    private final String[] argNames = {"user", "password"};
    
    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }
    
    @Override
    public void exec(CommandLine commandLine) {
        String[] args = commandLine.getArgs();
        if (args.length < this.argNames.length + 1) {
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
        String result = "GROUPS: \n";
        ArrayList<String[]> reply = (ArrayList<String[]>) response;
        for (String[] group : reply) {
            result += "\nID: "+group[0]+"      DESCRIPTION: "+group[1];
        }
        System.out.println(result);
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
