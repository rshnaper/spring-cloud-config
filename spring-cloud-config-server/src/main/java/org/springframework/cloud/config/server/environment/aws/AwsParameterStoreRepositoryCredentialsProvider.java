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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.config.server.environment.ConfigTokenProvider;
import org.springframework.cloud.config.server.environment.RepositoryException;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

/**
 * @author Ross Shnaper
 */
public class AwsParameterStoreRepositoryCredentialsProvider implements AWSCredentialsProvider {
	private ConfigTokenProvider tokenProvider;
	private AwsParameterStoreRepositoryProperties properties;

	public AwsParameterStoreRepositoryCredentialsProvider(AwsParameterStoreRepositoryProperties properties, ConfigTokenProvider tokenProvider) {
		this.properties = properties;
		this.tokenProvider = tokenProvider;
	}

	@Override
	public AWSCredentials getCredentials() {
		return getCredentialsProviderDelegate().getCredentials();
	}

	@Override
	public void refresh() {
		getCredentialsProviderDelegate().refresh();
	}

	protected AWSCredentialsProvider getCredentialsProviderDelegate() {
		AWSCredentialsProvider credentialsProvider;
		AWSCredentials clientCredentials = getClientCredentials();

		if (clientCredentials != null) {
			credentialsProvider = new AWSStaticCredentialsProvider(clientCredentials);
		}
		else if (!StringUtils.isEmpty(properties.getAwsProfile())) {
			credentialsProvider = new ProfileCredentialsProvider(properties.getAwsProfile());
		}
		else {
			credentialsProvider = new DefaultAWSCredentialsProviderChain();
		}
		return credentialsProvider;
	}

	private AWSCredentials getClientCredentials() {
		AWSCredentials credentials = null;
		try {
			String encodedCredentials = tokenProvider.getToken();
			if (!StringUtils.isEmpty(encodedCredentials)) {
				byte[] encodedCredentialsJson = Base64Utils.decodeFromString(encodedCredentials);
				//create credentials from json
				ObjectMapper objectMapper = new ObjectMapper();
				credentials = objectMapper.readValue(encodedCredentialsJson, AwsSessionCredentialsWrapper.class);
			}
		}
		catch (IllegalArgumentException e) {
			//client did not send credentials token
		}
		catch (Exception e) {
			throw new RepositoryException("Unable to parse client temporary credentials.", e);
		}
		return credentials;
	}
}
