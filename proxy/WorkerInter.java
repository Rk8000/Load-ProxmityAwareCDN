import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * 
 * Worker's Interface
 * 
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
public interface WorkerInter extends Remote {

	/**
	 * Sorts the input
	 * 
	 * @param ArrayList<Integer> input
	 * @return ArrayList<Integer>
	 * @throws RemoteException
	 */
	public ArrayList<Integer> getSortedResult(ArrayList<Integer> input)
			throws RemoteException;

	/**
	 * To get sum of elements in input
	 * 
	 * @param input
	 * @return int
	 * @throws RemoteException
	 */
	public int getSumofElements(ArrayList<Integer> input)
			throws RemoteException;
	
	
	/**
	 * Master adds jobs to queue
	 * 
	 * @param job
	 * @return boolean
	 * @throws RemoteException
	 */
	public boolean addToSortedQueue(ArrayList<Integer> job) throws RemoteException;
	
	/**
	 * To ping worker to check whether it is alive
	 * @return
	 * @throws RemoteException
	 */
	public boolean ping() throws RemoteException;
}
