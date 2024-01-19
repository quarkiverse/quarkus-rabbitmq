package io.quarkiverse.rabbitmqclient;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.rabbitmqclient.util.OtherClientService;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.test.QuarkusUnitTest;

public class QuarkusRabbitMQNoDefaultClientTest extends RabbitMQConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest() // Start unit test with your extension loaded
            .setFlatClassPath(true)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(OtherClientService.class)
                    .addAsResource(QuarkusRabbitMQNoDefaultClientTest.class.getResource("/no-default.properties"),
                            "application.properties"));

    @Test
    public void haveOnlyOneClient() {
        ArcContainerImpl container = (ArcContainerImpl) Arc.container();
        List<InjectableBean<?>> clients = container.getBeans().stream()
                .filter(b -> b.getBeanClass().equals(RabbitMQClient.class))
                .collect(Collectors.toList());

        Assertions.assertNotNull(clients);
        Assertions.assertEquals(1, clients.size());

        InjectableBean<?> client = clients.get(0);
        Assertions.assertNotNull(client);

        Set<Annotation> annotations = client.getQualifiers();
        Assertions.assertTrue(annotations.stream().anyMatch(a -> a.annotationType().equals(NamedRabbitMQClient.class)));
        Assertions.assertTrue(annotations.stream().filter(a -> a.annotationType().equals(NamedRabbitMQClient.class))
                .map(a -> ((NamedRabbitMQClient) a).value()).anyMatch(n -> n.equals("other")));
    }
}
