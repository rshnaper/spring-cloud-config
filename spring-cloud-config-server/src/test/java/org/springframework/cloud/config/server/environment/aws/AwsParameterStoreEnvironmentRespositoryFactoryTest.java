/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.config.server.environment.aws;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ross Shnaper
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsParameterStoreEnvironmentRespositoryFactoryTest {
	@Mock
	private AwsParameterStoreRepositoryProperties properties;

	@Mock
	private AwsParameterStoreRepositoryCredentialsProvider credentialsProvider;

	@Spy
	@InjectMocks
	private AwsParameterStoreEnvironmentRespositoryFactory factory;

	@Test
	public void testCustomRegion() {
		AWSSimpleSystemsManagementClientBuilder builder = AWSSimpleSystemsManagementClient.builder();
		when(properties.getRegion()).thenReturn("custom-region");
		when(factory.getBuilder()).thenReturn(builder);

		AWSSimpleSystemsManagement ssmClient = factory.getSsmClient(properties);

		assertThat(ssmClient, notNullValue());
		assertThat(builder.getRegion(), equalTo("custom-region"));
	}

	@Test
	public void testCredentialsProvider() {
		AWSSimpleSystemsManagementClientBuilder builder = AWSSimpleSystemsManagementClient.builder();
		when(factory.getBuilder()).thenReturn(builder);

		AWSSimpleSystemsManagement ssmClient = factory.getSsmClient(properties);

		assertThat(ssmClient, notNullValue());
		assertThat(builder.getCredentials(), equalTo(credentialsProvider));
	}

	@Test
	public void testBuildRespository() {
		AwsParameterStoreRepository respository = factory.build(properties);

		assertThat(respository, notNullValue());
		verify(factory).getBuilder();
		verify(factory).getSsmClient(properties);
	}
}