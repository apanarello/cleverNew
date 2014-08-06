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

/**
 *
 * @author Giovanni Volpintesta
 */
public class Group {
    private String ID;
    private String note;
    private String creator;
    private ArrayList<String> clients;

    public Group(String ID, String note, String creator, ArrayList<String> clients) {
        this.ID = ID;
        this.note = note;
        this.creator = creator;
        this.clients = clients;
    }

    public Group(String ID, String note, String creator) {
        this(ID, note, creator, new ArrayList<String>());
    }
    
    public Group(String xml) {
        String s = xml;
        String p[] = s.split("<group", 2);
        s = p[1];
        p = s.split("id", 2);
        s = p[1];
        p = s.split("=", 2);
        s = p[1];
        p = s.split("\"", 3);
        this.ID = p[1];
        s = xml;
        p = s.split("<note>", 2);
        s = p[1];
        p = s.split("</note>", 2);
        this.note = p[0];
        s = xml;
        p = s.split("<creator>", 2);
        s = p[1];
        p = s.split("</creator>", 2);
        this.creator = p[0];
        s = xml;
        this.clients = new ArrayList<String>();
        while (true) {
            p = s.split("<client>", 2);
            if (p.length<2)
                break;
            s = p[1];
            p = s.split("</client>", 2);
            if (p.length<2)
                break;
            this.clients.add(p[0]);
            s = p[1];
        }
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setClients(ArrayList<String> clients) {
        this.clients = clients;
    }

    public String getCreator() {
        return creator;
    }

    public ArrayList<String> getClients() {
        return clients;
    }
    
    public boolean containsClient (String client) {
        if (this.creator.compareTo(client)==0)
            return true;
        return this.clients.contains(client);
    }
    
    public void addClient (String client) {
        if (!this.containsClient(client))
            this.clients.add(client);
    }
    
    public void removeClient (String client) {
        this.clients.remove(client);
    }
    
    public String toXML() {
        String s = "<group id=\""+this.ID+"\">"
                + "<note>"+this.note+"</note>"
                + "<creator>"+this.creator+"</creator>";
        for (String client : this.clients)
            s += "<client>"+client+"</client>";
        s += "</group>";
        return s;
    }
    
}
