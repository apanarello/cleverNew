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

import java.util.List;
import org.apache.log4j.Logger;
import org.clever.ClusterManager.Federation.FederationListenerAgent;
import org.clever.Common.Exceptions.CleverException;
import org.clever.Common.XMLTools.MessageFormatter;
import org.clever.Common.XMPPCommunicator.CleverMessage;
import org.clever.Common.XMPPCommunicator.ErrorResult;
import org.clever.Common.XMPPCommunicator.MethodConfiguration;
import org.clever.Common.XMPPCommunicator.OperationResult;
import org.clever.Common.XMPPCommunicator.Result;

/**
 *
 * @author Giovanni Volpintesta
 */
public class FederationRequestExecutor extends Thread {
    FederationListenerAgent federationListenerAgent;
    CleverMessage msg;
    FederationMessagePoolThread poolThread;
    Logger logger;
    
    public FederationRequestExecutor(FederationListenerAgent federationListenerAgent, CleverMessage msg, Logger logger) {
        this.msg = msg;
        this.federationListenerAgent = federationListenerAgent;
        this.poolThread = this.federationListenerAgent.getFederationMessagePoolThread();
        this.logger = logger;
    }
    
    @Override
    public void run() {
        this.poolThread.putRequestInPool(this.msg, this.msg.getId());
        MethodConfiguration methodConf = new MethodConfiguration(msg.getBody(), msg.getAttachments());
        String agent = methodConf.getModuleName();
        String method = methodConf.getMethodName();
        boolean hasReply = msg.needsForReply();
        List params = methodConf.getParams();
        this.logger.debug ("moduleName: "+agent+" methodName: "+method+" hasReply: "+hasReply+" params: "+params+"\nid: "+msg.getId());
        CleverMessage replyMsg = new CleverMessage();
        replyMsg.setReplyToMsg(this.msg.getId());
        replyMsg.setId(this.msg.getId()); //important to retrieve the request message
        replyMsg.setHasReply(false);
        try {
            this.logger.debug("Launching federated method on CM...");
            Object replyObject = this.federationListenerAgent.invoke(agent, method, hasReply, params);
            this.logger.debug("object returned: "+replyObject);
            replyMsg.setType(CleverMessage.MessageType.REPLY);
            if (hasReply) {
                replyMsg.setBody (new OperationResult (Result.ResultType.OBJECT, replyObject, methodConf.getModuleName(), methodConf.getMethodName()));
                replyMsg.addAttachment (MessageFormatter.messageFromObject (replyObject));
            }
            //src e dst will be setted when the reply will be retrieved from stack
        } catch (CleverException ex) {
            replyMsg.setType(CleverMessage.MessageType.ERROR);
            replyMsg.setBody(new ErrorResult(Result.ResultType.ERROR, (new CleverException(ex)).toString(), methodConf.getModuleName(), methodConf.getMethodName()));
            replyMsg.addAttachment(MessageFormatter.messageFromObject(ex));
        } finally {
            this.logger.debug("Reply created: "+replyMsg.toXML());
            this.poolThread.manageReply(replyMsg);
        }
    }
}
