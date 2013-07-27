package com.example.meshonandroid;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import proxyServer.ApacheRequestFactory;
import adhoc.aodv.Node;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.DataRepMsg;



public class HttpFetcher implements Runnable {
    String httpRequest;
    Node mNode;
    int mContactId;
    Handler mainActivityMsgHandler;
    int MAX_PAYLOAD_SIZE = Constants.MAX_PAYLOAD_SIZE;
    int BUFSIZE= 512;
    DataMsg dmsg;


    public HttpFetcher(String hr, DataMsg msg, Node n, Handler h) {
        httpRequest = hr;
        mNode = n;
        mainActivityMsgHandler = h;
        mContactId = mNode.getNodeAddress();
        dmsg = msg;
    }


    @Override
    public void run() {
        String tag = "HttpFetcherThread:run";
        int pid = dmsg.getPacketID();
        try {
            // create httpRequest from string representation
            HttpRequest rq = ApacheRequestFactory.create(httpRequest);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DefaultHttpClient dhc = new DefaultHttpClient();
            dhc.setRedirectHandler(new RedirectHandler() {

                @Override
                public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
                    /*if(response.getStatusLine().getStatusCode() == 301){
                        return true;
                    }*/
                    return false;
                }


                @Override
                public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
                    Header uri = response.getFirstHeader("Location");
                    if(uri != null){
                        try {
                            return new URI(uri.getValue());
                        } catch (URISyntaxException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            });

            // execute requested http request
            HttpHost targetHost = new HttpHost(rq.getFirstHeader("Host").getValue());
            HttpResponse myresp = dhc.execute(targetHost, rq);

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
            // handle response content, if any
            if (myresp.getEntity() != null) {
                InputStream respStream =
                    myresp.getEntity().getContent();
                long contLength = myresp.getEntity().getContentLength();
                Log.d(tag, "response Entity length: " + contLength);
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
                int redd = 0;
                int offset = 0;
                byte[] responseBuf;
                if (contLength > 0) {
                    // write the response from responseBuf to out
                    responseBuf = new byte[bufLength];
                    while ((redd = respStream.read(responseBuf, offset, bufLength)) != -1) {
                        out.write(responseBuf, offset, redd);
                        offset += redd;
                    }
                } else {
                    responseBuf = new byte[BUFSIZE];
                    while ((redd = respStream.read(responseBuf, 0, BUFSIZE)) > 0) {
                        out.write(responseBuf, 0, redd);
                    }
                }
                respStream.close();
            }

            // base64 encode out (holding response data) and send it
            // back to
            // originator
            byte[] outBArray = Base64.encode(out.toByteArray(), 0);
            Utils.sendForwardedTrafficMsg(mainActivityMsgHandler, outBArray.length);
            if (outBArray.length > MAX_PAYLOAD_SIZE) {
                // if response is too big to fit in one packet, chop
                // it
                // up.
                int offset = 0;
                int packs = (outBArray.length / MAX_PAYLOAD_SIZE) + 1;
                Log.d(tag, "user data to large (" + outBArray.length
                           + ") to send over aodv in 1 msg. splitting into " + packs + " msgs");
                for (int i = 0; i < packs; i++) {
                    byte[] temp = new byte[Math.min(MAX_PAYLOAD_SIZE, outBArray.length - offset)];
                    try {
                        temp = Arrays.copyOfRange(outBArray, offset, offset + temp.length);
                        DataMsg respData =
                            new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(), temp, packs);
                        byte[] msgBytes = respData.toBytes();
                        mNode.sendData(pid, dmsg.getSourceID(), msgBytes);
                        pid++;
                        offset += MAX_PAYLOAD_SIZE;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // response fits in one packet, send 'er off
                DataMsg respData =
                    new DataRepMsg(mContactId, pid, dmsg.getBroadcastID(), outBArray);
                mNode.sendData(pid, dmsg.getSourceID(), respData.toBytes());
            }
        } catch (HttpException e2) {
            e2.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
