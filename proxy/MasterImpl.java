import java.net.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import javax.swing.text.html.parser.Entity;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * A class for Master node of the distributed computation
 * 
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
public class MasterImpl extends UnicastRemoteObject implements MasterInter {

	private static final long serialVersionUID = 1L;
	private HashMap<Integer, NodeData> workerMap = new HashMap<Integer, NodeData>();
	private int workerCount;	
	HashMap<ArrayList<Integer>, Integer> grMap = new HashMap<ArrayList<Integer>, Integer>();
	private ArrayList<NodeData> aliveWorkers = new ArrayList<NodeData>();
	private ArrayList<NodeData> deadWorkers = new ArrayList<NodeData>();	
	private HashMap<String, Integer> ipToIdMapping = new HashMap<String, Integer>();
	private HashMap<NodeData, ArrayList<Long>> rttOfWokersMap = new HashMap<NodeData, ArrayList<Long>>();
	private RTTMeasure rttMeasure = new RTTMeasure();
	int choice = 0;
	public String wotkerFileName;
	private long dummyMinRTT = Long.MAX_VALUE;

	// constructor
	public MasterImpl() throws RemoteException {

	}

	/**
	 * DataStructure To Maintain RTTs of CDNs
	 * 
     * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
     * @version 1.2
     * @since   2015-05-11
     */
	class RTTMeasure {
		NodeData cdn;  // Information
		long minRTT;   // Minimum RTT

		public NodeData getCdn() {
			return cdn;
		}

		public void setCdn(NodeData cdn) {
			this.cdn = cdn;
		}

		public long getMinRTT() {
			return minRTT;
		}

		public void setMinRTT(long minRTT) {
			this.minRTT = minRTT;
		}

	}

	/**
	 * joins the CDN to master
	 * 
	 * NodeData   Information about CDN
	 * 
	 * return  boolean   true if join is successful else false
	 */
	@Override
	public boolean join(NodeData worker) throws RemoteException {

		Entry<Integer, NodeData> ent = null;
		int key = 0;
		boolean alreadyExisiting = false;
		Iterator<Entry<Integer, NodeData>> it = workerMap.entrySet().iterator();
		
		/*
		 * To check if CDN has already registered. Used in case when CDN is 
		 * made alive from dead state. Reregistartion
		 */
		while (it.hasNext()) {
			ent = it.next();
			if (ent.getValue().getIpAddress().equals(worker.getIpAddress())) {
				key = ent.getKey();
				worker.setId(key);  // Assign Previous ID
				workerMap.put(key, worker);  // Add CDN to map of CDNs
				ipToIdMapping.put(worker.getIpAddress(), key); // IP to ID of CDN map
				System.out.println("Added to alive");
				aliveWorkers.add(worker);  // Add to Alive CDNs list
				rttOfWokersMap.put(worker, new ArrayList<Long>());
				alreadyExisiting = true;
				System.out.println("Worker Joined with ID " + key
						+ " (already exists)");
			}

		}
		
		/*
		 * If a new CDN joins system
		 */ 
		if (!alreadyExisiting) {
			this.workerMap.put(workerCount, worker);
			worker.setId(workerCount);
			aliveWorkers.add(worker);
			rttOfWokersMap.put(worker, new ArrayList<Long>());
			ipToIdMapping.put(worker.getIpAddress(), workerCount);
			System.out.println("New Worker Joined with ID " + workerCount);
			workerCount++;
		}

		return true;
	}


	/**
	 * Used to get Application Level RTTs from CDN
	 * Uses ThreadPool to acheive this task.
	 * 
	 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
	 */
	class MasterWorkAllocator {

		ExecutorService exeService = Executors.newFixedThreadPool(10);

		/**
		 * Shuts Down ThreadPool
		 */
		public void closeExe() {
			exeService.shutdown();
		}

		/**
		 * Gets the application level RTTs of CDNs
		 */
		public void getRTTsFromCDNs() {

			while (true) {

				if (exeService.isShutdown()) {
					exeService = Executors.newFixedThreadPool(10);
				}
				
				// Iterate over map of CDNs which contains entry of each CDN registered.
				final Iterator<Entry<Integer, NodeData>> workerIterator = workerMap
						.entrySet().iterator();

				while (workerIterator.hasNext()) {
					try {
						
						exeService.submit(new Runnable() { // starts a new Thread

							@Override
							public void run() {

								NodeData workerData = null; 
								long currCDNRTT = 0;

								try {

									synchronized (workerIterator) {

										if (workerIterator.hasNext()) {
											workerData = workerIterator.next()
													.getValue();  // Gets the one of the CDNs information
										}

									}

									if (aliveWorkers.contains(workerData)) { // If selected CDN is is current Alive 
																			 // CDNs list

										if (!workerData.isHasSetStartTime()) { // Start RTT calculation
											workerData.setHasSetStartTime(true); // Set the calculation started as true
											                                    // so that no other thread can access same CDN

											System.out.println("Sent Request for RTT to "
													+ workerData.getIpAddress());
										    // Set the start time
											workerData.setRtt_start_time(System
													.currentTimeMillis());

											// worker.requestRTT();
											
											// Open a socket connection with CDN
											Socket socket = new Socket(
													workerData.getIpAddress(),
													60000);

											// send file name to content server
											OutputStream outputServer = socket
													.getOutputStream();
											DataOutputStream out = new DataOutputStream(
													outputServer);
											out.writeUTF("RTTFile.txt"); // Dummy file for RTT
											System.out.println("Pint to: "
													+ workerData.getIpAddress());
											// read file at client
											InputStream is = socket
													.getInputStream();
											// write file to client disk -
											// buffer
											int bufferSize = 5000;
											byte[] bytes = new byte[bufferSize];

											int count;
											while ((count = is.read(bytes)) > 0) {
												// bos.write(bytes, 0, count);
											}

											is.close();
											socket.close();
											// Mark end time
											long end_time = System
													.currentTimeMillis();
											System.out.println(workerData
													.getIpAddress()
													+ " : "
													+ (end_time - workerData
															.getRtt_start_time()));
											// int idOfCDN =
											// ipToIdMapping.get(workerData.getIpAddress());

											workerData
													.setRtt_end_time(end_time);  // Sets the end time to CDN information

											currCDNRTT = workerData
													.getRtt_end_time()
													- workerData
															.getRtt_start_time(); // Current RTT 

											long avg_rtt = 0;
											
											// To get fine tuned measurement of RTT, average of calulated RTTs
											// is obtained. List size of RTTs is 30.
											
											if (workerData.getRttList().size() <= 30) {

												workerData.getRttList().add(
														currCDNRTT);

												for (long l : workerData
														.getRttList()) {

													avg_rtt += l;

												}

												currCDNRTT = (avg_rtt)
														/ workerData
																.getRttList()
																.size();

											} else {
												workerData.getRttList().remove(
														0); // To accomodate new RTTs

												for (long l : workerData
														.getRttList()) {

													avg_rtt += l;

												}

												currCDNRTT = (avg_rtt)
														/ workerData
																.getRttList()
																.size();

											}

											if (dummyMinRTT >= currCDNRTT) {
												dummyMinRTT = currCDNRTT;
												rttMeasure.setCdn(workerData);
												rttMeasure
														.setMinRTT(currCDNRTT); // Calculates the Best(Min.) RTT till now.
											}

											workerData
													.setHasSetStartTime(false); // Make CDN available for RTT calculation again

										}

									}

								} catch (Exception e) {
									workerData.setDown(true); // Set worker is down
									System.out.println("Ohh... CDN is dead! "
											+ workerData.getIpAddress()); 
									e.printStackTrace();

									synchronized (aliveWorkers) {
										if (aliveWorkers.contains(workerData)) { // CDN is in alive workers

											aliveWorkers.remove(workerData); // Remove from Alive CDN workers list
											System.out
													.println("Removed From Alive");
										}
									}

									synchronized (deadWorkers) { 
										if (!deadWorkers.contains(workerData)) { // If CDN is not already present in Dead worker
											deadWorkers.add(workerData);  // Add CDN to Dead workers list
											System.out.println("Added to Dead");
										}
									}

								}

							}

						});

					} catch (Exception e) {
						e.printStackTrace();
					}

				}

				if (!exeService.isShutdown()) {
					exeService.shutdownNow();
				}

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

		}
	}
		
	/**
	 * This class is used for JSch ssh connection from proxy to CDN
     *
     * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
     * @version 1.2
     * @since   2015-05-11
	 */
	class MyUserInfo implements UserInfo, UIKeyboardInteractive {

		String passwd = "ubuntu";

		public String getPassword() {
			return passwd;
		}

		public boolean promptYesNo(String str) {

			return true;

		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public boolean promptPassword(String message) {

			return false;

		}

		public void showMessage(String message) {

			JOptionPane.showMessageDialog(null, message);

		}

		public String[] promptKeyboardInteractive(String destination,

		String name,

		String instruction,

		String[] prompt,

		boolean[] echo) {

			return null;

		}

	}
	
	/**
	 * Starts CDNs from Proxy at system Startup
	 */
	public void startWorkers() {

		String workerToInvokeIp = "";
		JSch jsch = new JSch();
		try {
			BufferedReader workerBr = new BufferedReader(new FileReader(
					wotkerFileName));

			String line = "";

			while ((line = workerBr.readLine()) != null) {
				workerToInvokeIp = line;

				java.util.Properties config = new java.util.Properties();
				jsch.addIdentity("instance1.pem");
				config.put("StrictHostKeyChecking", "no");
				Session session = jsch.getSession("ubuntu", workerToInvokeIp);
				session.setConfig(config);
				session.connect();
				System.out.println("Is conn " + session.isConnected());

				System.out.println("Connected");

				ChannelExec channel = (ChannelExec) session.openChannel("exec");

				channel.setCommand("sh run.sh;");

				System.out.println("Command Executed");

				channel.connect();
				channel.disconnect();
				session.disconnect();

			}
			workerBr.close();
		} catch (IOException | JSchException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Point of Execution
	 * 
	 * Starts check status thread, Starts thread pool for getting RTTs
	 */
	public void startExe() {

		CheckStatus chStatus = new CheckStatus();
		Thread checkStatusThread = new Thread(chStatus);
		checkStatusThread.start();  // Check status of CDN

		//PingWorkers ping = new PingWorkers();
		//Thread pingToWorkersThread = new Thread(ping);
		// pingToWorkersThread.start();

		MasterWorkAllocator mw = new MasterWorkAllocator();
		mw.getRTTsFromCDNs(); // Starts collecting RTTs

		// startWorkers();


	}

	/**
	 * main method of program
	 * 
	 * @param args
	 * @throws IOException
	 */

	public static void main(String[] args) throws IOException {
		try{
			MasterImpl master = new MasterImpl();
			try {
				
				LocateRegistry.createRegistry(50000);
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Registry registry = LocateRegistry.getRegistry(50000); // Starts rmi registry for master
			registry.rebind("master", master); // Bind remote master object

			master.startExe(); // Starts execution of proxy
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	/**
	 * This class pings workers to see if they are alive
	 * 
	 *@author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
	 */
	class PingWorkers implements Runnable {
		public void run() {
			int i = 0;
			while (true) {
				
				NodeData worker = null;
				while (!aliveWorkers.isEmpty()) {
					try {
						i++;
						if (i > 1000)
							i = 0;
						i = i % aliveWorkers.size();
						worker = aliveWorkers.get(i);
						

						Registry workerReg = LocateRegistry.getRegistry(
								worker.getIpAddress(), 60000);

						WorkerInter workerInter = (WorkerInter) workerReg
								.lookup(worker.getIpAddress());
						workerInter.ping();

						try {
							Thread.sleep(3000);
						} catch (Exception e) {

						}

					} catch (Exception e) {
						worker.setDown(true);
						System.out.println("Problem");
						synchronized (aliveWorkers) {
							if (aliveWorkers.contains(worker)) {
								aliveWorkers.remove(worker);								
							}
						}
						synchronized (deadWorkers) {
							if (!deadWorkers.contains(worker)) {
								deadWorkers.add(worker);
							}
						}
					}
				}

			}
		}
	}

	/**
	 * This class tries to revive dead workers to make it alive
	 * 
     * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
	 */
	class CheckStatus implements Runnable {

		@Override
		public void run() {

			JSch jsch = new JSch();
			int i = 0;
			while (true) {

				// Iterator<NodeData> iter = deadWorkers.iterator();
				NodeData deadWorker = null;
				while (!deadWorkers.isEmpty()) {
					try {

						System.out.println("Inside DEAD ZONE");

						i++;

						if (i > 1000)
							i = 0;

						i = i % deadWorkers.size();
						deadWorker = deadWorkers.get(i);

						java.util.Properties config = new java.util.Properties();
						jsch.addIdentity("instance1.pem");
						config.put("StrictHostKeyChecking", "no");
						Session session = jsch.getSession("ubuntu",
								deadWorker.getIpAddress());
						// session.setPassword(password);
						session.setConfig(config);
						session.connect();
						System.out.println("Is conn " + session.isConnected());

						System.out.println("Connected");

						ChannelExec channel = (ChannelExec) session
								.openChannel("exec");

						channel.setCommand("sh run.sh;");
						channel.connect();
						
						synchronized (deadWorkers) {
							if (deadWorkers.contains(deadWorker)) { // if CDN is in dead workers list , it has again made alive
								System.out.println("Removed from dead");
								deadWorkers.remove(deadWorker); // Remove it from dead list
							}
						}
						
						channel.disconnect();
						session.disconnect();

					} catch (Exception e) {
						// e.printStackTrace();
						// System.out.println("Problem");
						try {
							// Thread.sleep(5000);
						} catch (Exception e1) {
						}
					}
				}

			}

		}

	}
	
	/**
	 * Gives back IP address of CDN which has minimum RTT to client.
	 * 
	 * @return String   IP address of CDN with minimum RTT
	 */
	@Override
	public String getMinRTTCDNIP() throws RemoteException {
		return rttMeasure.getCdn().getIpAddress();
	}

}
