/*
 * Copyright 2016-2019 the original author or authors.
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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.util.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.config.server.environment.ConfigTokenProvider;
import org.springframework.util.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.when;

/**
 * @author Ross Shnaper
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsParameterStoreRepositoryCredentialsProviderTest {
	@Mock
	private ConfigTokenProvider tokenProvider;

	@Mock
	private AwsParameterStoreRepositoryProperties properties;

	@InjectMocks
	@Spy
	private AwsParameterStoreRepositoryCredentialsProvider credentialsProvider;


	@Test
	public void testClientCredentials() {
		String credentials = "{\"AccessKeyId\":\"accessKey\",\"SecretAccessKey\":\"secretKey\",\"SessionToken\":\"sessionToken\"}";
		when(tokenProvider.getToken()).thenReturn(Base64.encodeAsString(credentials.getBytes()));

		AWSCredentials awsCredentials = credentialsProvider.getCredentials();
		assertThat(awsCredentials, instanceOf(AWSSessionCredentials.class));
		assertThat(awsCredentials.getAWSAccessKeyId(), equalTo("accessKey"));
		assertThat(awsCredentials.getAWSSecretKey(), equalTo("secretKey"));
		assertThat(((AWSSessionCredentials) awsCredentials).getSessionToken(), equalTo("sessionToken"));
	}

	@Test
	public void testSystemCredentials() {
		String accessKey = System.getProperty("aws.accessKeyId");
		String secretKey = System.getProperty("aws.secretKey");
		String sessionToken = System.getProperty("aws.sessionToken");
		try {
			System.setProperty("aws.accessKeyId", "accessKey");
			System.setProperty("aws.secretKey", "secretKey");
			System.setProperty("aws.sessionToken", "sessionToken");

			AWSCredentials awsCredentials = credentialsProvider.getCredentials();
			assertThat(awsCredentials, instanceOf(AWSSessionCredentials.class));
			assertThat(awsCredentials.getAWSAccessKeyId(), equalTo("accessKey"));
			assertThat(awsCredentials.getAWSSecretKey(), equalTo("secretKey"));
			assertThat(((AWSSessionCredentials) awsCredentials).getSessionToken(), equalTo("sessionToken"));
		}
		finally {
			if (!StringUtils.isEmpty(accessKey)) {
				System.setProperty("aws.accessKeyId", accessKey);
			}

			if (!StringUtils.isEmpty(secretKey)) {
				System.setProperty("aws.secretKey", secretKey);
			}

			if (!StringUtils.isEmpty(sessionToken)) {
				System.setProperty("aws.sessionToken", sessionToken);
			}
		}
	}

	@Test
	public void testCustomProfileCredentials() {
		when(tokenProvider.getToken()).thenReturn(null);
		when(properties.getAwsProfile()).thenReturn("customProfile");

		assertThat(credentialsProvider.getCredentialsProviderDelegate(), instanceOf(ProfileCredentialsProvider.class));
	}

	@Test
	public void testDefaultCredentials() {
		when(tokenProvider.getToken()).thenReturn(null);
		when(properties.getAwsProfile()).thenReturn(null);

		assertThat(credentialsProvider.getCredentialsProviderDelegate(), instanceOf(
				DefaultAWSCredentialsProviderChain.class));
	}
}