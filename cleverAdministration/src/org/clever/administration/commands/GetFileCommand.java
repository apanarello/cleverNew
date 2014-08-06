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
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
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
public class GetFileCommand extends CleverCommand {
    
    private final String commandName = "getFile";
    private final String agentName = "HadoopNamenodeAgent";
    private final String methodName = "getFile";
    private final String[] argNames = {"remote path of the source file", "local path of destination file", "user", "password", "timeout (optional)"};
    private static String dstFileName;
    
    @Override
    public Options getOptions() {
        Options options = new Options();
        options.addOption( "debug", false, "Displays debug information." );
        return options;
    }
    
    @Override
    public void exec(CommandLine commandLine) {
        String[] args = commandLine.getArgs();
        if (args.length!=this.argNames.length && args.length!=this.argNames.length+1) { //il timeout è opzionale
            System.out.println(this.getMenu());
        } else {
            try {
                Timestamper.write("G01-lancioGetFileDaShell");
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            GetFileCommand.dstFileName = args[2];
            File dstFile = new File(GetFileCommand.dstFileName);
            boolean exists;
            if (dstFile.exists()) {
                exists = true;
            } else {
                try {
                    dstFile.createNewFile();
                    exists = true;
                } catch (IOException ex) {
                    System.out.println("Destination file cannot be created: "+ex);
                    exists = false;
                }
            }
            if (exists) {
                try {
                    ArrayList params = new ArrayList();
                    params.add(args[1]);
                    params.add(args[3]);
                    params.add(args[4]);
                    if (args.length==6) {
                        //params.add(Long.getLong(args[5]));
                        params.add(args[5]);
                    } else { //manca il timeout
                        params.add("-1");
                    }
                    String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
                    try {
                        Timestamper.write("G02-lancioGetFileDaClientSuCM");
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
            }
        }
    }

    @Override
    public void handleMessage(Object response) {
        try {
            Timestamper.write("G22-getFileDaClientSuCMEseguitoCorrettamente");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("\""+this.commandName+"\" command successfully launched");
        File dstFile = new File (GetFileCommand.dstFileName);
        try {
            Timestamper.write("G23-inizioCreazioneFileRitornatoSulClient(GET)");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        if (dstFile.exists()) {
            StringReader reader = new StringReader((String)response);
            try {
                FileWriter writer = new FileWriter(dstFile);
                while (true) {
                    int c = reader.read();
                    if (c == -1)
                        break;
                    else writer.write(c);
                }
                reader.close();
                writer.close();
            } catch (IOException ex) {
                try {
                    Timestamper.write("G24B-fallitaCreazioneFileRitornatoSulClient(GET)");
                } catch (IOException ex1) {
                    System.out.println(ex1.getMessage());
                }
                System.out.println(ex);
            }
        }
        try {
            Timestamper.write("G24-riuscitaCreazioneFileRitornatoSulClient(GET)");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void handleMessageError(CleverException e) {
        try {
            Timestamper.write("G22B-getFileDaClientSuCMFallito");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("Failed launching \""+this.commandName+"\" command");
        System.out.println(e);
        File dstFile = new File(GetFileCommand.dstFileName);
        if (dstFile.exists())
            dstFile.delete();
    }
    
    private String getMenu() {
        String menu = "Usage of \"" + this.commandName + "\" command:"
                + "\n"+this.commandName;
        for (String arg : this.argNames)
            menu += " ["+arg+"]";
        return menu;
    }
}
