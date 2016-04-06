/*******************************************************
 * Copyright (C) 2016 High Performance Parallel and Distributed System Lab, National Cheng Kung University
 *******************************************************/
package ncku.hpds.fed.MRv2;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.hadoop.io.Text;

public final class TopCloudHasher {
	public static int topCounts;
	public static List<String> topURLs;
	
	public TopCloudHasher(){
	}
	
	public static String generateFileName(Text key, int topNumbers){
		int hash = 0 ;
	    //	for(int i =0; i< mKey2.toString().length(); i++){
	    	//	hash = hash*31 + mKey2.toString().charAt(i);
	    	//}
		
		
		if(topNumbers < topCounts){
			hash = (key.hashCode() & Integer.MAX_VALUE) % topNumbers;
		}
		else{
			hash = (key.hashCode() & Integer.MAX_VALUE) % topCounts;
		}
	   //hash = key.toString().charAt(0)%topCounts;
	    return Integer.toString(hash);
		
	}
	public static String getFileName(String Name) throws UnknownHostException{
	//	String[] preIP = fsDefaultName.split("/");
    //	String[] IP = preIP[2].split(":");
    	
		//InetAddress address = InetAddress.getByName(Name);
		
    //	String ipHDFS = address.getHostAddress()+":"+IP[1]+"/";
		String ipHDFS = Name +"/";
    	System.out.println("C0 -->10.3.1."+ ipHDFS);
    	
    	return setFileNameOrder(ipHDFS);
		
	}
	public static String setFileNameOrder(String topUrl){
		
		for(int i = 0 ; i< topURLs.size(); i++){
			System.out.println("TopCloudHasher:"+topURLs.get(i)+" "+topUrl);
			if((topURLs.get(i)+"/").equalsIgnoreCase(topUrl))
				return "/" + Integer.toString(i) ;
		}
		return "";
	}
}
