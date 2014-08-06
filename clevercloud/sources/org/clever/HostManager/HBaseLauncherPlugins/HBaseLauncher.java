/*
 * The MIT License
 *
 * Copyright 2013 giovanni.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.clever.HostManager.HBaseLauncherPlugins;

//TODO provare avvio e terminazione di zookeeper. La terminazione va fatta bene.

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.master.HMaster;
import org.apache.hadoop.hbase.regionserver.HRegionServer;
import org.apache.hadoop.hbase.zookeeper.HQuorumPeer;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.SecondaryNameNode;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.clever.ClusterManager.HBaseManagerPlugins.HostManagerNodes;
import org.clever.Common.Communicator.Agent;
import org.clever.Common.Exceptions.CleverException;
import org.clever.HostManager.HBaseLauncher.HBaseLauncherPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;










public class HBaseLauncher implements HBaseLauncherPlugin {
	
        public static final String ZOOKEEPER_CONFIGURATION_FILE_NAME = "zk-conf.xml";
    
        private Logger logger;
        private Agent owner;
        private String filesDirectory;
        
        public static final String NAMENODE_DIR_PROPERTY_NAME = "dfs.name.dir";
        public static final String NAMENODE_DIR = "name";
        public static final String SECONDARY_NAMENODE_DIR_PROPERTY_NAME = "fs.checkpoint.dir";
        public static final String SECONDARY_NAMENODE_DIR = "namesecondary";
        public static final String DATANODE_DIR_PROPERTY_NAME = "dfs.data.dir";
        public static final String DATANODE_DIR = "data";
        public static final String ZOOKEEPER_DIR_PROPERTY_NAME = "hbase.zookeeper.property.dataDir";
        public static final String ZOOKEEPER_DIR = "zookeeper";
        
        private String networkInterface = "eth0"; //valori di default
        private boolean isIPv4Preferred = true;
        private boolean isIPv6Used = false;
    
	//Le configurazioni non vengono conservate sull'host manager ma sul cluster manager
	
	//I nodi lanciati sull'host. Può essere lanciato solo un nodo di ogni tipo su ogni host
	private NameNode hadoopNamenode = null; //il namenode lanciato da questo HostManager
	private DataNode hadoopDatanode = null; //il datanode lanciato da questo HostManager
	private SecondaryNameNode hadoopSecondaryNamenode = null; //il secondary namenode lanciato da questo HostManager
	private boolean zookeeperPeerRunning = false; //indica se in questo HostManager è lanciato un peer Zookeeper
	private HMaster hbaseMaster = null; //il master di hbase lanciato da questo HostManager
	private HRegionServer hbaseRegionserver = null; //il regionserver lanciato da questo HostManager
	private Process zookeeperPeerProcess = null;
	
        
        
	public HBaseLauncher () {}
        
        private Configuration parseConfigurationXml (String xml) throws ParserConfigurationException, SAXException, IOException {
            this.logger.debug("HBASELAUNCHER DEBUG: parseConfiguration(): configurationString:\n"+xml);
            Configuration configuration = new Configuration(true);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
            doc.getDocumentElement().normalize(); //recommended
            Element rootelement = doc.getDocumentElement();
            NodeList properties = rootelement.getElementsByTagName("property");
            for (int i=0; i<properties.getLength(); i++) {
                Element property = (Element) properties.item(i);
                NodeList nameList = property.getElementsByTagName("name"); //può (e deve) essercene uno sono
                if (nameList.getLength()<1)
                    continue;
                NodeList valueList = property.getElementsByTagName("value"); //può (e deve) essercene uno sono
                if (valueList.getLength()<1)
                    continue;
                configuration.set(nameList.item(0).getTextContent(), valueList.item(0).getTextContent());
            }
            return configuration;
        }
	
        @Override
	public void startNamenode (String conf) throws Exception {
            (new NamenodeLauncher(this.parseConfigurationXml(conf), this)).exec();
	}
	
        @Override
	public void stopNamenode () throws IOException, CleverException {
		(new NamenodeStopper(this)).exec();
	}
	
        @Override
	public void startDatanode (String conf) throws Exception {
		(new DatanodeLauncher(this.parseConfigurationXml(conf), this)).exec();
	}
	
        @Override
	public void stopDatanode () throws IOException, CleverException {
		(new DatanodeStopper(this)).exec();
	}
	
        @Override
	public void startSecondaryNamenode (String conf) throws Exception {
		(new SecondaryNamenodeLauncher(this.parseConfigurationXml(conf), this)).exec();
	}
	
        @Override
	public void stopSecondaryNamenode () throws IOException, CleverException {
		(new SecondaryNamenodeStopper(this)).exec();
	}
	
        @Override
	public void startZookeeperPeer (String conf) throws Exception {
		(new ZookeeperPeerLauncher(this.parseConfigurationXml(conf), this)).exec();
	}
	
        @Override
	public void stopZookeeperPeer () throws IOException, CleverException {
		(new ZookeeperPeerStopper(this)).exec();
	}
	
        @Override
	public void startHBaseMaster (String conf) throws Exception {
		(new HBaseMasterLauncher(this.parseConfigurationXml(conf), this)).exec();
	}
	
        @Override
	public void stopHBaseMaster () throws IOException, CleverException {
		(new HBaseMasterStopper(this)).exec();
	}
	
        @Override
	public void startRegionserver (String conf) throws Exception {
		(new RegionserverLauncher(this.parseConfigurationXml(conf), this)).exec();
	}
	
        @Override
	public void stopRegionserver () throws IOException, CleverException {
		(new RegionserverStopper(this)).exec();
	}
        
        @Override
        public void setOwner (Agent owner) {
            this.owner = owner;
        }
        
        @Override
        public void setLogger (Logger logger) {
            this.logger = logger;
        }
        
        public Logger getLogger () {
            return logger;
        }
        
        @Override
        public void shutdown () throws IOException, CleverException {
            if (this.getHadoopNamenode()!=null)
                this.stopNamenode();
            if (this.getHadoopSecondaryNamenode()!=null)
                this.stopSecondaryNamenode();
            if (this.getHadoopDatanode()!=null)
                this.stopDatanode();
            if (this.isZookeeperPeerRunning())
                this.stopZookeeperPeer();
            if (this.getHbaseMaster()!=null)
                this.stopHBaseMaster();
            if (this.getHbaseRegionserver()!=null)
                this.stopRegionserver();
        }
        
        protected NameNode getHadoopNamenode() {
		return hadoopNamenode;
	}

	protected void setHadoopNamenode(NameNode hadoopNamenode) {
		this.hadoopNamenode = hadoopNamenode;
	}

	protected DataNode getHadoopDatanode() {
		return hadoopDatanode;
	}

	protected void setHadoopDatanode(DataNode hadoopDatanode) {
		this.hadoopDatanode = hadoopDatanode;
	}

	protected SecondaryNameNode getHadoopSecondaryNamenode() {
		return hadoopSecondaryNamenode;
	}

	protected void setHadoopSecondaryNamenode(SecondaryNameNode hadoopSecondaryNamenode) {
		this.hadoopSecondaryNamenode = hadoopSecondaryNamenode;
	}

	protected boolean isZookeeperPeerRunning() {
		return this.zookeeperPeerRunning;
	}

	protected void setZookeeperPeerRunnigState (boolean b) {
		this.zookeeperPeerRunning = b;
	}

	protected HMaster getHbaseMaster() {
		return hbaseMaster;
	}

	protected void setHbaseMaster(HMaster hbaseMaster) {
		this.hbaseMaster = hbaseMaster;
	}

	protected HRegionServer getHbaseRegionserver() {
		return hbaseRegionserver;
	}

	protected void setHbaseRegionserver(HRegionServer hbaseRegionserver) {
		this.hbaseRegionserver = hbaseRegionserver;
	}
	
	protected Process getZookeeperPeerProcess () {
		return this.zookeeperPeerProcess;
	}
	
	protected void setZookeeperPeerProcess (Process zookeeperPeerProcess) {
		this.zookeeperPeerProcess = zookeeperPeerProcess;
	}
        
        protected String getFilesDirectory () {
            return this.filesDirectory;
        }
        
        protected String getNamenodeDir () {
            if (this.getFilesDirectory()==null)
                return null;
            return this.getFilesDirectory()+File.separator+HBaseLauncher.NAMENODE_DIR;
        }
        
        protected String getSecondaryNamenodeDir () {
            if (this.getFilesDirectory()==null)
                return null;
            return this.getFilesDirectory()+File.separator+HBaseLauncher.SECONDARY_NAMENODE_DIR;
        }
        
        protected String getDatanodeDir () {
            if (this.getFilesDirectory()==null)
                return null;
            return this.getFilesDirectory()+File.separator+HBaseLauncher.DATANODE_DIR;
        }
        
        protected String getZookeeperDir () {
            if (this.getFilesDirectory()==null)
                return null;
            return this.getFilesDirectory()+File.separator+HBaseLauncher.ZOOKEEPER_DIR;
        }

        @Override
        public void setNetworkInterface (String name) {
            this.networkInterface = name;
        }
        
        @Override
        public void setIsIPv4Preferred (Boolean b) {
            this.isIPv4Preferred = b;
        }
        
        @Override
        public void setisIPv6Used (Boolean b) {
            this.isIPv6Used = b;
        }

        @Override
        public String getIpAddress() {
            NetworkInterface iface = null;
            try {
                iface = NetworkInterface.getByName(this.networkInterface);
            } catch (SocketException ex) {
                this.logger.warn("Exception while searching for network interface specified in configuration files. "+ex.getMessage());
            }
            if (iface==null)
                return null;
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            InetAddress ipv4Address = null;
            InetAddress ipv6Address = null;
            InetAddress choosenIpAddress;
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                this.logger.debug("HBASELAUNCHER DEBUG: ip trovato:" + address);
                if (address.getClass().getName().compareTo("java.net.Inet4Address")==0)
                    ipv4Address = address;
                else if (address.getClass().getName().compareTo("java.net.Inet6Address")==0)
                    ipv6Address = address;
                this.logger.debug("HBASELAUNCHER DEBUG: ipV4 trovato:" + ipv4Address);
                this.logger.debug("HBASELAUNCHER DEBUG: ipV6 trovato:" + ipv6Address);
            }
            if (this.isIPv6Used) {
                if (this.isIPv4Preferred && ipv4Address!=null)
                    choosenIpAddress = ipv4Address;
                else if (this.isIPv4Preferred && ipv4Address==null)
                    choosenIpAddress = ipv6Address;
                else if (!this.isIPv4Preferred && ipv6Address!=null)
                    choosenIpAddress = ipv6Address;
                else
                    choosenIpAddress = ipv4Address;
            } else {
                choosenIpAddress = ipv4Address;
            }
            if (choosenIpAddress==null)
                return null;
            return choosenIpAddress.getHostAddress();
        }

    @Override
    public ArrayList<String> getRunningNodes() {
        ArrayList<String> result = new ArrayList<String>();
        if (this.hadoopNamenode!=null)
            result.add(HostManagerNodes.NAMENODE);
        if (this.hadoopSecondaryNamenode!=null)
            result.add(HostManagerNodes.SECONDARY_NAMENODE);
        if (this.hadoopDatanode!=null)
            result.add(HostManagerNodes.DATANODE);
        if (this.zookeeperPeerRunning)
            result.add(HostManagerNodes.ZOOKEEPER);
        if (this.hbaseMaster!=null)
            result.add(HostManagerNodes.HBASE_MASTER);
        if (this.hbaseRegionserver!=null)
            result.add(HostManagerNodes.REGIONSERVER);
        return result;
    }

    @Override
    public void setFilesDirectory(String dir) {
        this.filesDirectory = dir;
    }

    @Override
    public void formatHadoopFS(String conf) throws Exception {
        (new NamenodeFormatter(this.parseConfigurationXml(conf), this)).exec();
    }
        
}
abstract class NodeManager {
	private HBaseLauncher hbaseLauncher;
	public NodeManager (HBaseLauncher hbaseLauncher) {
		super();
		this.hbaseLauncher = hbaseLauncher;
	}
	abstract protected void exec() throws IOException, CleverException;
	protected HBaseLauncher getHbaseLauncher() {
		return hbaseLauncher;
	}
	protected void setHbaseLauncher(HBaseLauncher hbaseLauncher) {
		this.hbaseLauncher = hbaseLauncher;
	}
}



abstract class NodeLauncher extends NodeManager {
	private Configuration conf;
	private String[] options;
	public NodeLauncher (Configuration conf, HBaseLauncher hbaseLauncher, String[] options) {
		super(hbaseLauncher);
		this.conf = conf;
		this.options = options;
	}
	protected abstract void launchNode() throws IOException, CleverException;
        @Override
	protected void exec() throws IOException, CleverException {
		this.launchNode();
	}
	protected Configuration getConfiguration() {
		return conf;
	}
	protected void setConfiguration(Configuration conf) {
		this.conf = conf;
	}
	protected String[] getOptions() {
		return options;
	}
	protected void setOptions(String[] options) {
		this.options = options;
	}
}



abstract class NodeStopper extends NodeManager {
	public NodeStopper (HBaseLauncher hbaseLauncher) {
		super(hbaseLauncher);
	}
	protected abstract void stopNode() throws CleverException;
        @Override
	protected void exec() throws IOException, CleverException{
		this.stopNode();
	}
}



class NamenodeLauncher extends NodeLauncher implements Runnable {
	public NamenodeLauncher (Configuration conf, HBaseLauncher hbaseLauncher) {
		super (conf, hbaseLauncher, null);
	}
        protected void control() throws CleverException {
                if (this.getHbaseLauncher().getHadoopNamenode()!=null) {
			String err = "Impossibile lanciare il namenode: esiste già un nodo di Hadoop in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
        }
        protected void updateProperties() {
                if (this.getHbaseLauncher().getNamenodeDir()!=null)
                    this.getConfiguration().set(HBaseLauncher.NAMENODE_DIR_PROPERTY_NAME, this.getHbaseLauncher().getNamenodeDir());
        }
        protected void launchNamenode () {
            this.run();
        }
        @Override
	public void run () {
		try {
                        String[] formattingOptions = {};
                        this.getHbaseLauncher().getLogger().info("Namenode lanciato sull'Host Manager");
			this.getHbaseLauncher().setHadoopNamenode( NameNode.createNameNode(formattingOptions, this.getConfiguration()));
		} catch (IOException e) {
                    try {
                        NamenodeStopper stopper = new NamenodeStopper (this.getHbaseLauncher());
                        stopper.exec();
                        String err = "Errore nel lancio del namenode. Il namenode è stato spento.\n"+e.getMessage();
                        this.getHbaseLauncher().getLogger().error(err);
                        throw new IOException (err);
                    } catch (IOException ex) {
                        this.getHbaseLauncher().getLogger().error(ex.getMessage());
                    } catch (CleverException ex) {
                        this.getHbaseLauncher().getLogger().error(ex.getMessage());
                    }
		}
	}
	@Override
	protected void launchNode() throws IOException, CleverException {
                this.control();
                this.getHbaseLauncher().getLogger().debug("HBASELAUNCHER DEBUG: launchNamenode(): controllo effettuato con successo");
                this.updateProperties();
                this.getHbaseLauncher().getLogger().debug("HBASELAUNCHER DEBUG: launchNamenode(): proprietà di Hadoop aggiornate");
                this.launchNamenode();
                this.getHbaseLauncher().getLogger().debug("HBASELAUNCHER DEBUG: launchNamenode(): namenode lanciato sull'host locale");
	}
}






class NamenodeFormatter extends NodeLauncher implements Runnable {
    public NamenodeFormatter (Configuration configuration, HBaseLauncher hbaseLauncher) {
        super(configuration, hbaseLauncher, null);
    }
    protected void control() throws CleverException {
            if (this.getHbaseLauncher().getHadoopNamenode()!=null) {
                    String err = "Impossibile formattare il filesystem con un namenode di Hadoop in esecuzione sulla JVM";
                    this.getHbaseLauncher().getLogger().error(err);
                    throw new CleverException (err);
            }
    }
    protected void updateProperties() {
            if (this.getHbaseLauncher().getNamenodeDir()!=null)
                this.getConfiguration().set(HBaseLauncher.NAMENODE_DIR_PROPERTY_NAME, this.getHbaseLauncher().getNamenodeDir());
    }
    protected void removeOldDir() throws IOException {
                File namenodeDir = new File (this.getHbaseLauncher().getNamenodeDir());
                if (namenodeDir.exists())
                    FileUtils.deleteDirectory(namenodeDir);
    }	
    protected void formatFS() {
        this.run();
    }
    @Override
    public void run () {
        try {
                String[] formattingOptions = {"-format"};
                this.getHbaseLauncher().getLogger().info("Formatting new HadoopFS...");
                NameNode.createNameNode(formattingOptions, this.getConfiguration());
                this.getHbaseLauncher().getLogger().info("Filesystem sull'Host Manager formattato correttamente");
        } catch (IOException e) {
            try {
                String err = "Errore nella formattazione dell'HadoopFS. Annullamento formattazione.\n"+e.getMessage();
                this.getHbaseLauncher().getLogger().error(err);
                NamenodeStopper stopper = new NamenodeStopper (this.getHbaseLauncher());
                stopper.exec();
                String err1 = "Errore nella formattazione dell'HadoopFS. Formattazione annullata.\n"+e.getMessage();
                this.getHbaseLauncher().getLogger().error(err1);
                throw new IOException (err);
            } catch (IOException ex) {
                this.getHbaseLauncher().getLogger().error(ex.getMessage());
            } catch (CleverException ex) {
                this.getHbaseLauncher().getLogger().error(ex.getMessage());
            }
        }
    }
    @Override
    protected void launchNode() throws IOException, CleverException {
        this.control();
        this.getHbaseLauncher().getLogger().debug("HBASELAUNCHER DEBUG: formatFS(): controllo effettuato con successo");
        this.updateProperties();
        this.getHbaseLauncher().getLogger().debug("HBASELAUNCHER DEBUG: formatFS(): proprietà di Hadoop aggiornate");
        this.removeOldDir();
        this.getHbaseLauncher().getLogger().debug("HBASELAUNCHER DEBUG: formatFS(): cartelle vecchie cancellate");
        this.formatFS();
        this.getHbaseLauncher().getLogger().debug("HBASELAUNCHER DEBUG: formatFS(): formattazione completata");
    }
}



class DatanodeLauncher extends NodeLauncher {
	public DatanodeLauncher (Configuration conf, HBaseLauncher hbaseLauncher) {
		super (conf, hbaseLauncher, null);
	}
        protected void control() throws CleverException {
                if (this.getHbaseLauncher().getHadoopNamenode()!=null || this.getHbaseLauncher().getHadoopSecondaryNamenode()!=null || this.getHbaseLauncher().getHadoopDatanode()!=null) {
			String err = "Impossibile lanciare il datanode: esiste già un nodo di Hadoop in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
                        throw new CleverException (err);
		}
        }
	protected void updateProperties() {
                if (this.getHbaseLauncher().getDatanodeDir()!=null)
                    this.getConfiguration().set(HBaseLauncher.DATANODE_DIR_PROPERTY_NAME, this.getHbaseLauncher().getDatanodeDir());
        }
        protected void removeOldDir() throws IOException {
                File datanodeDir = new File (this.getHbaseLauncher().getDatanodeDir());
                if (datanodeDir.exists())
                    FileUtils.deleteDirectory(datanodeDir);
        }
        protected void launchDatanode () throws IOException, CleverException {
		try {
			this.getHbaseLauncher().setHadoopDatanode( DataNode.createDataNode(new String[0], this.getConfiguration()));
                        this.getHbaseLauncher().getLogger().info("Datanode lanciato correttamente sull'Host Manager");
		} catch (IOException e) {
			DatanodeStopper stopper = new DatanodeStopper (this.getHbaseLauncher());
			stopper.exec();
			String err = "Errore nel lancio o nell'esecuzione del datanode. Il datanode è stato spento.\n"+e.getMessage();
                        this.getHbaseLauncher().getLogger().error(err);
                        throw new IOException (err);
		}
	}
	@Override
	protected void launchNode() throws IOException, CleverException {
                this.control();
                this.updateProperties();
                this.removeOldDir();
                this.launchDatanode();
	}
}



class SecondaryNamenodeLauncher extends NodeLauncher {
	public SecondaryNamenodeLauncher (Configuration conf, HBaseLauncher hbaseLauncher) {
		super (conf, hbaseLauncher, null);
	}
        protected void control() throws CleverException {
                if (this.getHbaseLauncher().getHadoopNamenode()!=null || this.getHbaseLauncher().getHadoopSecondaryNamenode()!=null || this.getHbaseLauncher().getHadoopDatanode()!=null) {
			String err = "Impossibile lanciare il secondary namenode: esiste già un nodo di Hadoop in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
        }
        protected void updateProperties() {
                if (this.getHbaseLauncher().getSecondaryNamenodeDir()!=null)
                    this.getConfiguration().set(HBaseLauncher.SECONDARY_NAMENODE_DIR_PROPERTY_NAME, this.getHbaseLauncher().getSecondaryNamenodeDir());
        }
        protected void removeOldDir() throws IOException {
                File secondaryNamenodeDir = new File (this.getHbaseLauncher().getSecondaryNamenodeDir());
                if (secondaryNamenodeDir.exists())
                    FileUtils.deleteDirectory(secondaryNamenodeDir);
        }
	protected void launchSecondaryNamenode () throws CleverException, IOException {
		try {
			this.getHbaseLauncher().setHadoopSecondaryNamenode(new SecondaryNameNode (this.getConfiguration()));
                        this.getHbaseLauncher().getLogger().info("Secondary Namenode lanciato correttamente sull'Host Manager");
		} catch (IOException e) {
			SecondaryNamenodeStopper stopper = new SecondaryNamenodeStopper (this.getHbaseLauncher());
			stopper.exec();
                        String err = "Errore nel lancio del secondary namenode. Il secondary namenode è stato spento.\n"+e.getMessage();
                        this.getHbaseLauncher().getLogger().error(err);
                        throw new IOException (err);
                }
	}
	@Override
	protected void launchNode() throws IOException, CleverException {
		this.control();
                this.updateProperties();
                this.removeOldDir();
                this.launchSecondaryNamenode();
	}
}



class ZookeeperProcessLauncher extends Thread {
	private HBaseLauncher hbaseLauncher;
	public ZookeeperProcessLauncher (HBaseLauncher hbaseLauncher) {
		this.hbaseLauncher = hbaseLauncher;
	}
        @Override
	public void run() {
		String separator = System.getProperty("file.separator");
		String classpath = System.getProperty("java.class.path");
		String path = System.getProperty("java.home")
	                + separator + "bin" + separator + "java";
		ProcessBuilder processBuilder = 
	                new ProcessBuilder(path, "-cp", 
	                classpath, 
	                HQuorumPeer.class.getName());
		Process process=null;
		try {
			process = processBuilder.start();
                        this.hbaseLauncher.getLogger().info("Peer di Zookeeper lanciato correttamente sull'Host Manager");
		} catch (IOException e) {
                        this.hbaseLauncher.getLogger().error("Errore nel lancio del peer di Zookeeper: "+e.getMessage());
		}
		this.hbaseLauncher.setZookeeperPeerProcess(process);
		this.hbaseLauncher.setZookeeperPeerRunnigState(true);
		try {
			if (process!=null)
				process.waitFor();
		} catch (InterruptedException e) {
			//quando termina libero le risorse
			this.removeConfigurationFile();
			this.hbaseLauncher.setZookeeperPeerProcess(null);
			this.hbaseLauncher.setZookeeperPeerRunnigState(false);
                        this.hbaseLauncher.getLogger().error("Interruzione del peer zookeeper");
			//TODO comunicare al ClusterManager la terminazione
		}
		//quando termina libero le risorse
		this.removeConfigurationFile();
		this.hbaseLauncher.setZookeeperPeerProcess(null);
		this.hbaseLauncher.setZookeeperPeerRunnigState(false);
                this.hbaseLauncher.getLogger().error("Peer Zookeeper terminato");
		//TODO comunicare al ClusterManager la terminazione
	}
	private void removeConfigurationFile () {
		File file = new File ("hbase-site.xml");
		if (file.exists())
			file.delete();
	}
}


//su zookeeper la cartella deve essere svuotata SEMPRE, quindi il lancio lo faccio privato così non può essere richiamato in una derivata
class ZookeeperPeerLauncher extends NodeLauncher {
	public ZookeeperPeerLauncher (Configuration conf, HBaseLauncher hbaseLauncher) {
		super (conf, hbaseLauncher, null);
	}
        private void control() throws CleverException {
                if (this.getHbaseLauncher().isZookeeperPeerRunning()) {
			String err = "Impossibile lanciare il peer zookeeper: esiste già un peer zookeeper in esecuzione sull'host";
			this.getHbaseLauncher().getLogger().error(err);
                        throw new CleverException (err);
		}
        }
        private void updateProperties() {
                if (this.getHbaseLauncher().getZookeeperDir()!=null)
                    this.getConfiguration().set(HBaseLauncher.ZOOKEEPER_DIR_PROPERTY_NAME, this.getHbaseLauncher().getZookeeperDir());
        }
        private void removeOldDir() throws IOException {
                File zookeeperDir = new File (this.getHbaseLauncher().getZookeeperDir());
                if (zookeeperDir.exists())
                    FileUtils.deleteDirectory(zookeeperDir);
        }
        private void launchZookeeperPeer () throws CleverException, IOException {
		this.createConfigurationFile();
		(new ZookeeperProcessLauncher (this.getHbaseLauncher())).start();
	}
	@Override
	protected void launchNode() throws IOException, CleverException {
                this.control();
                this.updateProperties();
                this.removeOldDir();
                this.launchZookeeperPeer();
	}
	private void createConfigurationFile () throws IOException {
		File file = new File (HBaseLauncher.ZOOKEEPER_CONFIGURATION_FILE_NAME);
		if (!file.exists())
			file.createNewFile();
		FileOutputStream out = new FileOutputStream (file);
		this.getConfiguration().writeXml(out);
		out.close();
	}
}



class HBaseMasterLauncher extends NodeLauncher {
	public HBaseMasterLauncher (Configuration conf, HBaseLauncher hbaseLauncher) {
		super (conf, hbaseLauncher, null);
	}
	private void launchHBaseMaster () throws CleverException, IOException {
		if (this.getHbaseLauncher().getHbaseMaster()!=null) {
			String err = "Impossibile lanciare il master di HBase: esiste già un master di HBase in esecuzione sulla JVM";
			this.getHbaseLauncher().getLogger().error(err);
                        throw new CleverException (err);
		}
		try {
			this.getHbaseLauncher().setHbaseMaster(new HMaster(this.getConfiguration()));
                        this.getHbaseLauncher().getLogger().info("Master di HBase lanciato correttamente sull'Host Manager");
                } catch (IOException e) {
			HBaseMasterStopper stopper = new HBaseMasterStopper (this.getHbaseLauncher());
			stopper.exec();
                        String err = "Errore nel lancio del master di HBase. Il master di HBase è stato spento.\n"+e.getMessage();
                        this.getHbaseLauncher().getLogger().error(err);
			throw new IOException (err);
		} catch (KeeperException e1) {
			HBaseMasterStopper stopper = new HBaseMasterStopper (this.getHbaseLauncher());
			stopper.exec();
			String err = "Errore nel lancio del master di HBase. Il master di HBase è stato spento.\n"+e1.getMessage();
                        this.getHbaseLauncher().getLogger().error(err);
			throw new IOException (err);
		} catch (InterruptedException e2) {
			HBaseMasterStopper stopper = new HBaseMasterStopper (this.getHbaseLauncher());
			stopper.exec();
			String err = "Errore nel lancio del master di HBase. Il master di HBase è stato spento.\n"+e2.getMessage();
                        this.getHbaseLauncher().getLogger().error(err);
			throw new IOException (err);
		}
	}
	@Override
	protected void launchNode() throws IOException, CleverException {
		this.launchHBaseMaster();
	}
}



class RegionserverLauncher extends NodeLauncher {
	public RegionserverLauncher (Configuration conf, HBaseLauncher hbaseLauncher) {
		super (conf, hbaseLauncher, null);
	}
	private void launchRegionserver () throws CleverException, IOException {
		if (this.getHbaseLauncher().getHbaseRegionserver()!=null) {
			String err = "Impossibile lanciare regionserver: esiste già un regionserver in esecuzione sulla JVM";
			this.getHbaseLauncher().getLogger().error(err);
                        throw new CleverException (err);
		}
		try {
			this.getHbaseLauncher().setHbaseRegionserver(new HRegionServer(this.getConfiguration()));
                        this.getHbaseLauncher().getLogger().info("Regionserver lanciato correttamente sull'Host Manager");
                } catch (InterruptedException e) {
			RegionserverStopper stopper = new RegionserverStopper (this.getHbaseLauncher());
			stopper.exec();
			String err = "Errore nel lancio o nell'esecuzione del master di HBase. Il master di HBase è stato spento.\n"+e.getMessage();
                        this.getHbaseLauncher().getLogger().error(err);
                        throw new IOException (err);
                }
	}
	@Override
	protected void launchNode() throws IOException, CleverException {
		this.launchRegionserver();
	}
}



class NamenodeStopper extends NodeStopper {
	public NamenodeStopper (HBaseLauncher hbaseLauncher) {
		super (hbaseLauncher);
	}
	private void stopNamenode () throws CleverException {
		if (this.getHbaseLauncher().getHadoopNamenode()==null) {
                        String err = "Impossibile stoppare il namenode: non esiste un namenode in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
		this.getHbaseLauncher().getHadoopNamenode().stop();
                this.getHbaseLauncher().getLogger().info("Namenode stoppato correttamente");
		this.getHbaseLauncher().setHadoopNamenode(null); //riporto a null l'handle altrimenti la prossima volta che lancio il nodo ci sarà errore
	}
	@Override
	protected void stopNode() throws CleverException {
		this.stopNamenode();
	}
}



class DatanodeStopper extends NodeStopper {
	public DatanodeStopper (HBaseLauncher hbaseLauncher) {
		super (hbaseLauncher);
	}
	private void stopDatanode () throws CleverException {
		if (this.getHbaseLauncher().getHadoopDatanode()==null) {
                        String err = "Impossibile stoppare il datanode: non esiste un datanode in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
		this.getHbaseLauncher().getHadoopDatanode().shutdown();
                this.getHbaseLauncher().getLogger().info("Datanode stoppato correttamente");
		this.getHbaseLauncher().setHadoopDatanode(null); //riporto a null l'handle altrimenti la prossima volta che lancio il nodo ci sarà errore
	}
	@Override
	protected void stopNode() throws CleverException {
		this.stopDatanode();
	}
}



class SecondaryNamenodeStopper extends NodeStopper {
	public SecondaryNamenodeStopper (HBaseLauncher hbaseLauncher) {
		super (hbaseLauncher);
	}
	private void stopSecondaryNamenode () throws CleverException {
		if (this.getHbaseLauncher().getHadoopSecondaryNamenode()==null) {
                        String err = "Impossibile stoppare il secondary namenode: non esiste un secondary namenode in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
		this.getHbaseLauncher().getHadoopSecondaryNamenode().shutdown();
                this.getHbaseLauncher().getLogger().info("Secondary namenode stoppato correttamente");
		this.getHbaseLauncher().setHadoopSecondaryNamenode(null); //riporto a null l'handle altrimenti la prossima volta che lancio il nodo ci sarà errore
	}
	@Override
	protected void stopNode() throws CleverException {
		this.stopSecondaryNamenode();
	}
}



class ZookeeperPeerStopper extends NodeStopper {
	public ZookeeperPeerStopper (HBaseLauncher hbaseLauncher) {
		super (hbaseLauncher);
	}
	private void stopZookeeperPeer () throws CleverException {
		if (!this.getHbaseLauncher().isZookeeperPeerRunning()) {
                        String err = "Impossibile stoppare il peer di Zookeeper: non esiste un peer di Zookeeper in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
		this.getHbaseLauncher().getZookeeperPeerProcess().destroy();
		this.getHbaseLauncher().setZookeeperPeerRunnigState(false);
		this.getHbaseLauncher().setZookeeperPeerProcess(null);
                this.getHbaseLauncher().getLogger().info("Peer di Zookeeper stoppato correttamente");
        }
	@Override
	protected void stopNode() throws CleverException {
		this.stopZookeeperPeer();
	}
}



class HBaseMasterStopper extends NodeStopper {
	public HBaseMasterStopper (HBaseLauncher hbaseLauncher) {
		super (hbaseLauncher);
	}
	private void stopHBaseMaster () throws CleverException {
		if (this.getHbaseLauncher().getHbaseMaster()==null) {
                        String err = "Impossibile stoppare il master di HBase: non esiste un master di HBase in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
		this.getHbaseLauncher().getHbaseMaster().stopMaster();
                this.getHbaseLauncher().getLogger().info("Master di HBase stoppato correttamente");
		this.getHbaseLauncher().setHbaseMaster(null); //riporto a null l'handle altrimenti la prossima volta che lancio il nodo ci sarà errore
	}
	@Override
	protected void stopNode() throws CleverException {
		this.stopHBaseMaster();
	}
}



class RegionserverStopper extends NodeStopper {
	public RegionserverStopper (HBaseLauncher hbaseLauncher) {
		super (hbaseLauncher);
	}
	private void stopRegionserver () throws CleverException {
		if (this.getHbaseLauncher().getHbaseRegionserver()==null) {
                        String err = "Impossibile stoppare il regionserver: non esiste un regionserver in esecuzione sulla JVM";
                        this.getHbaseLauncher().getLogger().error(err);
			throw new CleverException (err);
		}
		this.getHbaseLauncher().getHbaseRegionserver().stop("REGIONSERVER TERMINATO");
                this.getHbaseLauncher().getLogger().info("Regionserver stoppato correttamente");
		this.getHbaseLauncher().setHbaseRegionserver(null); //riporto a null l'handle altrimenti la prossima volta che lancio il nodo ci sarà errore
	}
	@Override
	protected void stopNode() throws CleverException {
		this.stopRegionserver();
	}
}
