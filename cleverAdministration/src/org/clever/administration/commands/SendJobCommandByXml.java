/*
 *  Copyright (c) 2014 Alfonso Panarello
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
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.S3tools.S3Tools;
import org.clever.Common.XMPPCommunicator.ConnectionXMPP;
import org.clever.administration.ClusterManagerAdministrationTools;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import timestamp.Timestamper;

/**
 *
 * @author apanarello
 */
public class SendJobCommandByXml extends CleverCommand {

    Logger log = Logger.getLogger("SendCommandLogger");
    private final String commandName = "sendJobXml";
    private final String agentName = "HadoopNamenodeAgent";
    // private final String  agentName = "HadoopJobtrackerAgent";
    private final String methodName = "sendJob";
    private final String[] argNames = {"Absolute XML local Path of conf command"};
    private S3Tools s3t = null;
    private File file = null;
    SAXBuilder builder = null;
    private Element rootElement;
    private Document document;

    @Override
    public Options getOptions() {

        Options options = new Options();
        options.addOption("debug", false, "Displays debug information.");
        return options;
    }

    @Override
    public void exec(CommandLine commandLine) {
        String[] args = commandLine.getArgs();

        if (args.length != 2) {
            System.out.println("Parametri di input " + (args.length - 1));
            System.out.println(this.getMenu());
        } else {
            builder = new SAXBuilder();
            String srcPath = args[1];
            File srcFile = new File(srcPath);
            if (srcFile.exists()) {
                try {
                    Timestamper.write("T01-lancioSendJobDaShell");
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
                try {
                    document = (Document) builder.build(srcFile);
                } catch (JDOMException ex) {
                    logger.error("Errore builder - Jdom :", ex);
                } catch (IOException ex) {
                    logger.error("Errore builder - IO :", ex);
                }
                rootElement = document.getRootElement();
                file = new File(rootElement.getChildText("S3credential_path").toString());
                FileReader reader = null;
                try {
                    reader = new FileReader(file);
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
                    //2=job, 4=Bucket, 3=fileS3 ,5=user,6=pass
                    params.add(rootElement.getChildText("job_type"));
                    params.add(rootElement.getChildText("bucket"));
                    params.add(rootElement.getChildText("file_name"));
                    params.add(rootElement.getChildText("user"));
                    params.add(rootElement.getChildText("pass"));

                    /* if (args.length == argNames.length) //manca il timeout
                     params.add("-1");
                    */
                    params.add(new Boolean(true)); //7=Forwardable
                    /*
                    s3t = new S3Tools();
                     s3t.getAuthWithPath(args[1]);
                     long size;
                     size = s3t.getInfo(args[3]);
                     params.add(size); //8=size S3 file
                    */
                    //Map<String, Integer> domResources = new HashMap<String, Integer>();
                    //ArrayList listDomains = new ArrayList();
/*
                    Element child = rootElement.getChild("domains");
                    List listOfTags = child.getChildren();
                    int num = listOfTags.size();
                    String domResources[][] = new String[(num) / 2][];
                    for (int k = 0; k < (num) / 2; k++) {
                        domResources[k] = new String[2];
                    }
                    int j = 0;
                    System.out.println("Numero Domini = " + listOfTags.size());
                    
                    for (int i = 0; i < listOfTags.size(); i = i + 2) { /*Adding couple domains resoures

                        domResources[j][0] = ((Element) listOfTags.get(i)).getText();
                        System.out.println("Elemento " + i + " della lista di domini \n");
                        System.out.println(((Element) listOfTags.get(i)).getText());
                        domResources[j][1] = ((Element) listOfTags.get(i + 1)).getText();
                        System.out.println("Value " + i + " della lista di domini \n");
                        System.out.println(((Element) listOfTags.get(i + 1)).getText());

                        j++;
                        // listDomains.add(args[i]);

                    }
                    params.add(domResources);
*/
                    for (int m = 0; m < params.size(); m++) {
                        System.out.println("Parametri della lista sono : " + params.get(m));

                    }
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
