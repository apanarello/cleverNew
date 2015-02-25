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
package org.clever.Common.Federation;

import java.util.ArrayList;
import java.util.List;

/**
 *  Contains the reply for a request and the domain that produced it.
 * @author Giovanni Volpintesta
 */
public class FederationReply {
    
    private String domain;
    private List<String> history;
    private Object reply;
    
    public FederationReply (String domain, List<String> history, Object reply) {
        this.domain = domain;
        this.history = history;
        this.reply = reply;
    }
    
    public FederationReply (String domain, Object reply) {
        this (domain, new ArrayList<String>(), reply);
    }
    
    public String getDomain() {
        return this.domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    /**
     * Returns a collection of all the domains on which the method was launched. 
     * @return 
     */
    public List<String> getHistory() {
        return this.history;
    }
    
    public void addDomainToHistory (String domain) {
        this.history.add(domain);
    }
    
    //the next commented method is just for debug 
    /*
    public void removeDomainToHistory (String domain) {
        this.history.remove(domain);
    }
    */
    
    public void clearHistory () {
        this.history.clear();
    }
    
    public Object getReply() {
        if (this.reply instanceof FederationReply)
            return ((FederationReply)this.reply).getReply();
        else
            return this.reply;
    }
    
    public void setReply(Object reply) {
        this.reply = reply;
    }
    
}
