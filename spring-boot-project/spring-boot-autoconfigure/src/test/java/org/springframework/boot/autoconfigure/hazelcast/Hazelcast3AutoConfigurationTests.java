/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import com.hazelcast.client.impl.clientside.HazelcastClientProxy;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringManagedContext;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastAutoConfiguration} with Hazelcast 3.
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions("hazelcast*.jar")
@ClassPathOverrides({ "com.hazelcast:hazelcast:3.12.8", "com.hazelcast:hazelcast-client:3.12.8",
		"com.hazelcast:hazelcast-spring:3.12.8" })
class Hazelcast3AutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class));

	@Test
	void defaultConfigFile() {
		// no hazelcast-client.xml and hazelcast.xml is present in root classpath
		// this also asserts that XML has priority over YAML
		// as both hazelcast.yaml and hazelcast.xml in test classpath.
		this.contextRunner.run((context) -> {
			Config config = context.getBean(HazelcastInstance.class).getConfig();
			assertThat(config.getConfigurationUrl()).isEqualTo(new ClassPathResource("hazelcast.xml").getURL());
		});
	}

	@Test
	void explicitConfigFileWithXml() {
		HazelcastInstance hazelcastServer = Hazelcast.newHazelcastInstance();
		try {
			this.contextRunner
					.withPropertyValues("spring.hazelcast.config=org/springframework/boot/autoconfigure/"
							+ "hazelcast/hazelcast-client-specific.xml")
					.run(assertSpecificHazelcastClient("explicit-xml"));
		}
		finally {
			hazelcastServer.shutdown();
		}
	}

	@Test
	void autoConfiguredConfigUsesSpringManagedContext() {
		this.contextRunner.run((context) -> {
			Config config = context.getBean(HazelcastInstance.class).getConfig();
			assertThat(config.getManagedContext()).isInstanceOf(SpringManagedContext.class);
		});
	}

	private ContextConsumer<AssertableApplicationContext> assertSpecificHazelcastClient(String label) {
		return (context) -> assertThat(context).getBean(HazelcastInstance.class).isInstanceOf(HazelcastInstance.class)
				.has(labelEqualTo(label));
	}

	private static Condition<HazelcastInstance> labelEqualTo(String label) {
		return new Condition<>((o) -> ((HazelcastClientProxy) o).getClientConfig().getLabels().stream()
				.anyMatch((e) -> e.equals(label)), "Label equals to " + label);
	}

}
