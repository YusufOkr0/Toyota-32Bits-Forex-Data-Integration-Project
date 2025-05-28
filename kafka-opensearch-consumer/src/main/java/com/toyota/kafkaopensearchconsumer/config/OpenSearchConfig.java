package com.toyota.kafkaopensearchconsumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@RequiredArgsConstructor
@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host}")
    String host;
    @Value("${opensearch.port}")
    int port;

    @Value("${opensearch.username}")
    String username;
    @Value("${opensearch.password}")
    String password;

    private final ObjectMapper objectMapper;

    @Bean
    public HttpHost httpHost() {
        return new HttpHost(host, port, "https");
    }

    @Bean
    public CredentialsProvider credentialsProvider() {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return credentialsProvider;
    }

    @Bean
    public SSLContext sslContext() {
        try {
            var trustStore = KeyStore.getInstance("JKS");
            try (InputStream inputStream = new ClassPathResource("opensearch-truststore.jks").getInputStream()) {
                trustStore.load(inputStream, "password".toCharArray());
            }

            var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SSLContext for OpenSearch.", e);
        }
    }

    @Bean
    public OpenSearchClient openSearchClient(HttpHost httpHost,
                                             CredentialsProvider credentialsProvider,
                                             SSLContext sslContext) {

        RestClient restClient = RestClient
                .builder(httpHost)
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setSSLContext(sslContext)
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                ).build();

        OpenSearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper(objectMapper)
        );
        return new OpenSearchClient(transport);
    }
}