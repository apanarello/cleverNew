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
package org.clever.ClusterManager.HBaseManagerPlugins;

import java.util.ArrayList;
import org.clever.Common.Shared.HostEntityInfo;

/**
 *
 * @author giovanni
 */
public class HostManagerNodes {
    public static final String NAMENODE = "namenode";
    public static final String SECONDARY_NAMENODE = "secondary namenode";
    public static final String DATANODE = "datanode";
    public static final String ZOOKEEPER = "zookeeper";
    public static final String HBASE_MASTER = "master"; //master and backup-master
    public static final String REGIONSERVER = "regionserver";
    
    private HostEntityInfo HM;
    private String IP;
    private ArrayList<String> runningNodes;
    
    public HostManagerNodes () {
        this.HM = null;
        this.IP = null;
        this.runningNodes = new ArrayList<String>();
    }
    
    public HostEntityInfo getHM() {
        return HM;
    }

    public void setHM(HostEntityInfo HM) {
        this.HM = HM;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public ArrayList<String> getRunningNodes() {
        return runningNodes;
    }

    public void setRunningNodes(ArrayList<String> runningNodes) {
        this.runningNodes = runningNodes;
    }
   
}
