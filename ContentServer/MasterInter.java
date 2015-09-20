import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The MasterInter Interface which has function to get the RTT from each CDNs
 *
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
public interface MasterInter extends Remote{

    /**
     * join Joins workers in system
     * @param worker
     * @return boolean
     * @throws RemoteException
     */
    public boolean join(NodeData worker) throws RemoteException;

    /**
     * acceptFileFromCDN Accepting the file from CDNs
     * @param filename, filData, ipAddress
     * @return void
     * @throws RemoteException
     */
    public void acceptFileFromCDN(String filename, byte[] fileData, String ipAddress) throws RemoteException;

    /**
     * getMinRTTCDNIP Get the min RTT from each CDNs
     * @param void
     * @return String
     * @throws RemoteException
     */
    public String getMinRTTCDNIP() throws RemoteException;
}

