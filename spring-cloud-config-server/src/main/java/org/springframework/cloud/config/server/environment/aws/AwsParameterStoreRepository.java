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

import com.amazonaws.auth.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.ConfigTokenProvider;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.RepositoryException;
import org.springframework.core.Ordered;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

/**
 * @author Ross Shnaper
 */
public class AwsParameterStoreRepository implements EnvironmentRepository, Ordered {
	private int order;
	private AwsParameterStoreRepositoryProperties properties;
	private ConfigTokenProvider tokenProvider;

	public AwsParameterStoreRepository(AwsParameterStoreRepositoryProperties properties,
		ConfigTokenProvider tokenProvider) {
		this.properties = properties;
		this.order = properties.getOrder();
		this.tokenProvider = tokenProvider;
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		return findOne(application, profile, label, false);
	}

	@Override
	public Environment findOne(String application, String profile, String label, boolean includeOrigin) {
		return null;
	}

	@Override
	public int getOrder() {
		return order;
	}

	private AWSCredentials getClientCredentials() {
		AWSCredentials credentials = null;
		try {
			String encodedCredentials = tokenProvider.getToken();
			if (!StringUtils.isEmpty(encodedCredentials)) {
				byte[] encodedCredentialsJson = Base64Utils.decodeFromString(encodedCredentials);
				//create credentials from json
				ObjectMapper objectMapper = new ObjectMapper();
				credentials = objectMapper.readValue(encodedCredentialsJson, AwsSessionCredentials.class);
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

	private AWSCredentialsProvider createCredentialsProvider() {
		AWSCredentialsProvider credentialsProvider;
		AWSCredentials clientCredentials = getClientCredentials();

		if (clientCredentials != null) {
			credentialsProvider = new AWSStaticCredentialsProvider(clientCredentials);
		}
		else {
			credentialsProvider = new DefaultAWSCredentialsProviderChain();
		}
		return credentialsProvider;
	}
}
