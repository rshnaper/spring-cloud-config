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

import com.amazonaws.auth.BasicSessionCredentials;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.jackson.JsonComponent;

/**
 * Subclass of {@link BasicSessionCredentials} with JSON serialization/deserialization support.
 *
 * @author Ross Shnaper
 */
@JsonComponent
public class AwsSessionCredentialsWrapper extends BasicSessionCredentials {

	public AwsSessionCredentialsWrapper(BasicSessionCredentials delegate) {
		this(delegate.getAWSAccessKeyId(), delegate.getAWSSecretKey(), delegate.getSessionToken());
	}

	@JsonCreator
	public AwsSessionCredentialsWrapper(
		@JsonProperty("AccessKeyId") String awsAccessKeyId,
		@JsonProperty("SecretAccessKey") String awsSecretKey,
		@JsonProperty("SessionToken") String sessionToken) {
		super(awsAccessKeyId, awsSecretKey, sessionToken);
	}

	@Override
	@JsonProperty("AccessKeyId")
	public String getAWSAccessKeyId() {
		return super.getAWSAccessKeyId();
	}

	@Override
	@JsonProperty("SecretAccessKey")
	public String getAWSSecretKey() {
		return super.getAWSSecretKey();
	}

	@Override
	@JsonProperty("SessionToken")
	public String getSessionToken() {
		return super.getSessionToken();
	}
}
