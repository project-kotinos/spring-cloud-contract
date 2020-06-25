/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package contracts;

import java.lang.reflect.Field;
import java.time.Duration;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.DefaultBinderFactory;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RestBase.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
public abstract class RestBase {

	private static final Logger log = LoggerFactory.getLogger(RestBase.class);

	@Value("${APPLICATION_BASE_URL}")
	String url;

	@Value("${APPLICATION_USERNAME:}")
	String username;

	@Value("${APPLICATION_PASSWORD:}")
	String password;

	@Autowired
	ApplicationContext applicationContext;

	@Before
	public void setup() {
		RestAssured.baseURI = this.url;
		if (StringUtils.hasText(this.username)) {
			RestAssured.authentication = RestAssured.basic(this.username, this.password);
		}
	}

	public void triggerMessage(String label) {
		triggerMessage(label, "");
	}

	public void triggerMessage(String label, String queueName) {
		triggerMessage(label, queueName, queueName);
	}

	public void triggerMessage(String label, String groupName, String queueName) {
		if (StringUtils.hasText(queueName)) {
			provisionQueue(groupName, queueName);
		}
		String url = this.url + "/springcloudcontract/" + label;
		log.info("Will send a request to [{}] in order to trigger a message", url);
		restTemplate().postForObject(url, "", String.class);
	}

	private void provisionQueue(String groupName, String queue) {
		DefaultBinderFactory binderFactory = applicationContext.getBean("binderFactory", DefaultBinderFactory.class);
		String binderName = binderName();
		Binder binder = binderFactory.getBinder(binderName, Binder.class);
		Field f = ReflectionUtils.findField(binder.getClass(), "provisioningProvider");
		f.setAccessible(true);
		try {
			ProvisioningProvider provisioner = (ProvisioningProvider) f.get(binder);
			ConsumerDestination consumerDestination = provisioner.provisionConsumerDestination(queue, groupName, new ExtendedConsumerProperties(consumerProperties(binderName)));
			log.info("Provisioned a following consumer destination [{}]", consumerDestination);
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private String binderName() {
		String messagingType = System.getProperty("VERIFIER_MESSAGING_TYPE");
		if (StringUtils.hasText(messagingType)) {
			return messagingType;
		}
		messagingType = System.getenv("VERIFIER_MESSAGING_TYPE");
		return StringUtils.hasText(messagingType) ? messagingType : "rabbit";
	}

	private Object consumerProperties(String binderName) {
		try {
			Class clazz;
			if ("kafka".equals(binderName)) {
				clazz = Class.forName("org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties");
			}
			else {
				clazz = Class.forName("org.springframework.cloud.stream.binder.rabbit.properties.RabbitConsumerProperties");
			}
			return clazz.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private RestTemplate restTemplate() {
		RestTemplateBuilder builder = new RestTemplateBuilder()
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(5));
		if (StringUtils.hasText(this.username)) {
			builder = builder.basicAuthentication(this.username, this.password);
		}
		return builder.build();
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class Config {

	}

}
