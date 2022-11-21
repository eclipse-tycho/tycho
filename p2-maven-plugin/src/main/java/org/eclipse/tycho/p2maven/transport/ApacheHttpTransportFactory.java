package org.eclipse.tycho.p2maven.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = HttpTransportFactory.class, hint = ApacheHttpTransportFactory.HINT)
public class ApacheHttpTransportFactory implements HttpTransportFactory {

	static final String HINT = "httpcore5";
	CloseableHttpClient httpclient;

	public ApacheHttpTransportFactory() {
		HttpClientBuilder builder = HttpClients.custom().disableRedirectHandling().disableContentCompression();
		PoolingHttpClientConnectionManagerBuilder cmBuilder = PoolingHttpClientConnectionManagerBuilder.create()
				.setMaxConnPerRoute(100)//
				.setMaxConnTotal(300)
//				.setConnectionTimeToLive(TimeValue.ofMilliseconds(DEFAULT_CONNECTION_TTL))
				// .setDefaultSocketConfig(DEFAULT_SOCKET_CONFIG)
				.setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
				.setConnPoolPolicy(PoolReusePolicy.LIFO);
//		configureSSLSocketFactory(cmBuilder);
		builder.setConnectionManager(cmBuilder.build());
		httpclient = builder.build();
	}

	@Override
	public HttpTransport createTransport(URI uri) {
		return new ApacheHttpTransport(uri, httpclient);
	}

	private static final class ApacheHttpTransport implements HttpTransport {

		private URI uri;
		private HttpGet httpget;
		private CloseableHttpClient httpclient;

		public ApacheHttpTransport(URI uri, CloseableHttpClient httpclient) {
			this.uri = uri;
			this.httpclient = httpclient;
			httpget = new HttpGet(uri);
		}

		@Override
		public void setHeader(String key, String value) {
			httpget.setHeader(key, value);

		}

		@Override
		public Response<InputStream> get() throws IOException {
//			httpclient.execute(httpget, response -> {
//				System.out.println("----------------------------------------");
//				System.out.println(httpget + "->" + new StatusLine(response));
//				// Process response message and convert it into a value object
//				return new Result(response.getCode(), EntityUtils.toString(response.getEntity()));
//			});
			CloseableHttpResponse response = httpclient.execute(httpget);
			// TODO Auto-generated method stub
			return new Response<InputStream>() {

				@Override
				public int statusCode() throws IOException {
					// TODO Auto-generated method stub
					return 200;
				}

				@Override
				public Map<String, List<String>> headers() {
					return Arrays.stream(response.getHeaders()).collect(Collectors.groupingBy(Header::getName,
							Collectors.mapping(Header::getValue, Collectors.toList())));
				}

				@Override
				public String getHeader(String header) {
					Header h;
					try {
						h = response.getHeader(header);
						if (h != null) {
							return h.getValue();
						}
					} catch (ProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}

				@Override
				public void close() {
					try {
						response.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				@Override
				public InputStream body() throws IOException {
					return response.getEntity().getContent();
				}
			};
		}

		@Override
		public Response<Void> head() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
