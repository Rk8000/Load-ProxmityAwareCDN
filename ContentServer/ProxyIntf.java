import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The ProxyIntf Interface which sends the IP address of CDNs
 *
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
public interface ProxyIntf extends Remote {
    
    /**
     * getCsIp sends IP address of the CDNs
     * @param void
     * @return String
     * @throws RemoteException
     */
	public String getCsIp() throws RemoteException;
}
