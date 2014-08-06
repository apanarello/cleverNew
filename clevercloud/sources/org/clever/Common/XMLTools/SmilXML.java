/*
 * The MIT License
 *
 * Copyright 2014 dissennato.
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

package org.clever.Common.XMLTools;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
//import java.util.logging.Logger;
import org.apache.log4j.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;



import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 *
 * @author dissennato
 */
public class SmilXML {
private final Logger logger;
String nameF,nameB;

    public SmilXML(Logger log, String f,String b) {
        logger = log;
        nameF=f;
        nameB=b;
    }
    /**
     * @param list
     * @throws java.io.FileNotFoundException
     * @throws java.lang.ClassNotFoundException
     */
    public  void createSmil(ArrayList <String>list ) throws FileNotFoundException, ClassNotFoundException {
        
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            DOMImplementation domImpl = doc.getImplementation();
           
           //OMImplementationLS domImplementation =
                   //DOMImplementationLS) DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            
            
            
            
            DocumentType docType = domImpl.createDocumentType("smil", "-//W3C//DTD SMIL 2.0//EN", "http://www.w3.org/2001/SMIL20/SMIL20.dtd");
            doc.appendChild(docType);
            Element smilElem = doc.createElement("smil");
            smilElem.setAttribute("xmlns", "http://www.w3.org/2001/SMIL20/Language");
           
           
            doc.appendChild(smilElem);

             // head elements

             Element head = doc.createElement("head");
             smilElem.appendChild(head);
             
             // body elements
             Element body = doc.createElement("body");
             smilElem.appendChild(body);
             // seq elements
             Element seq = doc.createElement("seq");
             body.appendChild(seq);
            for (String list1 : list) {
                String src = "https://s3.amazonaws.com/"+nameB+"/" + list1;
                Element video = doc.createElement("video");
                video.setAttribute("src", src);
                video.appendChild(seq);
            }       
             
            /*
             // set attribute to staff element
             Attr attr = doc.createAttribute("id");
             attr.setValue("1");
             staff.setAttributeNode(attr);
             // shorten way
             // staff.setAttribute("id", "1");
             // firstname elements
             Element firstname = doc.createElement("firstname");
             firstname.appendChild(doc.createTextNode("yong"));
             staff.appendChild(firstname);
             // lastname elements
             Element lastname = doc.createElement("lastname");
             lastname.appendChild(doc.createTextNodeile("mook kim"));
             staff.appendChild(lastname);
             // nickname elements
             Element nickname = doc.createElement("nickname");
             nickname.appendChild(doc.createTextNode("mkyong"));
             staff.appendChild(nickname);
             // salary elements
             Element salary = doc.createElement("salary");
             salary.appendChild(doc.createTextNode("100000"));
             staff.appendChild(salary);
             * */
            FileOutputStream fos = null;
            // write the content into xml file
            File f = new File("/home/dissennato/"+nameF+".smil");
            fos = new FileOutputStream(f);
           // TransformerFactory transformerFactory = TransformerFactory.newInstance();
            //Transformer transformer = transformerFactory.newTransformer();
            DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
            LSSerializer serializer = impl.createLSSerializer();
            LSOutput lso = impl.createLSOutput();
            lso.setByteStream(fos);
            serializer.write(doc,lso);
          //DOMSource source = new DOMSource(doc);
           // StreamResult result = new StreamResult(new File("/home/dissennato/file.xml"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            //transformer.transform(source, result);

            System.out.println("File saved!");

        } catch (ParserConfigurationException ex) {
        logger.error("Error in ParserConfiguration",ex);
    } catch (InstantiationException ex) {
        logger.error("Error in InstantiationException",ex);
    } catch (IllegalAccessException ex) {
        logger.error("Error in InstantiationException",ex);
    } catch (ClassCastException ex) {
        logger.error("Error in InstantiationException",ex);
    } 
    }
}
/**
 *
 * @author dissennato
 */

    
    

