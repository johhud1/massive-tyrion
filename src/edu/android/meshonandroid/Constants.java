package edu.android.meshonandroid;

public class Constants {
    public static final String encoding = "UTF-8";

    public static final byte PDU_EXITNODEREQ = 1;
    public static final byte PDU_DATAMSG = 2;
    public static final byte PDU_EXITNODEREP = 3;
    public static final byte PDU_DATAREQMSG = 4;
    public static final byte PDU_DATAREPMSG = 5;
    public static final byte PDU_IPDISCOVER = 6;
    public static final byte PDU_CONNECTDATAMSG = 7;
    public static final byte PDU_CONNECTIONCLOSEMSG = 8;

    public static final int EXITNODEREP_WAITTIME=700;
    public static final int MAX_NODES = 254;

    public static final String fakeResp = "HTTP/1.1 200 OK\n" + "Date: Mon, 23 May 2005 22:38:34 GMT\n"
        + "Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\n"
        + "Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n"
        + "Etag: \"3f80f-1b6-3e1cb03b\"\n"
        + "Content-Type: text/html; charset=UTF-8\n" + "Content-Length: 240\n"
        + "Connection: close\n" + "request recieved\r\n\r\n";

    public static final int LOCALPROXY_ACCEPT_TIMEOUT = 100;

    public static final int IP_staleness_time = 5; //in minutes

    public static final int STATUS_MSG_CODE = 0;
    public static final int LOG_MSG_CODE = 1;
    public static final int TTM_MSG_CODE = 2; //traffic through mesh (ie data the mesh has acquired on our behalf)
    public static final int TFM_MSG_CODE = 3; //traffic from mesh (ie data for the mesh that we have downloaded on their behalf)

    enum ContactSelectionStrategy{RoundRobin, FastestFirst};

    public static final ContactSelectionStrategy CONTACT_STRATEGY = ContactSelectionStrategy.RoundRobin;

    public static final int CONTACT_STALENESSTIME = -1;

    protected static final String INIT_SCRIPT_PATH = "/data/mybin/init.sh";

    protected static final String MESH_MSG_KEY = "MSGKEY";
    protected static final String MESH_MSG_CODE_KEY = "MSGCODEKEY";

    public static final String MESH_WIFI_SSID = "nexusbac";

    public static int MAX_PAYLOAD_SIZE = adhoc.aodv.Constants.MAX_PACKAGE_SIZE - (4 * 1000); // 8
                                                                                              // ints
                                                                                              // times
                                                                                              // 4
                                                                                              // bytes
                                                                                              // per
                                                                                              // int
                                                                                              // =
                                                                                              // total
                                                                                              // reserved
                                                                                              // bytes;

    public static final int WIFI_SCAN_SLEEP_INTERVAL = 1000;
}
