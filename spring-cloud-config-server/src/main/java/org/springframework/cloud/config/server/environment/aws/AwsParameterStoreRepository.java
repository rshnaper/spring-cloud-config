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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;

import org.springframework.cloud.aws.paramstore.AwsParamStorePropertySource;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * @author Ross Shnaper
 */
public class AwsParameterStoreRepository implements EnvironmentRepository, Ordered {
	private final static String PROPERTY_SOURCE_NAME_PREFIX = "aws-parameterstore:";
	private int order;
	private AwsParameterStoreRepositoryProperties properties;
	private AWSSimpleSystemsManagement ssmClient;

	public AwsParameterStoreRepository(AwsParameterStoreRepositoryProperties properties,
		AWSSimpleSystemsManagement ssmClient) {
		this.properties = properties;
		this.order = properties.getOrder();
		this.ssmClient = ssmClient;
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		return findOne(application, profile, label, false);
	}

	@Override
	public Environment findOne(String application, String profile, String label, boolean includeOrigin) {
		String[] activeProfiles = profile.split(",");
		Environment environment = new Environment(application, activeProfiles, label, null, null);

		for (String activeProfile : activeProfiles) {
			String path = getPath(properties, application, activeProfile);
			environment.add(createPropertySource(path));
		}

		return environment;
	}

	@Override
	public int getOrder() {
		return order;
	}

	private String getPath(AwsParameterStoreRepositoryProperties properties, String application, String profile) {
		String profileSeparator = !StringUtils.isEmpty(properties.getProfileSeparator()) ?
			properties.getProfileSeparator() :
			AwsParameterStoreRepositoryProperties.PATH_SEPARATOR;

		String path = StringUtils.isEmpty(properties.getPathPrefix()) ? AwsParameterStoreRepositoryProperties.PATH_SEPARATOR : properties.getPathPrefix();
		path += application + profileSeparator + profile + AwsParameterStoreRepositoryProperties.PATH_SEPARATOR;

		return path;
	}

	private PropertySource createPropertySource(String path) {
		AwsParamStorePropertySource source = new AwsParamStorePropertySource(path, ssmClient);
		source.init();

		Map<String, String> properties = Arrays.stream(source.getPropertyNames())
				.collect(Collectors.toMap(Function.identity(), property -> (String) source.getProperty(property)));

		return new PropertySource(PROPERTY_SOURCE_NAME_PREFIX + path, properties);
	}
}
