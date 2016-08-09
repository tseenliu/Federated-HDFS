package ncku.hpds.hadoop.fedhdfs;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import ncku.hpds.hadoop.fedhdfs.shell.InfoAggreration;
import ncku.hpds.hadoop.fedhdfs.shell.Ls;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.StringUtils;

public class TopcloudSelector {
	
	private static ArrayList<Map.Entry<String, CloudInfo>> list_Data;
	private static HashMap<String, CloudInfo> tmpElements = new HashMap<String, CloudInfo>();
	
	private static ArrayList<String> requestGlobalFile;
	private String globalfileInput = "";
	private String topcloud = "";
	
	public TopcloudSelector(String globalfile, boolean minTag) throws Throwable {
		queryGlobalFile(globalfile);
		addCloudsInfo();
		if(minTag == true){
			minOfValue();
		}else{
			maxOfValue();
		}
	}
	
	private void queryGlobalFile(String globalfile) throws Throwable {

		String SNaddress = SuperNamenodeInfo.getSuperNamenodeAddress();
		int SNport = SuperNamenodeInfo.getGNQueryServerPort();

		globalfileInput = globalfile;
		Socket client = new Socket(SNaddress, SNport);

		try {
			OutputStream stringOut = client.getOutputStream();
			
			stringOut.write(globalfileInput.getBytes());
			System.out.println("globalFile : " + globalfileInput);
			ObjectInputStream objectIn = new ObjectInputStream(client.getInputStream());
			Object object = objectIn.readObject();

			requestGlobalFile = (ArrayList<String>) object;

			stringOut.flush();
			stringOut.close();
			stringOut = null;
			objectIn.close();
			client.close();
			client = null;

		} catch (IOException e) {
			System.out.println("Socket connect error");
			System.out.println("IOException :" + e.toString());
		}
	}
	
	private void addCloudsInfo() throws Throwable {
		
		for (int i = 0; i < requestGlobalFile.size(); i++) {

			String tmpHostPath[] = requestGlobalFile.get(i).split(":");
			String tmpHost = tmpHostPath[0];
			CloudInfo tmpachHostInfo = new CloudInfo();
			HdfsInfoCollector thisCluster = new HdfsInfoCollector();
			tmpachHostInfo.setHdfs(thisCluster.getHdfsRemaining(tmpHost));
			tmpachHostInfo.setData(thisCluster.getDataSize(globalfileInput, tmpHost));
			tmpachHostInfo.setValue();
			tmpElements.put(tmpHost, tmpachHostInfo);
			
		}
	}
	
	private void maxOfHdfs() {
		
		list_Data = new ArrayList<Map.Entry<String, CloudInfo>>(tmpElements.entrySet());
		Collections.sort(list_Data, new Comparator<Map.Entry<String, CloudInfo>>() {
			public int compare(Map.Entry<String, CloudInfo> first, Map.Entry<String, CloudInfo> second) {
				//return (int) ((first.getValue().getHdfs()) - second.getValue().getHdfs());
				return Long.valueOf(first.getValue().getHdfs()).compareTo(Long.valueOf(second.getValue().getHdfs()));
			}
			});
		Collections.reverse(list_Data);

		for (Entry<String, CloudInfo> entry:list_Data) {
			System.out.println(entry);
        }
		
	}
	
	private void maxOfData() {
		
		list_Data = new ArrayList<Map.Entry<String, CloudInfo>>(tmpElements.entrySet());
		Collections.sort(list_Data, new Comparator<Map.Entry<String, CloudInfo>>() {
			public int compare(Map.Entry<String, CloudInfo> first, Map.Entry<String, CloudInfo> second) {
				//return (int) (second.getValue().getData() - first.getValue().getData());
				return Long.valueOf(first.getValue().getData()).compareTo(Long.valueOf(second.getValue().getData()));
			}
			});
		Collections.reverse(list_Data);

		for (Entry<String, CloudInfo> entry:list_Data) {
			System.out.println(entry);
        }
	}
	
	private void maxOfValue() {
		
		list_Data = new ArrayList<Map.Entry<String, CloudInfo>>(tmpElements.entrySet());
		Collections.sort(list_Data, new Comparator<Map.Entry<String, CloudInfo>>() {
			public int compare(Map.Entry<String, CloudInfo> first, Map.Entry<String, CloudInfo> second) {
				//return (int) (second.getValue().getData() - first.getValue().getData());
				return Double.valueOf(first.getValue().getValue()).compareTo(Double.valueOf(second.getValue().getValue()));
			}
			});
		Collections.reverse(list_Data);

		for (Entry<String, CloudInfo> entry:list_Data) {
			System.out.println(entry);
        }
	}
	
	private void minOfValue() {
		
		list_Data = new ArrayList<Map.Entry<String, CloudInfo>>(tmpElements.entrySet());
		Collections.sort(list_Data, new Comparator<Map.Entry<String, CloudInfo>>() {
			public int compare(Map.Entry<String, CloudInfo> first, Map.Entry<String, CloudInfo> second) {
				//return (int) (second.getValue().getData() - first.getValue().getData());
				return Double.valueOf(second.getValue().getValue()).compareTo(Double.valueOf(first.getValue().getValue()));
			}
			});
		Collections.reverse(list_Data);

		for (Entry<String, CloudInfo> entry:list_Data) {
			System.out.println(entry);
        }
	}
	
	public String getTopCloud() {
		topcloud = list_Data.get(0).getKey();
		return topcloud;
	}
	
	public void show() {
		System.out.println(tmpElements.entrySet());
	}
}