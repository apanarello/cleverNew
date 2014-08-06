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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;
import timestamp.Timestamper;

/**
 *
 * @author Giovanni Volpintesta
 */
public class PutFileCommand extends CleverCommand {
    
    private final String commandName = "putFile";
    private final String agentName = "HadoopNamenodeAgent";
    private final String methodName = "putFile";
    private final String[] argNames = {"local path of source file", "remote path of destination file", "user", "password", "timeout (optional)"};
    
    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }
    
    @Override
    public void exec(CommandLine commandLine) {
        String[] args = commandLine.getArgs();
        if (args.length!=this.argNames.length && args.length!=this.argNames.length+1) { //perchè l'ultimo è opzionale
            System.out.println(this.getMenu());
        } else {
            String srcPath = args[1];
            File srcFile = new File(srcPath);
            if (srcFile.exists()) {
                try {
                    Timestamper.write("P01-lancioPutFileDaShell");
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
                FileReader reader = null;
                try {
                    reader = new FileReader (srcFile);
                } catch (FileNotFoundException ex) {} //non accade perchè faccio il controllo prima
                try {
                    StringWriter writer = new StringWriter();
                    int c = -1;
                    while (true) {
                        try {
                            c = reader.read();
                        } catch (IOException ex) {
                            System.err.println(ex);
                        }
                        if (c == -1)
                            break;
                        else
                            writer.write(c);
                    }
                    String fileBuffer = writer.toString();
                    try {
                        reader.close();
                        writer.close();
                    } catch (IOException ex) {
                        System.err.println(ex);
                    }
                    ArrayList params = new ArrayList();
                    params.add(fileBuffer);
                   /* if (args.length == argNames.length) { //manca il timeout
                        for (int i=2; i<args.length; i++)
                            params.add(args[i]);
                        params.add(new Boolean(true)); //forwardable because it's the first launch.
                    } else { //c'è il timeout
                        for (int i=2; i<args.length-1; i++)
                            params.add(args[i]);
                        params.add(new Boolean(true)); //forwardable because it's the first launch.
                        params.add(Long.getLong(args[args.length-1]));
                    } */
                    for (int i=2; i<args.length; i++)
                        params.add(args[i]);
                    if (args.length == argNames.length) //manca il timeout
                        params.add("-1");
                    params.add(new Boolean(true)); //forwardable because it's the first launch.
                    String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
                    try {
                        Timestamper.write("P02-lancioPutFileDaClientSuCM");
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                    }
                    ClusterManagerAdministrationTools.instance().execAdminCommand(this, target, this.agentName, this.methodName, params, commandLine.hasOption("xml"));
                } catch ( CleverException ex ) {
                    if(commandLine.hasOption("debug")) {
                        ex.printStackTrace();
                    } else
                        System.out.println(ex);
                        logger.error( ex );
                }
            } else {
                System.out.println("Source File doesn't exist");
            }
        }
    }

    @Override
    public void handleMessage(Object response) {
        try {
            Timestamper.write("T21-SendJobDaClientSuCMEseguitoCorrettamente");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("\""+this.commandName+"\" command successfully launched");
    }

    @Override
    public void handleMessageError(CleverException e) {
        try {
            Timestamper.write("P21B-putFileDaClientSuCMFallito");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
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
