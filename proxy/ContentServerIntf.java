import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The ContentServerIntf Interface which has function to send the RTT from each CDNs
 *
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
interface ContentServerIntf extends Remote {
    
    /**
     * requestRTT request to send RTT
     * @param void
     * @return byte[]
     * @throws RemoteException
     */
    public byte[] requestRTT() throws RemoteException;

}