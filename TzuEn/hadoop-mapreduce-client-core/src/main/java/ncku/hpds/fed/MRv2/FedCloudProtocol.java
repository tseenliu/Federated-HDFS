/*******************************************************
 * Copyright (C) 2016 High Performance Parallel and Distributed System Lab, National Cheng Kung University
 *******************************************************/
package ncku.hpds.fed.MRv2;

public class FedCloudProtocol {
    /*
     *   Client                 Server
     *   Top Cloud   <---->      Region Cloud
     *    req PING                 res PONG
     *    res OK                   req Map-ProxyReduce Finished
     *    res OK                   req Start-Local Aggregation ( upload partial result from region cloud to top cloud )
     *    record AggregationTime   req Upload Finished  ( if not iterative, Top Cloud disconnect and Server Closed )
     *
     */
	public static final String REQ_MAP_PROXY_REDUCE_FINISHED = "MAP-PR-Finished";
    public static final String REQ_MIGRATE_DATA = "Migrate-Data";
    public static final String REQ_MIGRATE_DATA_FINISHED = "Migrate-Data-Finished";
	public static final String REQ_PING = "PING";
	public static final String RES_PONG = "PONG";
    public static final String RES_OK = "OK";
    public static final String REQ_BYE = "BYE";
    public static final String RES_BYE = "GoodBYE";
    public static final String REQ_REGION_MAP_FINISHED = "Region-Map-Finished";
    public static final String RES_REGION_MAP_FINISHED = "OK";
    public static final String REQ_REGION_WAN = "Region-WAN";
    public static final String RES_REGION_WAN = "WOK";
	public static final String REQ_WAN_SPEED = "req-WAN-Speed";
	public static final String RES_WAN_SPEED = "res-WAN-Speed";




    public enum FedSocketState {
        NONE, ACCEPTING, ACCEPTED, CONNECTING, CONNECTED, DISCONNECTED
    };
}
