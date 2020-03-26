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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.server.support.EnvironmentRepositoryProperties;
import org.springframework.core.Ordered;

/**
 * @author Ross Shnaper
 */
@ConfigurationProperties("spring.cloud.config.server.awsparameterstore")
public class AwsParameterStoreRepositoryProperties implements EnvironmentRepositoryProperties {
	/**
	 * Default path separator
	 */
	public static final String PATH_SEPARATOR = "/";

	private int order = Ordered.LOWEST_PRECEDENCE;
	private String profile;
	private String pathPrefix;
	private String region;

	public int getOrder() {
		return this.order;
	}

	@Override
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Profile to use for fetching parameters.
	 * @return
	 */
	public String getProfile() {
		return profile;
	}

	/**
	 * Profile to use for fetching parameters.
	 * @return
	 */
	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getPathPrefix() {
		return pathPrefix;
	}

	public void setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}
}
