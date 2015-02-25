/*
 * The MIT License
 *
 * Copyright 2015 apanarello.
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

/**
 *
 * @author apanarello
 */
public final class DataFederationContainer {

    private String fileBuffer, jobName, fileNameS3, bucketName, user, pass, domain, agentname;
    private Boolean reply;
    private byte part;
    private long inizioChunck,fineChunck;
    int weight;

    public DataFederationContainer(String fileBuffer, String jobName, String fileNameS3, String bucketName, String domain, String agentname, Boolean hasReplay, byte part, long inizioChunck, long fineChunck, int weight) {
      
        setFileBuffer(fileBuffer);
        setJobName(jobName);
        setFileNameS3(fileNameS3);
        setBucketName(bucketName);
        setDomain(domain);
        setHasReply(hasReplay);
        setPart(part);
        setAgentname(agentname);
        setInizioChunck(inizioChunck);
        setFineChunck(fineChunck);
        setWeight(weight);
        
        
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
   

   

    public String getAgentname() {
        return agentname;
    }

    public void setAgentname(String agentname) {
        this.agentname = agentname;
    }

    public long getInizioChunck() {
        return inizioChunck;
    }

    public void setInizioChunck(long inizioChunck) {
        this.inizioChunck = inizioChunck;
    }

    public long getFineChunck() {
        return fineChunck;
    }

    public void setFineChunck(long fineChunck) {
        this.fineChunck = fineChunck;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getHasReplay() {
        return reply;
    }

    public void setHasReply(Boolean forwardable) {
        this.reply = forwardable;
    }

    public byte getPart() {
        return part;
    }

    public void setPart(byte part) {
        this.part = part;
    }

    public String getFileBuffer() {
        return fileBuffer;
    }

    public void setFileBuffer(String fileBuffer) {
        this.fileBuffer = fileBuffer;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getFileNameS3() {
        return fileNameS3;
    }

    public void setFileNameS3(String fileNameS3) {
        this.fileNameS3 = fileNameS3;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

}
