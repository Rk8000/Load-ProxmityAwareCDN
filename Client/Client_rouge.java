import java.io.*;
import java.net.*;
import java.rmi.registry.*;
import java.rmi.Naming;

/**
 * The Client class which sends the request to Proxy
 *
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
public class Client_rouge  {

    String fileName, csIp;

    Client_rouge(String fileName) {
        this.fileName = fileName;
        csIp = null;
    }

    /**
     * rougeClient Class which extends Thread
     */
    class rougeClient extends Thread {

        rougeClient(String threadName) {
            super(threadName);
        }

        public void run() {
            requestFile();
        }
    }

    /**
     * getCsIp function receive the IP address of the CDNs
     * @param  void
     * @return void
     * @throws java.lang.Exception
     */
    void getCsIp() {
        MasterInter masterInter = null;
        try {
            Registry registry =
                    LocateRegistry.getRegistry("52.7.96.47", 50000);
            masterInter =
                    (MasterInter) registry.lookup("master");
            csIp = masterInter.getMinRTTCDNIP();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * requestFile function sends the request to CDNs
     * @param  void
     * @return void
     * @throws java.lang.Exception
     */
    void requestFile() {
        byte[] arr;
        int fileCounter = 0;
        InputStream is = null;
        BufferedOutputStream bos = null;

        try {
            //connect to content server
            if ( csIp == null )
                return;

            System.out.println(Thread.currentThread().getName() + " : Requesting file " + fileName);
            Socket socket = new Socket(csIp,60000);

            //send file name to content server
            OutputStream outputServer = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outputServer);
            out.writeUTF(fileName);

            //read file at client
            is = socket.getInputStream();
            //write file to client disk - buffer
            bos = new BufferedOutputStream(new FileOutputStream(fileName));

            int bufferSize = 5000;
            byte[] bytes = new byte[bufferSize];

            int count;
            while ((count = is.read(bytes)) > 0) {
                // bos.write(bytes, 0, count);
            }
            System.out.println(Thread.currentThread().getName() + " : Received file..");
            // closing all open streams
            bos.flush();
            bos.close();
            is.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The main method begins execution of the tests.
     * @param args not used
     * @return void
     */
    public static void main(String args[]) {
        int threadCnt = 0;
        Client_rouge c = new Client_rouge(args[0]);
        c.getCsIp();
        for ( int i = 0 ; i < 10 ; i++ ) {
            Client_rouge.rougeClient rc = c.new rougeClient("Thread " + (++threadCnt));
            rc.start();
        }
    }
}
