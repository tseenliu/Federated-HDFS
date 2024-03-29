package ncku.hpds.hadoop.fedhdfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import ncku.hpds.hadoop.fedhdfs.shell.Mkdir;
import ncku.hpds.hadoop.fedhdfs.shell.Union;

public class SubmitJobsScheduler {
	
	private static File FedConfpath = SuperNamenode.XMfile;
	
	private static ArrayList<String> requestGlobalFile;
	private static List<multipleMR> listJobs;
	private static List<FedMR> listFedJobs;
	private static List<copyJar> listCpJar;
	private static List<copyFedXML> listCpFedXML;
	
	private static String jarPath = null;
	private static String jarFile = null;
	private static String mainClass = null;
	private static String globalfileInput = null;
	private static String globalfileOutput = null;
	
	private static String hostName = null;
	private static Boolean assignHostName = false;
	private static Boolean minTag = false;
	
	private static String otherArgs = "";
	
	public static void main(String[] args) throws Throwable {
		
		FedHdfsConParser.setSupernamenodeConf(FedConfpath);
		
		String parseArg[] = args;
		
		if (args.length < 5) {
			System.err.println("Oringinal-MR Usage: submit jar [jarFile] [program] [globalfileInput] [globalfileOutput] [otherArgs]");
			System.err.println("Fed-MR Usage: submit -f/-mf jar [jarFile] [program] [globalfileInput] [globalfileOutput] [otherArgs]");
			System.exit(2);
		}
		else if (parseArg[0].equalsIgnoreCase("-f")) {
			
			if (args.length == 6) {
				jarPath = parseArg[2];
				jarFile = jarPath.substring(jarPath.lastIndexOf("/")+1, jarPath.length());
				mainClass = parseArg[3];
				globalfileInput = parseArg[4];
				globalfileOutput = parseArg[5];
				fedJobs(minTag);
			}
			else if (parseArg[1].contains("-Dcluster=")) {
				jarPath = parseArg[3];
				jarFile = jarPath.substring(jarPath.lastIndexOf("/")+1, jarPath.length());
				mainClass = parseArg[4];
				globalfileInput = parseArg[5];
				globalfileOutput = parseArg[6];
				hostName = parseArg[1].substring(10, parseArg[1].length());
				assignHostName = true;
				
				for (int i = 8; i <= parseArg.length; i++ ){
					otherArgs = otherArgs + parseArg[i-1] + " ";
				}
				System.out.println(otherArgs);
				fedJobs(minTag);
			}
			else {
				jarPath = parseArg[2];
				jarFile = jarPath.substring(jarPath.lastIndexOf("/")+1, jarPath.length());
				mainClass = parseArg[3];
				globalfileInput = parseArg[4];
				globalfileOutput = parseArg[5];
				
				for (int i = 7; i <= parseArg.length; i++ ){
					otherArgs = otherArgs + parseArg[i-1] + " ";
				}
				System.out.println(otherArgs);
				fedJobs(minTag);
			}
		}
		else if (parseArg[0].equalsIgnoreCase("-mf")) {
			
			jarPath = parseArg[2];
			jarFile = jarPath.substring(jarPath.lastIndexOf("/")+1, jarPath.length());
			mainClass = parseArg[3];
			globalfileInput = parseArg[4];
			globalfileOutput = parseArg[5];
			minTag = true;
			
			for (int i = 7; i <= parseArg.length; i++ ){
				otherArgs = otherArgs + parseArg[i-1] + " ";
			}
			System.out.println(otherArgs);
			fedJobs(minTag);
		}
		else if (args.length == 5) {
			
			jarPath = parseArg[1];
			jarFile = jarPath.substring(jarPath.lastIndexOf("/")+1, jarPath.length());
			mainClass = parseArg[2];
			globalfileInput = parseArg[3];
			globalfileOutput = parseArg[4];
			smJobs();
		}
		else {
			jarPath = parseArg[1];
			jarFile = jarPath.substring(jarPath.lastIndexOf("/")+1, jarPath.length());
			mainClass = parseArg[2];
			globalfileInput = parseArg[3];
			globalfileOutput = parseArg[4];
			
			for (int i = 6; i <= parseArg.length; i++ ){
				otherArgs = otherArgs + parseArg[i-1] + " ";
			}
			System.out.println(otherArgs);
			smJobs();
		}
	}
	
	private static void smJobs() throws Throwable {
		
		listCpJar = new ArrayList<copyJar>();
		listJobs = new ArrayList<multipleMR>();
		queryGlobalFile(globalfileInput);
		System.out.println("Physical Input Path : ");
		for ( String GNlink : requestGlobalFile ) { System.out.println(GNlink); }
		System.out.println("JAR : " + jarFile);
		
		for (int i = 0; i < requestGlobalFile.size(); i++) {
			String tmpHostPath[] = requestGlobalFile.get(i).split(":");
			listCpJar.add(new copyJar(jarPath, tmpHostPath[0]));
		}
		
		for ( copyJar job : listCpJar ) { job.start(); }
		for ( copyJar job : listCpJar) { job.join(); } 
		
		for (int i = 0; i < requestGlobalFile.size(); i++) {
			String tmpHostPath[] = requestGlobalFile.get(i).split(":");
			listJobs.add(new multipleMR(jarFile, mainClass, tmpHostPath[0], tmpHostPath[1], globalfileOutput, otherArgs));
		}
		
		System.out.println("Start running RegionCloud Jobs");
		for ( multipleMR job : listJobs ) { job.start(); }
		System.out.println("Wait For RegionCloud Jobs");
        for ( multipleMR job : listJobs) { job.join(); } 
        System.out.println("RegionCloud Jobs all finished");
		
        boolean isAllZero = true;
        for ( multipleMR job : listJobs) {
        	if(job.getExitVal() != 0){
        		isAllZero = false;
        		break;
        	}
        }
        if(isAllZero){
        	constructGN(globalfileOutput);
        }
	}
	
	private static void fedJobs(boolean minTag) throws Throwable {
		
		String TopJarPath;
		String realTop;
		listCpJar = new ArrayList<copyJar>();
		listFedJobs = new ArrayList<FedMR>();
		listCpFedXML = new ArrayList<copyFedXML>();
		
		//queryGlobalFile(FedHdfsConParser.getFedInputFile(FedConfpath));
		queryGlobalFile(globalfileInput);
		System.out.println(globalfileInput);
		//for ( String GNlink : requestGlobalFile ) { System.out.println(GNlink); }
		
		TopcloudSelector top = new TopcloudSelector(globalfileInput, minTag);
		if (assignHostName == true) {
			realTop = hostName;
		} else {
			realTop = top.getTopCloud();
		}
		System.out.println("Top Cloud of FedMR:" + realTop);
		
		//TopJarPath = FedHdfsConParser.getHadoopHOME(FedConfpath, top.getTopCloud()) + jarPath.substring(jarPath.lastIndexOf("/"), jarPath.length());
		TopJarPath = FedHdfsConParser.getHadoopHOME(FedConfpath, realTop) + "/" + jarFile;
		System.out.println(TopJarPath);
		
		XMLTransformer test = new XMLTransformer();
		test.transformer(requestGlobalFile, realTop, TopJarPath, mainClass);
		
		listCpJar.add(new copyJar(jarPath, realTop));
		for ( copyJar job : listCpJar ) { job.start(); }
		for ( copyJar job : listCpJar) { job.join(); }
		
		System.out.println(XMLTransformer.FedMR);
		listCpFedXML.add(new copyFedXML(XMLTransformer.FedMR, realTop));
		for ( copyFedXML job : listCpFedXML ) { job.start(); }
		for ( copyFedXML job : listCpFedXML) { job.join(); }
		
		listFedJobs.add(new FedMR(jarFile, realTop, globalfileInput, globalfileOutput, otherArgs));
		
		for ( FedMR job : listFedJobs ) { job.start(); }
		for ( FedMR job : listFedJobs ) { job.join();; }
		
		boolean isAllZero = true;
        for ( FedMR job : listFedJobs ) {
        	if(job.getExitVal() != 0){
        		isAllZero = false;
        		break;
        	}
        }
        if(isAllZero){
        	String globalfile = globalfileOutput.substring(globalfileOutput.lastIndexOf("/")+1, globalfileOutput.length());
        	Mkdir mkdirGN = new Mkdir();
    		mkdirGN.constructGlobalFile("-mkdir", globalfile);
    		Union unionGlobalFile = new Union();
    		unionGlobalFile.union("-union", globalfile, realTop + ":/user/hpds/" + globalfileOutput);
        }
	}
	
	private static void queryGlobalFile(String globalfileInput) throws Throwable {

		String SNaddress = SuperNamenodeInfo.getSuperNamenodeAddress();
		int SNport = SuperNamenodeInfo.getGNQueryServerPort();
		
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
	
	private static void constructGN(String globalfileOutput) {
		
		String globalfile = globalfileOutput.substring(globalfileOutput.lastIndexOf("/")+1, globalfileOutput.length());
		Mkdir mkdirGN = new Mkdir();
		mkdirGN.constructGlobalFile("-mkdir", globalfile);
		for (int i = 0; i < requestGlobalFile.size(); i++) {
        	String tmpHostPath[] = requestGlobalFile.get(i).split(":");
        	Union unionGlobalFile = new Union();
    		unionGlobalFile.union("-union", globalfile, tmpHostPath[0] + ":/user/hpds/" + globalfileOutput);
		}
		/*if (globalfileOutput.contains("/")) {
			String globalfile = globalfileOutput.substring(globalfileOutput.lastIndexOf("/")+1, globalfileOutput.length());
			Mkdir mkdirGN = new Mkdir();
			mkdirGN.constructGlobalFile("-mkdir", globalfile);
			for (int i = 0; i < requestGlobalFile.size(); i++) {
	        	String tmpHostPath[] = requestGlobalFile.get(i).split(":");
	        	Union unionGlobalFile = new Union();
	    		unionGlobalFile.union("-union", globalfile, tmpHostPath[0] + ":/user/hpds/" + globalfileOutput);
	        }
		}
		else {
			Mkdir mkdirGN = new Mkdir();
			mkdirGN.constructGlobalFile("-mkdir", globalfileOutput);
	        for (int i = 0; i < requestGlobalFile.size(); i++) {
	        	String tmpHostPath[] = requestGlobalFile.get(i).split(":");
	        	Union unionGlobalFile = new Union();
	    		unionGlobalFile.union("-union", globalfileOutput, tmpHostPath[0] + ":/user/hpds/" + globalfileOutput);
	        }
		}*/
	}
}

class FedMR extends Thread {
	
	private String JAR;
	private String hostName;
	private String input;
	private String output;
	private String otherArgs;
	private int exitVal;
	
	private ShellMonitor mOutputMonitor;
	private ShellMonitor mErrorMonitor;

	public FedMR(String JAR, String topCloud, String input, String output, String otherArgs) {
		
		this.JAR = JAR;
		this.hostName = topCloud;
		this.input = input;
		this.output = output;
		this.otherArgs = otherArgs;
	}
	
	File FedConfpath = SuperNamenode.XMfile;
	
	@Override
	public void run() {
		
		Runtime rt = Runtime.getRuntime();
		String HdfsUri = FedHdfsConParser.getHdfsUri(FedConfpath, hostName);
		String split[] = HdfsUri.split(":");
		String HostAddress = split[0];
		String cmd = "ssh hpds@" + HostAddress + " ";
		cmd = cmd + FedHdfsConParser.getHadoopHOME(FedConfpath, hostName) + "/bin/hadoop jar" + " ";
		cmd = cmd + FedHdfsConParser.getHadoopHOME(FedConfpath, hostName) + "/" + JAR + " ";
		cmd = cmd + FedHdfsConParser.getFedMainClass(FedConfpath) + " ";
		cmd = cmd + " -Dfed=true ";
		cmd = cmd + " -Dfedconf=" + FedHdfsConParser.getHadoopHOME(FedConfpath, hostName) + "/fed_task/FedMR.xml ";
		cmd = cmd + FedHdfsConParser.getFedOtherArgs(FedConfpath) + " ";
		cmd = cmd + input + " " + output;
		cmd = cmd + " " + otherArgs;
	
		System.out.println(" FedJob : " + cmd);
		
		Process proc;
		try {
			proc = rt.exec(cmd);
			
			mOutputMonitor = new ShellMonitor( proc.getInputStream(), "Fed" );
			mErrorMonitor = new ShellMonitor( proc.getErrorStream(), "Fed" );
			mOutputMonitor.start();
			mErrorMonitor.start();
			mOutputMonitor.join();
			mErrorMonitor.join();
			
			/*String line = null;
			InputStream stderr = proc.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			System.out.println("<ERROR>");
			while ((line = br.readLine()) != null)
				System.out.println(line);
			System.out.println("</ERROR>");*/
			
			/*InputStream stdout = proc.getInputStream ();
            InputStreamReader osr = new InputStreamReader (stdout);
            BufferedReader obr = new BufferedReader (osr);
            //System.out.println ("<output>");
            while ( (line = obr.readLine ()) != null )
                System.out.println(line);
            //System.out.println ("</output>");*/

			exitVal = proc.waitFor();
			System.out.println("FedMR-Process exitValue: " + exitVal);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getExitVal() {
		return exitVal;
	}
}

class multipleMR extends Thread {
	
	private String JAR;
	private String mainClass;
	private String hostName;
	private String input;
	private String output;
	private String otherArgs;
	private int exitVal;
	
	private ShellMonitor mOutputMonitor;
	private ShellMonitor mErrorMonitor;

	public multipleMR(String JAR, String mainClass, String hostName, String input, String output, String otherArgs) {
		
		this.JAR = JAR;
		this.mainClass = mainClass;
		this.hostName = hostName;
		this.input = input;
		this.output = output;
		this.otherArgs = otherArgs;
	}
	
	File FedConfpath = SuperNamenode.XMfile;
	
	@Override
	public void run() {
		
		Runtime rt = Runtime.getRuntime();
		String HdfsUri = FedHdfsConParser.getHdfsUri(FedConfpath, hostName);
		String split[] = HdfsUri.split(":");
		String HostAddress = split[0];
		String cmd = "ssh hpds@" + HostAddress + " ";
		cmd = cmd + FedHdfsConParser.getHadoopHOME(FedConfpath, hostName) + "/bin/hadoop jar" + " ";
		cmd = cmd + FedHdfsConParser.getHadoopHOME(FedConfpath, hostName) + "/" + JAR + " ";
		cmd = cmd + mainClass + " ";
		cmd = cmd + input + " " + output;
		cmd = cmd + " " + otherArgs;
	
		System.out.println(cmd);
		
		Process proc;
		try {
			proc = rt.exec(cmd);
			
			mOutputMonitor = new ShellMonitor( proc.getInputStream(), hostName + "-" + HostAddress );
			mErrorMonitor = new ShellMonitor( proc.getErrorStream(), hostName + "-" + HostAddress );
			mOutputMonitor.start();
			mErrorMonitor.start();
			mOutputMonitor.join();
			mErrorMonitor.join();
			
			/*InputStream stderr = proc.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			//System.out.println("<ERROR>");
			while ((line = br.readLine()) != null)
				System.out.println(line);
			//System.out.println("</ERROR>");*/

			exitVal = proc.waitFor();
			System.out.println("FedMR-Process exitValue: " + exitVal);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getExitVal() {
		return exitVal;
	}
}

class copyJar extends Thread {
	
	private String JAR;
	private String hostName;

	public copyJar(String JAR, String hostName) {
		this.hostName = hostName;
		this.JAR = JAR;
	}
	
	File FedConfpath = SuperNamenode.XMfile;
	
	@Override
	public void run() {
		
		Runtime rt = Runtime.getRuntime();
		String HdfsUri = FedHdfsConParser.getHdfsUri(FedConfpath, hostName);
		String split[] = HdfsUri.split(":");
		String HostAddress = split[0];
		String cmd = "scp" + " " + JAR + " ";
		cmd = cmd + "hpds@" + HostAddress + ":" + FedHdfsConParser.getHadoopHOME(FedConfpath, hostName);
		System.out.println(cmd);
		
		Process proc;
		try {
			proc = rt.exec(cmd);
			InputStream stderr = proc.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			//System.out.println("<ERROR>");
			while ((line = br.readLine()) != null)
				System.out.println(line);
			//System.out.println("</ERROR>");

			int exitVal = proc.waitFor();
			System.out.println("JarCopy-Process exitValue: " + exitVal);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class copyFedXML extends Thread {
	
	private String FedXML;
	private String hostName;

	public copyFedXML(String FedXML, String hostName) {
		this.hostName = hostName;
		this.FedXML = FedXML;
	}
	
	File FedConfpath = SuperNamenode.XMfile;
	
	@Override
	public void run() {
		
		Runtime rt = Runtime.getRuntime();
		String HdfsUri = FedHdfsConParser.getHdfsUri(FedConfpath, hostName);
		String split[] = HdfsUri.split(":");
		String HostAddress = split[0];
		String cmd = "scp" + " " + FedXML + " ";
		cmd = cmd + "hpds@" + HostAddress + ":" + FedHdfsConParser.getHadoopHOME(FedConfpath, hostName) + "/fed_task";
		System.out.println(cmd);
		
		Process proc;
		try {
			proc = rt.exec(cmd);
			InputStream stderr = proc.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			//System.out.println("<ERROR>");
			while ((line = br.readLine()) != null)
				System.out.println(line);
			//System.out.println("</ERROR>");

			int exitVal = proc.waitFor();
			System.out.println("XMLCopy-Process exitValue: " + exitVal);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}