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
package org.clever.Common.S3tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.crypto.NoSuchPaddingException;
import org.apache.log4j.Logger;
import org.clever.Common.Exceptions.CleverException;

/**
 *
 * @author apanarello
 */
public class S3Tools {

    public String destPath;
    public AmazonS3 s3;
    public Logger logger;

    public S3Tools() {

        s3 = null;
    }

    public S3Tools(Logger logger) {
        this.logger = logger;
        s3 = null;
    }

    /**
     * This sample demonstrates how to make basic requests to Amazon S3 using
     * the AWS SDK for Java.
     * <p>
     * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
     * account, and be signed up to use Amazon S3. For more information on
     * Amazon S3, see http://aws.amazon.com/s3.
     * <p>
     * <b>Important:</b> Be sure to fill in your AWS access credentials in the
     * AwsCredentials.properties file before you try to run this sample.
     * http://aws.amazon.com/security-credentials
     */
    /**
     * This sample demonstrates how to make basic requests to Amazon S3 using
     * the AWS SDK for Java.<p>
     * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
     * account, and be signed up to use Amazon S3. For more information on
     * Amazon S3, see http://aws.amazon.com/s3.
     * <p>
     * <b>Important:</b> Be sure to fill in your AWS access credentials in the
     * AwsCredentials.properties file before you try to run this sample.
     * http://aws.amazon.com/security-credentials
     *
     * @param rootK fileAut
     * @param destPath in local filesystem
     * @param bucket in S3amazon
     * @param fileName in S3amazon
     * @param init starByte to download
     * @param ends endByte to download
     * @throws java.io.IOException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.NoSuchProviderException
     * @throws javax.crypto.NoSuchPaddingException
     * @throws java.lang.InterruptedException
     * @throws java.security.spec.InvalidKeySpecException
     * @throws org.clever.Common.Exceptions.CleverException
     */
    public void getFileFromS3(String rootK, String destPath, String bucket, String fileName, Long init, Long ends) throws IOException, CleverException {
        /*
         * Important: Be sure to fill in your AWS access credentials in the
         *            AwsCredentials.properties file before you try to run this
         *            sample.
         * http://aws.amazon.com/security-credentials
         */
    	//System.out.println("test");
        //logger.debug("Authentication done with amazon: " + this.toString());
        /*AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(
         S3Sample.class.getResourceAsStream("/home/dissennato/Scaricati/rootkey.csv")));
         */

        /*
         byte[] salt = {
         (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
         (byte)0x56, (byte)0x34, (byte)0xE3, (byte)0x03
         };
         String passPhrase = pass; 
        
         int iterationCount = 19;

         //KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
         //SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
         //PropertiesCredentials p = new PropertiesCredentials(null)
         File f = new File(rootK);
       
         PropertiesCredentials prop = new PropertiesCredentials(f);
         AmazonS3 s3 = new AmazonS3Client(prop);*/
        //String bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
        //String key = "MyObjectKey";
        // System.out.println("===========================================");
        this.logger.debug("Getting Started with Amazon S3");
        //System.out.println("===========================================\n");

        try {
            /*
             * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
             * so once a bucket name has been taken by any user, you can't create
             * another bucket with that same name.
             *
             * You can optionally specify a location for your bucket if you want to
             * keep your data closer to your applications or users.
             */
            //System.out.println("Creating bucket " + bucketName + "\n");
            //s3.createBucket(bucketName);

            /*
             * List the buckets in your account
             */
            /*System.out.println("Listing buckets");
             for (Bucket bucket : s3.listBuckets()) {
             System.out.println(" - " + bucket.getName());
             }
             System.out.println();

             /*
             * Upload an object to your bucket - You can easily upload a file to
             * S3, or upload directly an InputStream if you know the length of
             * the data in the stream. You can also specify your own metadata
             * when uploading to S3, which allows you set a variety of options
             * like content-type and content-encoding, plus additional metadata
             * specific to your applications.
             */
            /*
             File f = createSampleFile();
             PutObjectRequest req = new PutObjectRequest("testKKBucket", "key1", f);
             ObjectMetadata metadata = new ObjectMetadata();
             metadata.setContentLength(f.length());
             req.setMetadata(metadata);
             System.out.println("Uploading a new object to S3 from a file\n");
             s3.putObject(req);
             */
            /*
             * Download an object - When you download an object, you get all of
             * the object's metadata and a stream from which to read the contents.
             * It's important to read the contents of the stream as quickly as
             * possibly since the data is streamed directly from Amazon S3 and your
             * network connection will remain open until you read all the data or
             * close the input stream.
             *
             * GetObjectRequest also supports several other options, including
             * conditional downloading of objects based on modification times,
             * ETags, and selectively downloading a range of an object.
             */
            /*
             * List the buckets in your account
             */
            //int start = Integer.parseInt(init);
            //int end = Integer.parseInt(end);
            this.destPath = destPath;
            Long start = (init);
            Long end = (ends);

            logger.debug("Start to Download bytes of object: " + start + " - " + end);
            //System.out.println(end);
            /*
             String host=InetAddress.getLocalHost().getHostName();
             System.out.println(host);
             if(host.equals("alfonso-HP")){
             System.out.println("UGUALE");
             end=19432118;
             start=0;
             }*/
            int count;
            //String s=null;
            logger.debug("PROVO A CREARE FILE CON PATH: " + destPath);
            File fileVid = new File(destPath);
            logger.debug("CREATO FILE CON PATH: " + destPath);
            S3Object object = s3.getObject(new GetObjectRequest(bucket, fileName).withRange(start, end));
            logger.debug("CREATO OGGETTO S3OBJECT");
            //System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
            /*Process pro,pro2;
             pro= Runtime.getRuntime().exec("sh /home/apanarello/misure.sh");
             pro2= Runtime.getRuntime().exec("hadoop fs -ls  /home/");
             BufferedReader stdInput=new BufferedReader(new InputStreamReader(pro2.getInputStream()));
             while((s=stdInput.readLine())!=null){
             System.out.println(s);
             }*/

            try {

                if (fileVid.exists()) {
                    logger.warn("file " + destPath + " just exists");
                    try {
                        InputStream in = object.getObjectContent();
                        byte[] buf = new byte[(end).intValue()];
                        OutputStream out = new FileOutputStream(fileVid);
                        while ((count = in.read(buf)) != -1) {
                            if (Thread.interrupted()) {
                                throw new InterruptedException();
                            }
                            out.write(buf, 0, count);
                        }
                        out.close();
                    } catch (IOException e) {
                        logger.debug("Error to write file in localFs", e);
                    } catch (InterruptedException e) {
                        logger.debug("Error to write file in localFs- Interruptesd", e);
                    }

                } else if (fileVid.createNewFile()) {

                    try {
                        InputStream in = object.getObjectContent();
                        byte[] buf = new byte[end.intValue()];
                        OutputStream out = new FileOutputStream(fileVid);
                        while ((count = in.read(buf)) != -1) {
                            if (Thread.interrupted()) {
                                throw new InterruptedException();
                            }
                            out.write(buf, 0, count);
                        }
                        out.close();
                        logger.debug("Il file  è stato creato: " + destPath);
                    } catch (IOException e) {
                        logger.error("during writing file to fs", e);
                    } catch (InterruptedException e) {
                        logger.error("during writing file to fs", e);
                    }

                } else {
                    logger.debug("Il file " + destPath + " non può essere creato");
                }
            } catch (IOException e) {
                logger.error("", e);
            }
        } catch (AmazonServiceException ase) {
            /* logger.error("Caught an AmazonServiceException, which means your request made it "
             + "to Amazon S3, but was rejected with an error response for some reason.",ase);
             logger.error("Error Message:    " + ase.getMessage(),ase);
             logger.error("HTTP Status Code: " + ase.getStatusCode(),ase);
             logger.error("AWS Error Code:   " + ase.getErrorCode(),ase);
             logger.error("Error Type:       " + ase.getErrorType(),ase);
             logger.error("Request ID:       " + ase.getRequestId(),ase);
             */
        } catch (AmazonClientException ace) {
            /* logger.error("Caught an AmazonClientException, which means the client encountered "
             + "a serious internal problem while trying to communicate with S3, "
             + "such as not being able to access the network.",ace);
             logger.error("Error Message: " + ace.getMessage(),ace);
             */
        }
    }

    public void uploadFile(String srcPath, String bucket, String destFileName) throws IOException, CleverException {
        logger.debug("it's trying to autenticate with amazon " + this.toString());
//        if (s3==null) s3=getAuth(fileRoot);
        logger.debug("Authentication done with amazon: " + this.toString());
        File f = new File(srcPath);
        PutObjectRequest req = new PutObjectRequest(bucket, destFileName, f);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(f.length());
        req.setCannedAcl(CannedAccessControlList.PublicRead);
        req.setMetadata(metadata);
        
        logger.debug("Uploading a new object to S3 from a file\n");
        s3.putObject(req);

    }
    /* private void displayTextInputStream(InputStream input) throws IOException {
     BufferedReader reader = new BufferedReader(new InputStreamReader(input));
     while (true) {
     String line = reader.readLine();
     if (line == null) break;

     System.out.println("    " + line);
     }
     System.out.println();
     }*/

    /**
     *
     * @param fileName
     * @return
     * @throws CleverException
     */
    public long getInfo(String fileName) throws CleverException {
        this.logger.debug("Entrato in getINFO s3 " + this.getClass().getName());
        Long size = null;
        ObjectListing obj;
        List<S3ObjectSummary> obList;

        // String Key=null;
        //Bucket objBucket=null;
        System.out.println("Listing Bucket");
        try {
            for (Iterator<Bucket> it = s3.listBuckets().iterator(); it.hasNext();) {
                obj = s3.listObjects(it.next().getName());
                this.logger.debug("BUCKET" + obj.getBucketName());
                obList = obj.getObjectSummaries();
                this.logger.debug("NUMERO OGGETTI IN BUCKET" + obj.getBucketName() + " : " + obList.size());

                do {
                    for (S3ObjectSummary objSumm : obj.getObjectSummaries()) {

                        this.logger.debug("FILE NAME AMAZON " + objSumm.getSize() + " " + objSumm.getKey());
                        this.logger.debug("FILE NAME IN INGRESSO: " + fileName + "S3FILENAME :" + objSumm.getKey());
                        if (fileName.equals(objSumm.getKey())) {
                            //bucket.add(objSumm.getKey());
                            this.logger.debug("Size ricavata " + objSumm.getSize() + "in " + this.getClass().getName());
                            return objSumm.getSize();

                        }
                    }

                } while (obj.isTruncated());

            }
        } catch (AmazonServiceException ex) {
            System.err.println(ex.getErrorCode());

        }

        return -1;
    }

    public void getAuth(String rootK) throws IOException {

        byte[] salt = {
            (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
            (byte) 0x56, (byte) 0x34, (byte) 0xE3, (byte) 0x03
        };
        String passPhrase = "clever10"; /*clever10*/

        this.logger.debug("INIZIO AUTENTICAZIONE CON S3");
        int iterationCount = 19;
        StringReader sRead = new StringReader(rootK);
        BufferedReader bRead = new BufferedReader(sRead);
        String s;
        s = bRead.readLine();
        File temp = null;
        FileOutputStream fos = null;
        int data;
        try {
            temp = File.createTempFile("root", ".csv", new File("/home/apanarello/"));

            fos = new FileOutputStream(temp);
            //temp = new File("rootkey_", ".csv", new File("/home/apanarello/"));

            fos.write(rootK.getBytes());
            temp.deleteOnExit();
        } catch (IOException ex) {
            this.logger.error("Creazione file temporaneo non riuscita", ex);
        }
        //KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
        //SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        //PropertiesCredentials p = new PropertiesCredentials(null)
        //File f = new File(rootK);

        PropertiesCredentials prop = new PropertiesCredentials(temp);
        s3 = new AmazonS3Client(prop);
        this.logger.debug("Ritorno Oggetto s3 " + this.getClass().getName());
        //return s3;

    }

    public void getAuthWithPath(String rootK) throws IOException {

        byte[] salt = {
            (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
            (byte) 0x56, (byte) 0x34, (byte) 0xE3, (byte) 0x03
        };
        String passPhrase = "clever10"; /*clever10*/

        int iterationCount = 19;

        //KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
        //SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        //PropertiesCredentials p = new PropertiesCredentials(null)
        File f = new File(rootK);

        PropertiesCredentials prop = new PropertiesCredentials(f);
        AmazonS3 s3 = new AmazonS3Client(prop);

    }

    /**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3
     *
     * @return A newly created temporary file with text data.
     *
     * @throws IOException
     */
    /*private static File createSampleFile() throws IOException {
     File file = File.createTempFile("aws-java-sdk-", ".txt");
     file.deleteOnExit();

     Writer writer = new OutputStreamWriter(new FileOutputStream(file));
     writer.write("abcdefghijklmnopqrstuvwxyz\n");
     writer.write("01234567890112345678901234\n");
     writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
     writer.write("01234567890112345678901234\n");
     writer.write("abcdefghijklmnopqrstuvwxyz\n");
     writer.close();

     return file;
     }
     */
    /*
     *
     * Displays the contents of the specified input stream as text.
     *
     * @param input
     *            The input stream to display as text.
     *
     * @throws IOException
     */
    /*private static void displayTextInputStream(InputStream input) throws IOException {
     BufferedReader reader = new BufferedReader(new InputStreamReader(input));
     while (true) {
     String line = reader.readLine();
     if (line == null) break;

     System.out.println("    " + line);
     }
     System.out.println();
     }
     */
//}
}
