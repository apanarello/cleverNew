/*
 * The MIT License
 *
 * Copyright 2014 apanarello.
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
package org.clever.Common.JobRouler;

/**
 *
 * @author apanarello
 */
public class Dataweight implements Cloneable {

    private int weight;
    private String domain;
    private long start, end;

    public Dataweight(String dom, int w) {
        this.setDomain(dom);
        this.setWeight(w);
    }

    public int getWeight() {
        return weight;
    }

    public String getDomain() {
        return domain;
    }

    private void setWeight(int weight) {
        this.weight = weight;
    }

    private void setDomain(String domain) {
        this.domain = domain;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        try {
            Dataweight t = (Dataweight) super.clone(); // Object.clone copia campo a campo
            return t;
        } catch (CloneNotSupportedException e) {
// non puo’ accadere, ma va comunque gestito
            throw new InternalError(e.toString());
        }
    }

}
