/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.test.xml;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.xml.router.XPathRouter;
import org.springframework.integration.xml.selector.StringValueTestXPathMessageSelector;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class XmlTests {

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	private PollableChannel wrongMessagesChannel;

	@Autowired
	private PollableChannel splittingChannel;

	@Autowired
	private PollableChannel receivedChannel;

	@Test
	public void testXpathFlow() {
		this.inputChannel.send(new GenericMessage<>("<foo/>"));
		assertNotNull(this.wrongMessagesChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<foo xmlns=\"my:namespace\"/>"));
		assertNotNull(this.wrongMessagesChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<Tags xmlns=\"my:namespace\"/>"));
		assertNotNull(this.splittingChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<Tag xmlns=\"my:namespace\"/>"));
		assertNotNull(this.receivedChannel.receive(10000));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public PollableChannel wrongMessagesChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow xpathFlow() {
			return IntegrationFlows.from("inputChannel")
					.filter(new StringValueTestXPathMessageSelector("namespace-uri(/*)", "my:namespace"),
							e -> e.discardChannel(wrongMessagesChannel()))
					.route(xpathRouter())
					.get();
		}

		@Bean
		public PollableChannel splittingChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel receivedChannel() {
			return new QueueChannel();
		}

		@Bean
		public AbstractMappingMessageRouter xpathRouter() {
			XPathRouter router = new XPathRouter("local-name(/*)");
			router.setEvaluateAsString(true);
			router.setResolutionRequired(false);
			router.setDefaultOutputChannel(wrongMessagesChannel());
			router.setChannelMapping("Tags", "splittingChannel");
			router.setChannelMapping("Tag", "receivedChannel");
			return router;
		}

	}

}
