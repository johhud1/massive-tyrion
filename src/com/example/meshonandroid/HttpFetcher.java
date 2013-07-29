package com.example.meshonandroid;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import proxyServer.ApacheRequestFactory;
import adhoc.aodv.Node;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpException;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.StatusLine;
import ch.boye.httpclientandroidlib.client.HttpClient;

import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.DataRepMsg;



public class HttpFetcher implements Runnable {
    String httpRequest;
    Node mNode;
    int mContactId;
    Handler mainActivityMsgHandler;
    int MAX_PAYLOAD_SIZE = Constants.MAX_PAYLOAD_SIZE;
    int BUFSIZE = 512;
    private HttpClient dhc;
    DataMsg dmsg;


    public HttpFetcher(String hr, DataMsg msg, Node n, Handler h, HttpClient dhc) {
        httpRequest = hr;
        mNode = n;
        mainActivityMsgHandler = h;
        mContactId = mNode.getNodeAddress();
        dmsg = msg;
        this.dhc = dhc;
    }


    @Override
    public void run() {
        String tag = "HttpFetcherThread:run";
        int pid = dmsg.getPacketID();
        try {
            // create httpRequest from string representation
            HttpRequest rq = ApacheRequestFactory.create(httpRequest);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // execute requested http request
            HttpHost targetHost = new HttpHost(rq.getFirstHeader("Host").getValue());
            HttpResponse myresp = dhc.execute(targetHost, rq);

            Header[] hs = myresp.getHeaders("Content-length");
            int bufLength = 0;
            if (hs.length > 0) {
                Header hContLength = hs[0];// there may be a
                                           // mismatch between
                                           // contLength and
                                           // buflength, could
                                           // present a problem.
                                           // idk.
                bufLength = new Integer(hContLength.getValue());
            }
            // parse out the response, putting it into out stream
            // handle statusline and headers
            StatusLine respSL = myresp.getStatusLine();
            out.write((respSL.getProtocolVersion().toString() + " " + respSL.getStatusCode() + " "
                       + respSL.getReasonPhrase() + "\n").getBytes(Constants.encoding));
            Header[] headers = myresp.getAllHeaders();
            for (Header h : headers) {
                out.write((h.getName() + ": " + h.getValue() + "\n").getBytes(Constants.encoding));
            }
            out.write(("\r\n").getBytes(Constants.encoding));
            bufLength += out.size();
            // handle response content, if any
            if (myresp.getEntity() != null) {// && bufLength != out.size()) {
                // if entity isn't null, and there's more than just headers to
                // transfer
                InputStream respStream = myresp.getEntity().getContent();
                long contLength = myresp.getEntity().getContentLength();
                Log.d(tag, "response Entity length: " + contLength);

                int redd = 0;
                int offset = out.size();
                int packs = (bufLength / (MAX_PAYLOAD_SIZE - 15000)) + 1;
                // this arbitrary 15k buffer is because base64 increases size.
                // should fix this in a better way
                // ie, don't have a numPacks field, but rather a
                // 'areMorePackets' field in each dataRep, and pid indicates
                // ordering.

                if (contLength > 0) {
                    // stream http response content when size is known
                    byte[] responseBuf =
                        Arrays.copyOf(out.toByteArray(),
                                      Math.min(MAX_PAYLOAD_SIZE - 15000, bufLength));
                    while ((redd =
                        respStream.read(responseBuf, offset, responseBuf.length - offset)) != -1) {
                        if (redd + offset >= responseBuf.length) {
                            /*
                            DataMsg respData =
                                new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(),
                                               Base64.encode(responseBuf, 0), true);
                            byte[] msgBytes = respData.toBytes();
                            mNode.sendData(pid, dmsg.getSourceID(), msgBytes);*/
                            sendBuffer(responseBuf, responseBuf.length, pid);
                            pid++;
                            offset = 0;
                        } else {
                            offset += redd;
                        }
                    }
                    // send remaining data;
                    /*DataMsg respData =
                        new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(), Base64.encode(Arrays
                            .copyOf(responseBuf, offset), 0), false);
                    mNode.sendData(pid, dmsg.getSourceID(), respData.toBytes());*/
                    sendBuffer(responseBuf, offset, pid);
                } else if (contLength == -1) {
                    // stream response data when we don't know the final length
                    byte[] responseBuf = Arrays.copyOf(out.toByteArray(), MAX_PAYLOAD_SIZE - 15000);
                    byte[] tempBuf = new byte[BUFSIZE];
                    while ((redd = respStream.read(tempBuf, 0, BUFSIZE)) > 0) {
                        if (redd + offset >= responseBuf.length) {
                            /*
                             * DataMsg respData = new DataRepMsg(mContactId,
                             * pid, dmsg.getBroadcastID(),
                             * Base64.encode(responseBuf, 0), true); byte[]
                             * msgBytes = respData.toBytes();
                             * mNode.sendData(pid, dmsg.getSourceID(),
                             * msgBytes);
                             */
                            sendBuffer(responseBuf, responseBuf.length, pid);
                            pid++;
                            offset = 0;
                            System.arraycopy(tempBuf, 0, responseBuf, offset, redd);
                            continue;
                        }
                        System.arraycopy(tempBuf, 0, responseBuf, offset, redd);
                        offset += redd;
                    }
                    // send remaining data
                    /*DataMsg respData =
                        new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(), Base64.encode(Arrays
                            .copyOf(responseBuf, offset), 0), false);
                    mNode.sendData(pid, dmsg.getSourceID(), respData.toBytes());*/
                    sendBuffer(responseBuf, offset, pid);
                } else {
                    // send contentless response (headers only)
                    byte[] responseBuf = Arrays.copyOf(out.toByteArray(), bufLength);
                    /*DataMsg respData =
                        new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(),
                                       Base64.encode(responseBuf, 0), false);
                    byte[] msgBytes = respData.toBytes();
                    mNode.sendData(pid, dmsg.getSourceID(), msgBytes);*/
                    sendBuffer(responseBuf, responseBuf.length, pid);
                }
                respStream.close();
                return;
            } else {
                Log.e(tag, "getEntity for this http response was null");
            }
            // send contentless response (headers only)
            byte[] responseBuf = Arrays.copyOf(out.toByteArray(), bufLength);
            DataMsg respData =
                new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(),
                               Base64.encode(responseBuf, 0), false);
            byte[] msgBytes = respData.toBytes();
            mNode.sendData(pid, dmsg.getSourceID(), msgBytes);
        } catch (HttpException e2) {
            e2.printStackTrace();
            sendFailureMsg(pid, e2);

        } catch (IOException e) {
            e.printStackTrace();
            sendFailureMsg(pid, e);
        }

    }


    private void
        sendBuffer(byte[] responseBuf, int limit, int pid) throws UnsupportedEncodingException {
        String tag = "HttpFetcher:sendBuffer";
        Log.d(tag, "sending packet("+pid+") off; size: "+limit/1000+"KB");
        Utils.sendForwardedTrafficMsg(mainActivityMsgHandler, limit);
        if (limit == responseBuf.length) {
            DataMsg respData =
                new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(),
                               Base64.encode(responseBuf, 0), true);
            byte[] msgBytes = respData.toBytes();
            mNode.sendData(pid, dmsg.getSourceID(), msgBytes);
        } else {
            DataMsg respData =
                new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(), Base64.encode(Arrays
                    .copyOf(responseBuf, limit), 0), true);
            byte[] msgBytes = respData.toBytes();
            mNode.sendData(pid, dmsg.getSourceID(), msgBytes);
        }
    }


    private void sendFailureMsg(int pid, Exception e) {
        try {
            mNode.sendData(pid, dmsg.getSourceID(), makeFailureDataRepMsg(e.getMessage())
                .toBytes());
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }


    private DataRepMsg makeFailureDataRepMsg(String msg) {
        try {
            return new DataRepMsg(mContactId, dmsg.getPacketID(), dmsg.getBroadcastID(),
                                  Base64.encode(msg.getBytes(Constants.encoding), 0), false);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new DataRepMsg(mContactId, dmsg.getPacketID(), dmsg.getBroadcastID(),
                                  new byte[0], false);
        }
    }

}
