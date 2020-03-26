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
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.ConfigTokenProvider;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.RepositoryException;
import org.springframework.cloud.config.server.support.EnvironmentRepositoryProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ross Shnaper
 */
public class AwsParameterStoreRepository implements EnvironmentRepository, Ordered {
	private final static String PROPERTY_SOURCE_NAME_PREFIX = "aws-parameterstore:";
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
		String path = StringUtils.isEmpty(properties.getPathPrefix()) ? "" : properties.getPathPrefix();
		path += AwsParameterStoreRepositoryProperties.PATH_SEPARATOR + application;

		if (!StringUtils.isEmpty(profile)) {
			path += AwsParameterStoreRepositoryProperties.PATH_SEPARATOR + profile;
		}

		Environment environment = new Environment(application, new String[]{profile}, label, null, null);
		environment.add(createPropertySource(path));
		return environment;
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
		else if (!StringUtils.isEmpty(properties.getProfile())) {
			credentialsProvider = new ProfileCredentialsProvider(properties.getProfile());
		}
		else {
			credentialsProvider = new DefaultAWSCredentialsProviderChain();
		}
		return credentialsProvider;
	}

	private List<Parameter> getParameters(String path) {
		AWSCredentialsProvider credentialsProvider = createCredentialsProvider();
		AWSSimpleSystemsManagementClientBuilder builder =  AWSSimpleSystemsManagementClient.builder();
		builder.withCredentials(credentialsProvider);
		if (!StringUtils.isEmpty(properties.getRegion())) {
			builder.withRegion(properties.getRegion());
		}
		AWSSimpleSystemsManagement ssmClient = builder.build();

		GetParametersByPathRequest request = new GetParametersByPathRequest();
		request.withPath(path).withWithDecryption(true).withRecursive(true);
		GetParametersByPathResult result = ssmClient.getParametersByPath(request);
		return result != null ? result.getParameters() : null;
	}

	private PropertySource createPropertySource(String path) {
		Map<String, String> parameterMap = new HashMap<>();
		String paramName;
		List<Parameter> parameters = getParameters(path);
		if (StringUtils.isEmpty(parameters)) {
			for (Parameter parameter : getParameters(path)) {
				paramName = parameter.getName().replace(path, "")
					.replace(AwsParameterStoreRepositoryProperties.PATH_SEPARATOR, ".");
				parameterMap.put(paramName, parameter.getValue());
			}
		}
		return new PropertySource(PROPERTY_SOURCE_NAME_PREFIX + path, parameterMap);
	}
}
