package io.quarkiverse.rabbitmqclient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.rabbitmqclient.util.RabbitMQTestContainer;
import io.quarkiverse.rabbitmqclient.util.RabbitMQTestHelper;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RabbitMQTestContainer.class)
public class QuarkusRabbitMQConsumerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest() // Start unit test with your extension loaded
            .setFlatClassPath(true)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RabbitMQTestHelper.class)
                    .addAsResource(
                            QuarkusRabbitMQConsumerTest.class
                                    .getResource("/rabbitmq/rabbitmq-properties.properties"),
                            "application.properties")
                    .addAsResource(QuarkusRabbitMQConsumerTest.class.getResource("/rabbitmq/ca/cacerts.jks"),
                            "rabbitmq/ca/cacerts.jks")
                    .addAsResource(QuarkusRabbitMQConsumerTest.class.getResource("/rabbitmq/client/client.jks"),
                            "rabbitmq/client/client.jks"));

    @Inject
    RabbitMQTestHelper rabbitMQTestHelper;

    @BeforeEach
    public void setup() throws IOException {
        rabbitMQTestHelper.ssl().declareExchange("receive-test");
        rabbitMQTestHelper.ssl().declareQueue("receive-test-queue", "receive-test");
    }

    @AfterEach
    public void cleanup() throws IOException {
        rabbitMQTestHelper.ssl().deleteQueue("receive-test-queue");
        rabbitMQTestHelper.ssl().deleteExchange("receive-test");
    }

    @Test
    public void testRabbitMQConsumer() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        rabbitMQTestHelper.ssl().basicConsume("receive-test-queue", false, (tag, envelope, properties, body) -> {
            System.out.println(new String(body, StandardCharsets.UTF_8));
            cdl.countDown();
        });

        rabbitMQTestHelper.ssl().send("receive-test", "{'foo':'bar'}");
        Assertions.assertTrue(cdl.await(1, TimeUnit.SECONDS));
    }
}
