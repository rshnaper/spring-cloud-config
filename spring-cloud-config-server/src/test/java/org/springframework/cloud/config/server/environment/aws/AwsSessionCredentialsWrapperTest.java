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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Ross Shnaper
 */
public class AwsSessionCredentialsWrapperTest {
	@Test
	public void testSerialization() throws JsonProcessingException {
		AwsSessionCredentialsWrapper credentialsWrapper = new AwsSessionCredentialsWrapper("accessKey", "secretKey", "sessionToken");
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writerFor(AwsSessionCredentialsWrapper.class).writeValueAsString(credentialsWrapper);
		assertThat(json, equalTo("{\"AccessKeyId\":\"accessKey\",\"SecretAccessKey\":\"secretKey\",\"SessionToken\":\"sessionToken\"}"));
	}

	@Test
	public void testDeserialization() throws JsonProcessingException {
		String json = "{\"AccessKeyId\":\"accessKey\",\"SecretAccessKey\":\"secretKey\",\"SessionToken\":\"sessionToken\"}";
		ObjectMapper mapper = new ObjectMapper();
		AwsSessionCredentialsWrapper credentialsWrapper = mapper.readerFor(AwsSessionCredentialsWrapper.class).readValue(json);
		assertThat(credentialsWrapper.getAWSAccessKeyId(), equalTo("accessKey"));
		assertThat(credentialsWrapper.getAWSSecretKey(), equalTo("secretKey"));
		assertThat(credentialsWrapper.getSessionToken(), equalTo("sessionToken"));
	}
}
