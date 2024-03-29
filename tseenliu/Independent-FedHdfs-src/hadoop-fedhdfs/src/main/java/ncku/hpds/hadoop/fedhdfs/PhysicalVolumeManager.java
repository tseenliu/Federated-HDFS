package ncku.hpds.hadoop.fedhdfs;

import java.awt.List;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.io.*;
import org.w3c.dom.Element;

import java.io.*;
import java.util.*;

public class PhysicalVolumeManager implements Serializable {
	
	private Vector<ArrayList<String>> fsPathElements = new Vector<ArrayList<String>>();
	private ArrayList<String> tmpFsPathElement;
	private final HashMap<String, String> PhysicalMappingTable = new HashMap<String, String>();
	
	public void addfsPathElementToArrayLists(String Uri, Configuration conf) throws IOException {
		tmpFsPathElement = new ArrayList<String>();
		makefsPathElement(Uri, conf);
		fsPathElements.addElement(tmpFsPathElement);
	}
	
	private void makefsPathElement(String Uri, Configuration conf) throws IOException {
		FileSystem fs = FileSystem.get(conf);
		FileStatus[] status = fs.listStatus(new Path(Uri));
		
		for (int i = 0; i < status.length; i++) {
			if (status[i].isDirectory()) {
				tmpFsPathElement.add(status[i].getPath().toString());
				makefsPathElement(status[i].getPath().toString(), conf);
			} else{
				tmpFsPathElement.add(status[i].getPath().toString());
			}
		}
	}
	
	public void updataPhysicalTable(Vector<Element> theFedhdfsElements){
		
		int VectorSize = fsPathElements.size();
		for (int i = 0; i < VectorSize; i++){
			for (int j = 0; j < fsPathElements.get(i).size(); j++){
				PhysicalMappingTable.put(fsPathElements.get(i).get(j), FedHdfsConParser.getValue("HostName",
						theFedhdfsElements.elementAt(i)));
			}
		}
	}
	
	public Vector<ArrayList<String>> getFsPathElements() {
		return fsPathElements;
	}
	
	public void showAllPhysicalMapping(){
		System.out.println(fsPathElements);	
	}
	
	public void showFederatedClustersSize(){
		int VectorSize = fsPathElements.size();
		System.out.println("\n[INFO] Number of Federated Clusters : " + VectorSize);
	}
	
	public int getFederatedClustersSize(){
		int VectorSize = fsPathElements.size();
		System.out.println("\nThe size of the VectorSize : " + VectorSize);
		return VectorSize;
	}
	
	public void showTotalFedPathNum(){
		int VectorSize = fsPathElements.size();
		int HTelements = 0;
		for (int i = 0; i < VectorSize; i++){
			HTelements += fsPathElements.get(i).size();	
			}
		System.out.println("\n[INFO] Number of the Federated hdfs path : " + HTelements);
	}
	
	public int getTotalFedPathNum(){
		int VectorSize = fsPathElements.size();
		int HTelements = 0;
		for (int i = 0; i < VectorSize; i++){
			HTelements += fsPathElements.get(i).size();	
			}
		return HTelements;
	}
	
	public void showPhysicalHashTable(){
		System.out.println(PhysicalMappingTable.entrySet());
	}
	
	public Set<Entry<String, String>> getPhysicalHashTable(){
		return PhysicalMappingTable.entrySet();
	}
	
	public void ShowCheckAhostName(String hostName){
		System.out.println(PhysicalMappingTable.containsValue(hostName));
	}
	
	public boolean checkAPaths(String hostName){
		return PhysicalMappingTable.containsValue(hostName);
	}
	
	public void checkAPath(String pathName){
		System.out.println(PhysicalMappingTable.containsKey(pathName));
	}
	
	public boolean ShowCheckAPath(String pathName){
		return PhysicalMappingTable.containsKey(pathName);
	}
	
	public void showAhostName(String pathName){
		System.out.println(PhysicalMappingTable.get(pathName));
	}
	
	public String getAhostName(String pathName){
		return PhysicalMappingTable.get(pathName);
	}
	
	public HashMap<String, String> getPhysicalMappingTable() {
		return PhysicalMappingTable;
	}
	
	public void physicalMappingDownload(){
		try{
			//----------------------------declare for writing--------------------------
			//We don't need "DataOutputStream"
			File fout = new File("FedFSImage/HashTable.txt");
			FileOutputStream fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			//----------------------------write into file.txt---------------------------
			for (String key: PhysicalMappingTable.keySet()){
				bw.write(key  + " --> "+ PhysicalMappingTable.get(key) + "\n");
			}
			//bw.write(PhysicalMappingTable.entrySet().toString());
			bw.flush();	
		}catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}catch (IOException ex) {
			ex.printStackTrace();
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
		System.out.println("\n[INFO] Physical Table download successful");
	} 
}


