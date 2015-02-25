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

package org.clever.ClusterManager.HadoopPlugin;

/**
 *
 * @author Giovanni Volpintesta
 */
public class SednaUtil {
    public SednaUtil() {}
    
    public static String decodeSednaXml (String xml) {
        String result = "";
        String[] p;
        while (true) {
            p = xml.split("&", 2);
            if (p.length<2) {
                result += p[0];
                break;
            }
            result += p[0];
            p = p[1].split(";", 2);
            if (p.length<2) { //to prevent null pointers, but this condition should never be true
                result += p[0];
                break;
            }
            if (p[0].compareTo("nbsp")==0) {
                result += " ";
                xml = p[1];
            } else if (p[0].compareTo("quot")==0) {
                result += "\"";
                xml = p[1];
            } else if (p[0].compareTo("amp")==0) {
                result += "&";
                xml = p[1];
            } else if (p[0].compareTo("lt")==0) {
                result += "<";
                xml = p[1];
            } else if (p[0].compareTo("gt")==0) {
                result += ">";
                xml = p[1];
            } else if (p[0].compareTo("acute")==0) {
                result += "´";
                xml = p[1];
            } else {
                result += "&"+p[0]+";";
                xml = p[1];
            }
        }
        return result;
    }
}
