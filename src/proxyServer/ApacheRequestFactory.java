package proxyServer;


import java.io.ByteArrayInputStream;
import java.io.IOException;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpException;
import ch.boye.httpclientandroidlib.HttpMessage;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.impl.DefaultHttpRequestFactory;
import ch.boye.httpclientandroidlib.impl.entity.EntityDeserializer;
import ch.boye.httpclientandroidlib.impl.entity.LaxContentLengthStrategy;
import ch.boye.httpclientandroidlib.impl.io.AbstractSessionInputBuffer;
import ch.boye.httpclientandroidlib.impl.io.HttpRequestParser;
import ch.boye.httpclientandroidlib.io.HttpMessageParser;
import ch.boye.httpclientandroidlib.io.SessionInputBuffer;
import ch.boye.httpclientandroidlib.message.BasicHttpEntityEnclosingRequest;
import ch.boye.httpclientandroidlib.message.BasicLineParser;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;



/**
 *
 */
public class ApacheRequestFactory {
    public static HttpRequest create(final String requestAsString) throws IOException, HttpException {

        SessionInputBuffer inputBuffer = new AbstractSessionInputBuffer(){
            {
                init(new ByteArrayInputStream(requestAsString.getBytes()), 10,
                     new BasicHttpParams());
            }


            @Override
            public boolean isDataAvailable(int timeout) throws IOException {
                throw new RuntimeException("have to override but probably not even called");
            }
        };
        HttpMessageParser parser =
            new HttpRequestParser(inputBuffer,
                                  new BasicLineParser(new ProtocolVersion("HTTP", 1, 1)),
                                  new DefaultHttpRequestFactory(), new BasicHttpParams());
        HttpMessage message = parser.parse();
        if (message instanceof BasicHttpEntityEnclosingRequest) {
            BasicHttpEntityEnclosingRequest request = (BasicHttpEntityEnclosingRequest) message;
            EntityDeserializer entityDeserializer =
                new EntityDeserializer(new LaxContentLengthStrategy());
            HttpEntity entity = entityDeserializer.deserialize(inputBuffer, message);
            request.setEntity(entity);
        }
        return (HttpRequest) message;

    }
}
