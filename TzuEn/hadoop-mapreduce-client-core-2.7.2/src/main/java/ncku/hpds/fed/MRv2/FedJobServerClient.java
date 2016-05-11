/*******************************************************
 * Copyright (C) 2016 High Performance Parallel and Distributed System Lab, National Cheng Kung University
 *******************************************************/
package ncku.hpds.fed.MRv2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class FedJobServerClient extends Thread {
	private String mIP = "";
	private int mPort = 0;
    private boolean mRunFlag = false;
	private Object mLock = new Object();
	private Socket mSocket = null;
	private FedCloudProtocol.FedSocketState mState = FedCloudProtocol.FedSocketState.NONE;
	private BufferedReader mInput = null;
	private PrintWriter mOutput = null;

	public FedJobServerClient(String ip, int port) {
		mIP = ip;
		mPort = port;
	}

	public void run() {
		System.out.println("Start FedJob Client");
		int i = 0;
		synchronized (mLock) {
			do {
				try {
					// mRunFlag = false;
					mState = FedCloudProtocol.FedSocketState.CONNECTING;
					mSocket = new Socket();
					mSocket.setSoTimeout(60000); // wait 60 second
					mSocket.setTcpNoDelay(true);
					mSocket.setReuseAddress(true);
					mSocket.setKeepAlive(true);
					mSocket.connect(new InetSocketAddress(mIP, mPort), 300000);
					if (mSocket.isConnected()) {
						mInput = new BufferedReader(new InputStreamReader(
								mSocket.getInputStream()));
						mOutput = new PrintWriter(mSocket.getOutputStream());
						mState = FedCloudProtocol.FedSocketState.CONNECTED;
						mRunFlag = true;
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					// mRunFlag = false;
				}
				i++;
				try {
					System.out.println("connect to " + mIP
							+ " failed reconnect ");
					Thread.sleep(3000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (i < 20 && mRunFlag == false);
		}
		//String line = "";
		System.out.println("FedJobServerClient to " + mIP );
		while (mRunFlag) {
			try {
				synchronized (mLock) {
					//System.out.println("PING");
					mOutput.println(FedCloudProtocol.REQ_PING);
					mOutput.flush();
					
				}
				Thread.sleep(500); // 500ms
			} catch (Exception e) {
				System.out.println("Can't Ping\n");
                mRunFlag = false; 

				//mRunFlag = false;
				// e.printStackTrace();
			}
		}
		try {
			synchronized (mLock) {
				if (mSocket.isConnected()) {
					mSocket.close();
					mState = FedCloudProtocol.FedSocketState.DISCONNECTED;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // end of run
	private String sendMessage(String m) {
		
		while( ! (mSocket != null) ){
		}
        String res = "";
        try {
            synchronized ( mLock ) {
                if ( mState == FedCloudProtocol.FedSocketState.CONNECTED && 
                        mSocket != null ) {
                	System.out.println("send:"+m);
                    mOutput.println(m);
                    mOutput.flush();
                    res = mInput.readLine();
                }
            }
        } catch ( Exception e ) {
        	e.printStackTrace();
        }
        return res;
    }
	public String sendBye(){
		return sendMessage(FedCloudProtocol.REQ_BYE);
	}
	public String send5MBfile(){
		String res = "";
        try {
            synchronized ( mLock ) {
                if ( mState == FedCloudProtocol.FedSocketState.CONNECTED && 
                        mSocket != null ) {
                	byte[] five =  new byte[5*1024*1024];
                	InputStream in = new ByteArrayInputStream(five);

                    int count;
                    while ((count = in.read(five)) > 0) {
                        mOutput.println(five.toString());
                    }

                	//System.out.println("send:");
                    //mOutput.println();
                    mOutput.flush();
                    res = mInput.readLine();
                }
            }
        } catch ( Exception e ) {
        	e.printStackTrace();
        }
        return res;
	}
	public String sendRegionMapFinished(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_REGION_MAP_FINISHED +" "+ fs );
	}
	public String sendReqInfo_normalized(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_INFO +" "+ fs );
	}
	public String sendReqInfo(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_INFO_2 +" "+ fs );
	}

	public String sendReqInterInfo(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_INTER_INFO +" "+ fs );
	}
	public String sendRegionResource(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_REGION_RESOURCE+" "+ fs );
	}
	public String sendRegionWAN(String from, String dest, double speed) { 
		return sendMessage( FedCloudProtocol.REQ_REGION_WAN +" "+from+">"+ dest+">"+ Double.toString(speed) );
	}
	public String sendInterDataSize(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_INTER_SIZE+" "+ fs );
	}
	public String sendWaitBarrier(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_WAIT_BARRIER+" "+ fs );
	}
	public String sendRegionMapStartTime(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_RM_START+" "+fs);
	}
	public String sendRegionMapStopTime(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_RM_STOP+" "+fs);
	}
	public String sendRegionInterTransferStartTime(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_INTER_START+" "+fs);
	}
	public String sendRegionInterTransferStopTime(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_INTER_STOP+" "+fs);
	}
	public String sendTopStartTime(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_TOP_START+" "+fs);
	}
	public String sendTopStopTime(String fs) { 
		return sendMessage( FedCloudProtocol.REQ_TOP_STOP+" "+fs);
	}
	
	public void stopClientProbe() {
        try { 
           // sendMessage( FedCloudProtocol.REQ_BYE ); 
            mRunFlag = false;
            this.join();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
