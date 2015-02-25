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
import org.apache.log4j.Logger;

/**
 *
 * @author Giovanni Volpintesta
 */
public class Client {
    
    public enum CLIENT_TYPE {
        CLIENT,
        DOMAIN
    }
    
    private CLIENT_TYPE TYPE;
    private String password;
    private ArrayList<String[]> groups;
    private String name;

    public Client(String name, CLIENT_TYPE TYPE, String password, ArrayList<String[]> groups) {
        this.TYPE = TYPE;
        this.password = password;
        this.groups = groups;
        this.name = name;
    }

    public Client(String name, CLIENT_TYPE TYPE, String password) {
        this(name, TYPE, password, new ArrayList<String[]>());
    }
    
    public Client (String xml, Logger logger) { //il logger è solo per il debug
        String s = xml;
        String p[] = s.split("<client", 2);
        s = p[1];
        p = s.split("name", 2);
        s = p[1];
        p = s.split("=", 2);
        s = p[1];
        p = s.split("\"", 3);
        this.name = p[1];
        s = xml;
        p = s.split("<type>", 2);
        s = p[1];
        p = s.split("</type>", 2);
        this.TYPE = CLIENT_TYPE.valueOf(p[0]);
        s = xml;
        p = s.split("<password>", 2);
        s = p[1];
        p = s.split("</password>", 2);
        this.password = p[0];
        s = xml;
        p = s.split("<groups>", 2);
        //if the <groups> tag wasn't found (p.lenght == 1), there is the tag <groups />,
        //that represents an empty list, so the next routine has to be skipped,
        this.groups = new ArrayList<String[]>();
        if (p.length == 2) {
            s = p[1];
            p = s.split("</groups>", 2);
            String groupsXML = p[0];
            while (true) {
                p = groupsXML.split("<group", 2);
                if (p.length<2)
                    break;
                groupsXML = p[1];
                p = groupsXML.split("</group>", 2);
                if (p.length<2)
                    break;
                groupsXML = p[1]; //setting groupsXML we're ready for the next cycle, so p[] can be used
                String groupXml = p[0];
                p = groupXml.split("id", 2);
                groupXml = p[1];
                p = groupXml.split("\"", 3);
                String id = p[1];
                p = groupXml.split("<note>",2);
                groupXml = p[1];
                p = groupXml.split("</note>",2);
                String note = p[0];
                String [] group = {id, note};
                this.groups.add(group);
            }
        }
        
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTYPE(CLIENT_TYPE TYPE) {
        this.TYPE = TYPE;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setGroups(ArrayList<String[]> groups) {
        this.groups = groups;
    }
    
    public String getName() {
        return name;
    }

    public CLIENT_TYPE getTYPE() {
        return TYPE;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Returns a collection of all the groups this client is part of.
     * The groups are String arrays containing the group's ID (position 0) and the
     * group description (position 1);
     * @return 
     */
    public ArrayList<String[]> getGroups() {
        return groups;
    }
    
    public void addGroup (String ID, String note) {
        if (!this.isPartOfGroup(ID)) {
            String[] group = {ID, note};
            this.groups.add(group);
        }
    }
    
    public boolean isPartOfGroup (String ID) {
        for (String[] group : this.groups)
            if (group[0].compareTo(ID)==0)
                return true;
        return false;
    }
    
    public String[] getGroup (String ID) {
        for (String[] group : this.groups)
            if (group[0].compareTo(ID)==0)
                return group;
        return null;
    }
    
    public String getGroupDescription (String ID) {
        String[] group = this.getGroup(ID);
        if (group == null)
            return null;
        return group[1];
    }
    
    public void removeGroup (String ID) {
        String[] group = this.getGroup(ID);
        if (group!=null)
            this.groups.remove(group);
    }
    
    public String toXML() {
        String s = "<client name=\""+this.name+"\">"
                + "<type>"+this.TYPE.toString()+"</type>"
                + "<password>"+this.password+"</password>"
                + "<groups>";
        for (String[] group : this.groups)
            s += "<group id=\""+group[0]+"\">"
                    + "<note>"+group[1]+"</note>"
                    + "</group>";
        s += "</groups></client>";
        return s;
    }
    
}
