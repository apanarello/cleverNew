/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clever.administration.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;
import timestamp.Timestamper;
import org.clever.Common.S3tools.S3Tools;

/**
 *
 * @author apanarello
 */
public class SendJobCommand extends CleverCommand {

    Logger log = Logger.getLogger("SendCommandLogger");
    private final String commandName = "sendJob";
    private final String agentName = "HadoopNamenodeAgent";
    // private final String  agentName = "HadoopJobtrackerAgent";
    private final String methodName = "sendJob";
    private final String[] argNames = {"local Path file Rootkey", "job name", "bucketName","S3fileName", "user", "pass", "one or more domains"};
    private S3Tools s3t=null;
    @Override
    public Options getOptions() {

        Options options = new Options();
        options.addOption("debug", false, "Displays debug information.");
        return options;
    }

    @Override
    public void exec(CommandLine commandLine) {
        String[] args = commandLine.getArgs();
        if (args.length <= 5) {
            System.out.println("Parametri di input "+(args.length-1));
            System.out.println(this.getMenu());
        } else {
            
            String srcPath = args[1];
            File srcFile = new File(srcPath);
            if (srcFile.exists()) {
                try {
                    Timestamper.write("T01-lancioSendJobDaShell");
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
                FileReader reader = null;
                try {
                    reader = new FileReader(srcFile);
                } catch (FileNotFoundException ex) {
                }
                try {
                    StringWriter writer = new StringWriter();
                    int c = -1;
                    while (true) {
                        try {
                            c = reader.read();
                        } catch (IOException ex) {
                            System.err.println(ex);
                        }
                        if (c == -1) {
                            break;
                        } else {
                            writer.write(c);
                        }
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
                    for (int i = 2; i <= 6; i++) //2=job, 4=Bucket, 3=fileS3 ,5=user,6=pass
                    {
                        params.add(args[i]);
                    }
                    /* if (args.length == argNames.length) //manca il timeout
                     params.add("-1");*/
                    params.add(new Boolean(true)); //7=Forwardable
                    /*s3t = new S3Tools();
                    s3t.getAuthWithPath(args[1]);
                    long size;
                    size = s3t.getInfo(args[3]);
                    params.add(size); //8=size S3 file*/
                    //Map<String, Integer> domResources = new HashMap<String, Integer>();
                    //ArrayList listDomains = new ArrayList();
                    String domResources[][]=new String [(args.length-7)/2][];
                    for(int k=0;k<(args.length-7)/2;k++){
                    domResources[k]=new String[2];
                    }
                    int j=0;
                    for (int i = 7; i < args.length; i = i + 2) { /*Adding couple domains resoures*/
                        
                        domResources[j][0]=args[i];
                        System.out.println(i);
                        domResources[j][1]=args[i+1];
                         System.out.println(domResources[j][0]);
                         System.out.println(domResources[j][1]);
                        j++;
                           // listDomains.add(args[i]);

                    }
                    params.add(domResources);
                    String target = ClusterManagerAdministrationTools.instance().getConnectionXMPP().getActiveCC(ConnectionXMPP.ROOM.SHELL);
                    try {
                        Timestamper.write("T02-lancioSendJobDaClientSuCM");
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                    }
                    ClusterManagerAdministrationTools.instance().execAdminCommand(this, target, this.agentName, this.methodName, params, commandLine.hasOption("xml"));

                } catch (CleverException ex) {
                    System.err.println(ex);
                }

            } else {
                System.out.println("RootKey file doesn't exist");
            }
        }

    }

    @Override
    public void handleMessage(Object response) {

        try {
            Timestamper.write("T21-sendJobDaClientSuCMEseguitoCorrettamente");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("\"" + this.commandName + "\" command successfully launched");
        

    }

    @Override
    public void handleMessageError(CleverException e) {
        try {
            Timestamper.write("T21B-sendJobDaClientSuCMFallito");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("Failed launching \"" + this.commandName + "\" command");
        System.out.println(e);
    }

    private String getMenu() {

        String menu = "Usage of \"" + this.commandName + "\" command:"
                + "\n" + this.commandName;
        for (String arg : this.argNames) {
            menu += " [" + arg + "]";
        }
        return menu;
    }
    
}
