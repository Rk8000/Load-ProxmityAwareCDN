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
public class Client {

    String csIp, fileName;

    Client(String fileName) {
        this.fileName = fileName;
    }

    /**
     * getCsIp function receive the IP address of the CDNs
     * @param  void
     * @return void
     * @throws java.lang.Exception
     */
    void getCsIp() {
        ProxyIntf pIntf = null;
        try {
            Registry registry =
                    LocateRegistry.getRegistry();
            pIntf = (ProxyIntf) registry.lookup("proxy");
            csIp = pIntf.getCsIp();
            System.out.println("CS ip set to : " + csIp);
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
        InputStream is = null;
        BufferedOutputStream bos = null;

        try {
            //connect to content server
            Socket socket = new Socket("52.4.198.116",60000);
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
                bos.write(bytes, 0, count);
            }
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
        Client c = new Client(args[0]);
        c.requestFile();
    }
}
