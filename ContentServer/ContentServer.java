import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

/**
 * The ServerReq class creates request at the CDNs end
 *
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
class ServerReq extends Thread {

    Socket clientSocket;
    static ArrayList<com.amazonaws.services.s3.model.Bucket> bucketArrayList = new ArrayList<com.amazonaws.services.s3.model.Bucket>();
    AmazonS3 s3;
    String bucketName;
    ServerReq(Socket clientSocket, AmazonS3 s3, String bucketName) {
        this.clientSocket = clientSocket;
        this.s3 = s3;
        this.bucketName = bucketName;
    }

    /**
     * listingTheBucket function lists all the Bucket
     * @param  s3
     * @return void
     */
    private static void listingTheBucket(AmazonS3 s3) {
        for( com.amazonaws.services.s3.model.Bucket bucket : s3.listBuckets()){
            bucketArrayList.add(bucket);
        }
    }

    /**
     * sendDataToClient function sends the data back to Client
     * @param  s3, file
     * @return boolean
     * @throws java.io.IOException
     */
    public void sendDataToClient(AmazonS3 s3, String file) throws IOException {
        Path path = Paths.get("/home/ubuntu/" + file);
        //first checking if the CDNs are having the file in its disk
        if(Files.notExists(path)){
            // not present in the disk
            if(listingKeysOfAllTheObject(s3, file)){
                gettingDataFromS3(s3, file);
            }
            else {
                System.out.println("file is not present in both local CDNs as well as in S3, can't serve you request!");
            }
        } else if(Files.exists(path)){
            // Checking if the file is current version of S3 file
            if(checkingIfTheFileIsCurrent(s3, file)){
                // the file is current
            } else {
                // the file is not current, getting the latest file from the S3
                gettingDataFromS3(s3, file);
            }
        }
    }

    /**
     * checkingIfTheFileIsCurrent function looks if the file present in CDNs chache is recent or not
     * @param  s3, file
     * @return boolean
     */
    private boolean checkingIfTheFileIsCurrent(AmazonS3 s3, String file) {
        String eTag = gettingETagOfTheFile(file);
        if (eTag == null) return true;
        System.out.println(eTag);
        FileReader inputFile = null;
        BufferedReader bufferReader = null;
        String line = null;
        try{
            inputFile = new FileReader("/home/ubuntu/" + file + ".etag");
            bufferReader = new BufferedReader(inputFile);
            line = bufferReader.readLine();
            bufferReader.close();
        }catch(Exception e){
            System.out.println("Error while reading file line by line:" + e.getMessage());
        }
        if( eTag.equals(line)){
            return true;
        }
        return false;
    }

    /**
     * gettingETagOfTheFile function looking for Etag of the file present in AWS S3
     * @param  file
     * @return eTag
     */
    private String gettingETagOfTheFile(String file) {
        if (file.equals("RTTFile.txt"))
            return null;
        String eTag = null;
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, file));
        eTag = object.getObjectMetadata().getETag();
        return eTag;
    }

    /**
     * listingKeysOfAllTheObject function looking for all requested file in every bucket present in AWS S3
     * @param  s3 file
     * @return boolean
     * @throws AmazonServiceException
     */
    private boolean listingKeysOfAllTheObject(AmazonS3 s3, String file) {
        try {
            for (int i = 0; i < 1; i++) {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName);
                ObjectListing objectListing;
                do {
                    objectListing = s3.listObjects(listObjectsRequest);
                    for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                        if (objectSummary.getKey().equalsIgnoreCase(file)) {
                            return true;
                        }
                    }
                    listObjectsRequest.setMarker(objectListing.getNextMarker());
                } while (objectListing.isTruncated());
            }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, " +
                    "which means your request made it " +
                    "to Amazon S3, but was rejected with an error response " +
                    "for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, " +
                    "which means the client encountered " +
                    "an internal error while trying to communicate" +
                    " with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
        return false;
    }

    /**
     * gettingDataFromS3 function request the file from AWS S3
     * @param  s3 , key
     * @return void
     * @throws AmazonServiceException
     */
    public void gettingDataFromS3(AmazonS3 s3, String key) throws IOException {
        try {
            for (int i = 0; i < 1; i++) {
                S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
                if( object.getKey().equalsIgnoreCase(key)) {
                    String contentType = object.getObjectMetadata().getContentType();
                    storingTheObjectToDisk1(object.getObjectContent(), key);
                    break;
                }
            }
            String eTag = gettingETagOfTheFile(key);
            try {
                File file = new File("/home/ubuntu/" + key + ".etag");
                BufferedWriter output = new BufferedWriter(new FileWriter(file));
                output.write(eTag);
                output.close();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which" + " means your request made it " + "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means"+ " the client encountered " + "an internal error while trying to " + "communicate with S3, " + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /**
     * storingTheObjectToDisk1 function store the file requested from AWS S3 to its chache
     * @param  objectContent , key
     * @return void
     * @throws java.io.IOException
     */
    private static void storingTheObjectToDisk1(InputStream objectContent, String key) {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        byte[] buff = new byte[50*1024];
        int count;
        try {
            bos = new BufferedOutputStream(new FileOutputStream("/home/ubuntu/" + key));
            while( (count = objectContent.read(buff)) != -1)
            {
                bos.write(buff, 0, count);
            }
            bos.close();
            objectContent.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * run function receives the file requested by client and send the file back to client
     * @param  void
     * @return void
     * @throws java.lang.Exception
     */
    public void run() {
        String fileName;
        DataInputStream in;
        byte[] arr = new byte[5000];
        try {
            //getting the file name from client
            in = new DataInputStream(clientSocket.getInputStream());
            fileName = in.readUTF();
            // calling the function to send the file back to the client
            sendDataToClient(s3, fileName);
            //read file from disk
            FileInputStream fis = new FileInputStream("/home/ubuntu/" + fileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //output stream for socket
            BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());
            // if the file is for getting RTT
            if ( !fileName.equals("RTTFile.txt") )
                System.out.println("\n Serving file: " + fileName + "\n");
            // writing to streams
            int count;
            while ((count = bis.read(arr)) > 0) {
                out.write(arr, 0, count);
            }
            // flushing and closing all the open streams
            out.flush();
            out.close();
            fis.close();
            bis.close();
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * The ContentServer class implements CDNs of the application which takes the request from Proxy
 * and interact the requested Client
 */

public class ContentServer extends Thread implements ContentServerIntf {

    // initial setup
    ServerSocket serverSocket;
    AmazonS3 s3;
    ContentServer() {
        serverSocket = null;
    }

    /**
     * setUp function creates the Credentials for AWS account
     * @param  void
     * @return void
     * @throws AmazonClientException
     */
    void setUp() {
        AWSCredentials credentials;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        s3 = new AmazonS3Client(credentials);
    }

    /**
     * run function creates the serverSocket and create the serverReq object
     * @param  void
     * @return void
     * @throws java.lang.Exception
     */
    public void run() {
        try {
            serverSocket = new ServerSocket(60000);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                (new ServerReq(clientSocket, s3, "dsproject.test")).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * getCsPublicIp returns the public IP address of the CDNs
     * @param  void
     * @return String
     * @throws java.lang.Exception
     */
    String getCsPublicIp() {
        String ip = null;
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/public-ipv4");
            URLConnection conn = url.openConnection();
            Scanner s = new Scanner(conn.getInputStream());
            if (s.hasNext()) {
                ip = s.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ip;
    }

    /**
     * requestRTT provides the dummy file to Proxy for calculating the RTT
     * @param  void
     * @return byte[]
     * @throws java.rmi.RemoteException
     */
    public byte[] requestRTT() throws RemoteException {
        File f = new File("/home/ubuntu/RTTFile.txt");
        byte[] arr = new byte[1000];

        try {
            FileInputStream is = new FileInputStream(f);
            is.read(arr, 0, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    /**
     * The main method begins execution of the tests.
     * @param args not used
     * @throws java.lang.Exception
     */
    public static void main(String args[]) {
        ContentServer cs = new ContentServer();
        try {
            // Bind the RMI Object
            ContentServerIntf stub =
                    (ContentServerIntf) UnicastRemoteObject.exportObject(cs, 0);
            LocateRegistry.createRegistry(40000);
            Registry registry = LocateRegistry.getRegistry(InetAddress.getLocalHost().getHostAddress(), 40000);
            registry.rebind(cs.getCsPublicIp(), stub);

            // start the ContentServer
            cs.setUp();
            cs.start();

            // register with proxy
            NodeData nodeData = new NodeData();
            nodeData.setIpAddress(cs.getCsPublicIp());
            Registry masterReg = LocateRegistry.getRegistry("52.7.96.47", 50000);
            MasterInter masterInter = (MasterInter) masterReg.lookup("master");
            masterInter.join(nodeData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
