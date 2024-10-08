package com.yas.search.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class IntegrationTestConfiguration extends ElasticsearchContainer implements
    ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Bean(destroyMethod = "stop")
    public KeycloakContainer keycloakContainer(DynamicPropertyRegistry registry) {
        KeycloakContainer keycloak = new KeycloakContainer()
            .withRealmImportFiles("/test-realm.json")
            .withReuse(true);

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
            () -> keycloak.getAuthServerUrl() + "/realms/quarkus");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            () -> keycloak.getAuthServerUrl() + "/realms/quarkus/protocol/openid-connect/certs");
        return keycloak;
    }

    @Bean(destroyMethod = "stop")
    @ServiceConnection
    public ElasticTestContainer elasticTestContainer() {
        ElasticTestContainer elasticTestContainer = new ElasticTestContainer();
        elasticTestContainer.start();
        return elasticTestContainer;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.2.2.arm64"));
        kafka.start();
        setProperties(environment, "spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
    }

    private void setProperties(ConfigurableEnvironment environment, String name, Object value) {
        MutablePropertySources sources = environment.getPropertySources();
        PropertySource<?> source = sources.get(name);
        if (source == null) {
            source = new MapPropertySource(name, new HashMap<>());
            sources.addFirst(source);
        }
        ((Map<String, Object>) source.getSource()).put(name, value);
    }
}
