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
import org.springframework.cloud.config.server.environment.EnvironmentRepositoryFactory;
import org.springframework.util.StringUtils;

/**
 * @author Ross Shnaper
 */
public class AwsParameterStoreEnvironmentRespositoryFactory implements
	EnvironmentRepositoryFactory<AwsParameterStoreRepository, AwsParameterStoreRepositoryProperties> {

	private AwsParameterStoreRepositoryCredentialsProvider credentialsProvider;

	public AwsParameterStoreEnvironmentRespositoryFactory(AwsParameterStoreRepositoryCredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public AwsParameterStoreRepository build(
		AwsParameterStoreRepositoryProperties environmentProperties) {
		AWSSimpleSystemsManagement ssmClient = getSsmClient(environmentProperties);
		return new AwsParameterStoreRepository(environmentProperties, ssmClient);
	}

	private AWSSimpleSystemsManagement getSsmClient(AwsParameterStoreRepositoryProperties properties) {
		AWSSimpleSystemsManagementClientBuilder builder =  AWSSimpleSystemsManagementClient
			.builder();
		builder.withCredentials(this.credentialsProvider);
		if (!StringUtils.isEmpty(properties.getRegion())) {
			builder.withRegion(properties.getRegion());
		}
		return builder.build();
	}
}
