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

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class AwsParameterStoreRepositoryTest {
	@Mock
	private AwsParameterStoreRepositoryProperties properties;

	@Mock
	private AWSSimpleSystemsManagement ssmClient;

	@Mock
	private GetParametersByPathRequest request;

	@InjectMocks
	@Spy
	private AwsParameterStoreRepository repository;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testDefaultProfile() {
		testEnvironment("app", "default", null);
	}

	@Test
	public void testCustomProfile() {
		testEnvironment("app", "profile", null);
	}

	@Test
	public void testCustomProfileSeparator() {
		testEnvironment("app", "profile1", "--");
	}

	@Test
	public void testCustomPathPrefix() {
		testEnvironment("app", "profile1", null, "/config/");
	}

	private String getPath(String appName, String profile, String profileSeparator) {
		return String.format("/%s%s%s/", appName, profileSeparator, profile);
	}

	private List<Parameter> generateParameters(String path) {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter().withName(path + "foo").withValue("bar"));
		return parameters;
	}

	private Map<String, String> getExpectedParameters(List<Parameter> parameters, String path) {
		Map<String, String> expectedParameters = new HashMap<>();
		for (Parameter parameter : parameters) {
			expectedParameters.put(parameter.getName().replace(path, "").replace("/", "."), parameter.getValue());
		}
		return expectedParameters;
	}

	private void testEnvironment(String appName, String profile, String customProfileSeparator) {
		testEnvironment(appName, profile, customProfileSeparator, null);
	}

	private void testEnvironment(String appName, String profile, String customProfileSeparator, String customPathPrefix) {
		String profileSeparator = !StringUtils.isEmpty(customProfileSeparator) ? customProfileSeparator : "/";
		if (!StringUtils.isEmpty(customProfileSeparator)) {
			when(properties.getProfileSeparator()).thenReturn(profileSeparator);
		}

		String path = getPath(appName, profile, profileSeparator);
		if (!StringUtils.isEmpty(customPathPrefix)) {
			when(properties.getPathPrefix()).thenReturn(customPathPrefix);
			path = customPathPrefix + path;
		}

		List<Parameter> awsParameters = generateParameters(path);
		Map<String, String> expectedParameters = getExpectedParameters(awsParameters, path);

		GetParametersByPathResult result = new GetParametersByPathResult();
		result.setParameters(awsParameters);
		when(ssmClient.getParametersByPath(any())).thenReturn(result);

		Environment environment = repository.findOne(appName, profile, null, false);
		assertEnvironment(environment, expectedParameters, path);
	}

	private void assertEnvironment(Environment environment, Map<String, String> expectedParameters, String path) {
		assertThat(environment.getPropertySources(), hasItem(
			allOf(
				hasProperty("name", endsWith(path)),
				hasProperty("source", equalTo(expectedParameters))
			)
		));
	}

}
