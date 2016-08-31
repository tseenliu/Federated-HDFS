package fbicloud.botrank ;

import fbicloud.botrank.KeyComparator.* ;
import fbicloud.utils.Host2HostBehaviorFS ;
import fbicloud.utils.HostBehaviorFS ;
import fbicloud.utils.SimilarityFunction ;

import java.io.* ;
import java.math.BigDecimal ;
import java.math.RoundingMode ;
import java.util.* ;

import org.apache.hadoop.conf.Configuration ;
import org.apache.hadoop.conf.Configured ;
import org.apache.hadoop.fs.Path ;
import org.apache.hadoop.fs.FileSystem ;
import org.apache.hadoop.fs.FileStatus ;
import org.apache.hadoop.fs.FSDataOutputStream ;
import org.apache.hadoop.io.LongWritable ;
import org.apache.hadoop.io.Text ;
import org.apache.hadoop.io.NullWritable ;
import org.apache.hadoop.io.IntWritable ;
import org.apache.hadoop.mapreduce.Job ;
import org.apache.hadoop.mapreduce.Mapper ;
import org.apache.hadoop.mapreduce.Reducer ;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat ;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat ;
import org.apache.hadoop.util.GenericOptionsParser ;
import org.apache.hadoop.util.Tool ;
import org.apache.hadoop.util.ToolRunner ;


public class GroupPhase2MR_FED extends Configured implements Tool
{
	public static class GetFVIDMappingMapper extends Mapper <LongWritable, Text, IntWritable, Text>
	{
		private FSDataOutputStream out ;

		private int numOfReducer = 0 ;
		private int count = 0 ;

		// length of line
		private final static int lengthOfLine = 5 ;

		// map inter key, value
		private IntWritable interKey = new IntWritable() ;
		private Text interValue = new Text() ;


		public void setup ( Context context ) throws IOException, InterruptedException
		{
			Configuration config = context.getConfiguration() ;

			numOfReducer = Integer.parseInt ( config.get ( "mapreduce.job.reduces" ) ) ;
			String hdfsPath = config.get( "HDFSPath", "/user/hpds/" ) ;
			String mappingListDir = config.get( "FVID_IP_MappingList" ) ;

			FileSystem fs = FileSystem.get ( config ) ;
			fs.mkdirs ( new Path ( hdfsPath + mappingListDir ) ) ;
			out = fs.create ( new Path ( hdfsPath + mappingListDir + "/fvidIPMapping-" + context.getTaskAttemptID().getTaskID().getId())) ;
		}

		public void map ( LongWritable key, Text value, Context context ) throws IOException, InterruptedException
		{
			// input format
			// value :														index
			// FVID    protocol,G#    srcIPs    dstIPs    20 features		0~4


			String outputMappingList ;


			String line = value.toString() ;
			String[] subLine = line.split ("\t") ;

			String fvID = subLine[0] ;
			String protocol_gid = subLine[1] ;
			String srcIPs = subLine[2] ;
			String dstIPs = subLine[3] ;
			String features = subLine[4] ;

			if ( subLine.length == lengthOfLine )
			{
				interKey.set ( ++count % numOfReducer ) ;
				interValue.set ( fvID + "\t" + protocol_gid + "\t" + features ) ;
				context.write ( interKey, interValue ) ;


				// ### B part ###
				// Write the " FVID, Source IPs " to Mapping list
				outputMappingList = fvID + "\t" + srcIPs + "\n" ;
				out.write ( outputMappingList.getBytes(), 0, outputMappingList.length() ) ;
			}
		}

		public void cleanup ( Context context ) throws IOException, InterruptedException
		{
			super.cleanup ( context ) ;
			out.close() ;
		}
	}

	public static class GetFVIDMappingReducer extends Reducer <IntWritable, Text, NullWritable, Text>
	{
		// reduce output value
		private Text outputValue = new Text();


		public void reduce ( IntWritable key, Iterable<Text> values, Context context ) throws IOException, InterruptedException
		{
			// input format
			// value :										index
			// FVID    protocol,G#    20 features			0~2


			// ### C part ###
			for ( Text val : values )
			{
				String[] subLine = val.toString().split ("\t") ;

				String fvID = subLine[0] ;
				String protocol_gid = subLine[1] ;
				String features = subLine[2] ;

				outputValue.set ( fvID + "\t" + protocol_gid + "\t" + features ) ;
				context.write ( NullWritable.get(), outputValue ) ;
			}
			// ### C part end ###
		}
	}


	public int run ( String[] args ) throws Exception
	{
		Configuration conf = this.getConf() ;

		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs() ;
		if ( otherArgs.length != 3 )
		{
			System.out.println("arg[0] will not used.");
			System.out.println("GroupPhase2MR_FED: <in> <out> <fvidipmapping>") ;
			System.exit(2);
		}

		/*********************************************************/
		// job 6
		// get Feature Vector ID mapping
		/*********************************************************/

		conf.set("FVID_IP_MappingList",args[2]);

		Job getFVIDMapping = Job.getInstance(conf, "Group 2 - GetFVIDMapping");
		getFVIDMapping.setJarByClass(GroupPhase2MR_FED.class);

		getFVIDMapping.setMapperClass(GetFVIDMappingMapper.class);
		getFVIDMapping.setReducerClass(GetFVIDMappingReducer.class);

		getFVIDMapping.setMapOutputKeyClass(IntWritable.class);
		getFVIDMapping.setMapOutputValueClass(Text.class);

		getFVIDMapping.setOutputKeyClass(NullWritable.class);
		getFVIDMapping.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(getFVIDMapping, new Path(args[1]+"_assignFVID"));
		//System.out.println("**InputPath:" + args[1]+"_assignFVID");

		FileOutputFormat.setOutputPath(getFVIDMapping, new Path(args[1]));

		return getFVIDMapping.waitForCompletion(true) ? 0 : 1 ;
	}

	public static void main ( String[] args ) throws Exception
	{
		// Let ToolRunner handle generic command-line options
		int res = ToolRunner.run ( new Configuration(), new GroupPhase2MR_FED(), args ) ;
		// Run the class after parsing with the given generic arguments
		// res == 0 -> normal exit
		// res != 0 -> Something error
		System.exit (res) ;
	}
}