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

package org.clever.ClusterManager.FederationPlugins;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.Federation.FederationListenerAgent;
import org.clever.Common.XMPPCommunicator.CleverMessage;

/**
 *
 * @author Giovanni Volpintesta
 */
public class FederationMessagePoolThread extends Thread {
    FederationListenerAgent federationListenerAgent;
    Logger logger;
    HashMap<Integer, CleverMessage> pool;
    ArrayDeque<CleverMessage> replyStack;
    
    public FederationMessagePoolThread (FederationListenerAgent agent) {
        this.federationListenerAgent = agent;
        this.pool = new HashMap<Integer, CleverMessage>();
        this.replyStack = new ArrayDeque<CleverMessage>();
    }
    
    @Override
    public void run() {
        //super.run();
        while (true) {
            if (this.replyStack.isEmpty())
                try {
                    this.logger.info("There are not messages to send to other federated CMs. Time to sleep for FederationMessagePoolThread");
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException ex) {
                    this.logger.info("Messages pool thread finished sleeping");
                }
            
            CleverMessage reply = replyStack.getFirst();
            this.logger.debug("Presa la reply "+reply+" dallo stack");
            replyStack.removeFirst();
            int id = reply.getId();
            this.logger.debug("ID della reply: "+id);
            Integer ID = new Integer(id);
            if (!pool.containsKey(ID)) //the msg already had a reply
                continue;
            CleverMessage msg = pool.get(ID);
            this.logger.debug("Preso il messaggio a cui rispondere dalla hashmap alla key: "+id+"; msg: "+msg);
            pool.remove(ID);
            
            reply.setSrc(msg.getDst());
            reply.setDst(msg.getSrc());
            reply.setId(UUID.randomUUID().hashCode());
            reply.setReplyToMsg(msg.getId());
            this.federationListenerAgent.sendMessage(reply);
            this.logger.info("Sent reply: "+reply.toXML());
        }
    }
    
    public synchronized void putRequestInPool (CleverMessage msg, int id) {
        Integer ID = new Integer(id);
        if (!this.pool.containsKey(ID)) //if the request already is in the pool, it's discarded
            this.pool.put(ID, msg);
        this.logger.info("Request "+msg+" added in pool with id "+id);
    }
    
    public synchronized void manageReply (CleverMessage msg) {
        this.replyStack.addLast(msg);
        logger.info("Reply "+msg+" (id = "+msg.getId()+") added to the stack");
        this.notifyAll();
    }
    
    public void setLogger (Logger logger) {
        this.logger = logger;
    }
}
