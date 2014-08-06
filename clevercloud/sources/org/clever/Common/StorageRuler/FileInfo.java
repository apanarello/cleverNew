/*
 * The MIT License
 *
 * Copyright (c) 2014 Giovanni Volpintesta
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

package org.clever.Common.StorageRuler;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Giovanni Volpintesta
 */
public class FileInfo {
    public enum PERMISSION_CODE {
        READ,
        WRITE,
        READ_AND_WRITE
    }
    public static final String LOCAL_DOMAIN = "LOCAL_DOMAIN";
    private String path;
    private String ID;
    private String domain;
    private String creatorID;
    private ArrayList<String> ownersID;
    private HashMap <String, PERMISSION_CODE> rights;

    public FileInfo(String path, String ID, String domain, String creatorID, ArrayList<String> ownersID, HashMap<String, PERMISSION_CODE> rights) {
        this.path = path;
        this.ID = ID;
        this.domain = domain;
        this.creatorID = creatorID;
        this.ownersID = ownersID;
        this.rights = rights;
        this.addOwner(creatorID, PERMISSION_CODE.READ_AND_WRITE);
    }

    public FileInfo(String path, String ID, String domain, String creatorID) {
        this (path, ID, domain, creatorID, new ArrayList<String>(), new HashMap<String, PERMISSION_CODE>());
    }
    
    public FileInfo(String xml) {
        /* String s = xml;
        String p[] = s.split("<file", 2);
        s = p[1];
        p = s.split("path", 2);
        s = p[1];
        p = s.split("=", 2);
        s = p[1];
        p = s.split("\"", 3);        
        this.path = p[1]; */
        
        String s = xml;
        String p[] = s.split("<path>", 2);
        s = p[1];
        p = s.split("</path>", 2);
        this.path = p[0];

        
        s = xml;
        p = s.split("<id>", 2);
        s = p[1];
        p = s.split("</id>", 2);
        this.ID = p[0];
        
        /* s = xml;
        p = s.split("<file", 2);
        s = p[1];
        p = s.split("id=\"", 2);
        s = p[1];
        p = s.split("=", 2);
        s = p[1];
        p = s.split("\"", 3);
        this.ID = p[1]; */
        
        s = xml;
        p = s.split("<domain>", 2);
        s = p[1];
        p = s.split("</domain>", 2);
        this.domain = p[0];
        s = xml;
        p = s.split("<creator>", 2);
        s = p[1];
        p = s.split("</creator>", 2);
        this.creatorID = p[0];
        s = xml;       
        p = s.split("<owners>", 2);
        //if the <owners> tag wasn't found (p.lenght == 1), there is the tag <owners />,
        //that represents an empty list, so the next routine has to be skipped,
        this.ownersID = new ArrayList<String>();
        this.rights = new HashMap<String, PERMISSION_CODE>();
        this.addOwner(this.creatorID, PERMISSION_CODE.READ_AND_WRITE);
        if (p.length == 2) {
            s = p[1];
            p = s.split("</owners>", 2);
            String ownersXML = p[0];
            while (true) {
                p = ownersXML.split("<group", 2);
                if (p.length<2)
                    break;
                ownersXML = p[1];
                p = ownersXML.split("</group>", 2);
                if (p.length<2)
                    break;
                ownersXML = p[1]; //setting ownersXML we're ready for the next cycle, so p[] can be used
                String groupXml = p[0];
                p = groupXml.split("id", 2);
                groupXml = p[1];
                p = groupXml.split("\"", 3);
                String groupID = p[1];
                p = groupXml.split("<rights>",2);
                groupXml = p[1];
                p = groupXml.split("</rights>",2);
                PERMISSION_CODE permission = PERMISSION_CODE.valueOf(p[0]);
                this.addOwner(groupID, permission);
            }
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

    public ArrayList<String> getOwnersID() {
        return this.ownersID;
    }

    public void setOwnersID(ArrayList<String> ownersID) {
        this.ownersID = ownersID;
    }

    public HashMap<String, PERMISSION_CODE> getRights() {
        return rights;
    }

    public void setRights(HashMap<String, PERMISSION_CODE> rights) {
        this.rights = rights;
    }
    
    public String getFirstOwner () {
        if (this.creatorID!=null)
            return this.creatorID;
        else
            return this.getOwnersID().get(0);
    }
    
    public final void addOwner (String ownerID, PERMISSION_CODE rights) {
        if (!this.hasOwner(ownerID) /* && !this.isCreator(ownerID) */ ) {
            this.ownersID.add(ownerID);
            this.rights.put(ownerID, rights);
        }
    }
    
    public void removeOwner (String ownerID) {
        if (!this.isCreator(ownerID)) {
            this.rights.remove(ownerID);
            this.ownersID.remove(ownerID);
        }
    }
    
    public PERMISSION_CODE getRights (String ownerID) {
        if (this.isCreator(ownerID))
            return PERMISSION_CODE.READ_AND_WRITE;
        return this.rights.get(ownerID);
    }
    
    public boolean hasWriteRights (String ownerID) {
        PERMISSION_CODE permissions = this.getRights(ownerID);
        return permissions==PERMISSION_CODE.WRITE || permissions==PERMISSION_CODE.READ_AND_WRITE;
    }
    
    public boolean hasReadRights (String ownerID) {
        PERMISSION_CODE permissions = this.getRights(ownerID);
        return permissions==PERMISSION_CODE.READ || permissions==PERMISSION_CODE.READ_AND_WRITE;
    }
    
    public boolean isCreator (String owner) {
        return this.creatorID.compareTo(owner)==0;
    }
    
    public boolean hasOwner (String owner) {
        return this.ownersID.contains(owner);
    }
    
    public void modifyRights (String ownerID, PERMISSION_CODE newRights) {
        if (this.hasOwner(ownerID)) {
            this.rights.remove(ownerID);
            this.rights.put(ownerID, newRights);
        } else
            this.addOwner(ownerID, newRights);
    }
    
    public String toXML() {
        /* String result = "<file path=\""+this.path+"\">"
                + "<id>"+this.ID+"</id>"
                + "<owners>";
        for (String ownerID : this.ownersID) {
            result += "<group id=\""+ownerID+"\">"
                    + "<rights>"+this.getRights(ownerID).toString()+"</rights>"
                    + "</group>";
        }
        result += "</owners></file>";
        return result; */
        
        String result = "<file path=\""+this.path+"\" id=\""+this.ID+"\">"
                + "<path>"+this.path+"</path>"
                + "<id>"+this.ID+"</id>"
                + "<domain>"+this.domain+"</domain>"
                + "<creator>"+this.creatorID+"</creator>"
                + "<owners>";
        for (String ownerID : this.ownersID) {
            result += "<group id=\""+ownerID+"\">"
                    + "<rights>"+this.getRights(ownerID).toString()+"</rights>"
                    + "</group>";
        }
        result += "</owners></file>";
        return result;
    }
    
}
