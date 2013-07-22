package proxyServer;

import org.apache.http.*;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.entity.EntityDeserializer;
import org.apache.http.impl.entity.LaxContentLengthStrategy;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.HttpRequestParser;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicLineParser;
import org.apache.http.params.BasicHttpParams;

import java.io.ByteArrayInputStream;
import java.io.IOException;



/**
 *
 */
public class ApacheRequestFactory {
    public static HttpRequest create(final String requestAsString) throws IOException, HttpException {

        SessionInputBuffer inputBuffer = new AbstractSessionInputBuffer() {
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
