package com.example.fanoutproxy.config;

import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DownstreamHttpClientConfig {

    @Bean
    SSLContextParameters trustAllSslContextParameters() {
        TrustManagersParameters trustManagers = new TrustManagersParameters();
        trustManagers.setTrustManager(trustAllManager());

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagers);
        return sslContextParameters;
    }

    @Bean
    HostnameVerifier trustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }

    @Bean
    CamelContextConfiguration downstreamSslVerificationConfiguration(
            SSLContextParameters trustAllSslContextParameters,
            HostnameVerifier trustAllHostnameVerifier
    ) {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                configureHttpComponent(camelContext, "http", trustAllSslContextParameters, trustAllHostnameVerifier);
                configureHttpComponent(camelContext, "https", trustAllSslContextParameters, trustAllHostnameVerifier);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // No-op.
            }
        };
    }

    private void configureHttpComponent(
            CamelContext camelContext,
            String componentName,
            SSLContextParameters sslContextParameters,
            HostnameVerifier hostnameVerifier
    ) {
        HttpComponent component = camelContext.getComponent(componentName, HttpComponent.class);
        component.setSslContextParameters(sslContextParameters);
        component.setX509HostnameVerifier(hostnameVerifier);
    }

    private TrustManager trustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
