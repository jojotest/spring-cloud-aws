/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@Configuration
@Import(ContextDefaultConfigurationRegistrar.class)
public class SqsConfiguration {

	@Autowired(required = false)
	private AWSCredentialsProvider awsCredentialsProvider;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private ResourceIdResolver resourceIdResolver;

	@Autowired(required = false)
	private final SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory = new SimpleMessageListenerContainerFactory();

	@Autowired(required = false)
	private final QueueMessageHandlerFactory queueMessageHandlerFactory = new QueueMessageHandlerFactory();

	@Bean
	public SimpleMessageListenerContainer simpleMessageListenerContainer(AmazonSQSAsync amazonSqs) {
		if (this.simpleMessageListenerContainerFactory.getAmazonSqs() == null) {
			this.simpleMessageListenerContainerFactory.setAmazonSqs(amazonSqs);
		}
		if (this.simpleMessageListenerContainerFactory.getResourceIdResolver() == null) {
			this.simpleMessageListenerContainerFactory.setResourceIdResolver(this.resourceIdResolver);
		}

		SimpleMessageListenerContainer simpleMessageListenerContainer = this.simpleMessageListenerContainerFactory.createSimpleMessageListenerContainer();
		simpleMessageListenerContainer.setMessageHandler(queueMessageHandler(amazonSqs));
		return simpleMessageListenerContainer;
	}

	@Bean
	public QueueMessageHandler queueMessageHandler(AmazonSQS amazonSqs) {
		if (this.simpleMessageListenerContainerFactory.getQueueMessageHandler() != null) {
			return this.simpleMessageListenerContainerFactory.getQueueMessageHandler();
		} else {
			return getMessageHandler(amazonSqs);
		}
	}

	private QueueMessageHandler getMessageHandler(AmazonSQS amazonSqs) {
		if (this.queueMessageHandlerFactory.getAmazonSqs() == null) {
			this.queueMessageHandlerFactory.setAmazonSqs(amazonSqs);
		}

		return this.queueMessageHandlerFactory.createQueueMessageHandler();
	}

	@Lazy
	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingAmazonClient(AmazonSQS.class)
	public AmazonSQSAsync amazonSQS() {
		AmazonSQSAsyncClient amazonSQSAsyncClient;
		if (this.awsCredentialsProvider != null) {
			amazonSQSAsyncClient = new AmazonSQSAsyncClient(this.awsCredentialsProvider);
		} else {
			amazonSQSAsyncClient = new AmazonSQSAsyncClient();
		}

		if (this.regionProvider != null) {
			amazonSQSAsyncClient.setRegion(this.regionProvider.getRegion());
		}

		return new AmazonSQSBufferedAsyncClient(amazonSQSAsyncClient);
	}
}
