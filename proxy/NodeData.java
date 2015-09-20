import java.io.Serializable;
import java.util.ArrayList;

/**
 * The NodeData class creates Profile fro each CDNs
 *
 * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
 * @version 1.2
 * @since   2015-05-11
 */
class NodeData implements Serializable {

    private String name; // Name of worker
    private int id; // id of worker
    private long rtt_start_time = 0;  // Start time for RTT calculations
    private long rtt_end_time = Long.MAX_VALUE;  // End time for RTT calculations
    private boolean hasSetStartTime;
    private boolean gotEndTime;
    private ArrayList<Long> rttList = new ArrayList<Long>(30);
    private long lastRTTMeasured;
    private String ipAddress;  // Ip address
    private boolean isDown; // master will set if worker is done
    private boolean isBusy;
    private String masterIP; // master's ip
    private boolean hasAssgnd; // when worker sends result to master

    /**
     * The Result class
     * @author  Satyajeet Shahane, Vaibhav Page, Ravi Kumar Singh
     * @version 1.2
     * @since   2015-05-11
     */
    class Result implements Serializable {
        ArrayList<Integer> res;  // Result to send to master
        int howManyJobs; // Number of jobs to send
        String ipAddr;  // Ip address of worker
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isDown() {
        return isDown;
    }

    public void setDown(boolean isDown) {
        this.isDown = isDown;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean isBusy) {
        this.isBusy = isBusy;
    }

    public String getMasterIP() {
        return masterIP;
    }

    public void setMasterIP(String masterIP) {
        this.masterIP = masterIP;
    }

    public boolean getHasAssgnd() {
        return hasAssgnd;
    }

    public void setHasAssgnd(boolean hasAssgnd) {
        this.hasAssgnd = hasAssgnd;
    }

    public long getRtt_start_time() {
        return rtt_start_time;
    }

    public void setRtt_start_time(long rtt_start_time) {
        this.rtt_start_time = rtt_start_time;
    }

    public long getRtt_end_time() {
        return rtt_end_time;
    }

    public void setRtt_end_time(long rtt_end_time) {
        this.rtt_end_time = rtt_end_time;
    }

    public boolean isHasSetStartTime() {
        return hasSetStartTime;
    }

    public void setHasSetStartTime(boolean hasSetStartTime) {
        this.hasSetStartTime = hasSetStartTime;
    }

    public boolean isGotEndTime() {
        return gotEndTime;
    }

    public void setGotEndTime(boolean gotEndTime) {
        this.gotEndTime = gotEndTime;
    }

    public ArrayList<Long> getRttList() {
        return rttList;
    }

    public void setRttList(ArrayList<Long> rttList) {
        this.rttList = rttList;
    }

    public long getLastRTTMeasured() {
        return lastRTTMeasured;
    }

    public void setLastRTTMeasured(long lastRTTMeasured) {
        this.lastRTTMeasured = lastRTTMeasured;
    }
}
