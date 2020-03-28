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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.util.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * @author Ross Shnaper
 */
@RunWith(MockitoJUnitRunner.class)
public class AwsParameterStoreRepositoryTest {
	@Mock
	private AwsParameterStoreRepositoryProperties properties;

	@Mock
	private AWSSimpleSystemsManagement ssmClient;


	@InjectMocks
	@Spy
	private AwsParameterStoreRepository repository;

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

	@Test
	public void testMultipleProfiles() {
		testEnvironment("app", "profile1,profile2", null, null);
	}

	@Test
	public void testNoProfile() {
		testEnvironment("app", "", null, null);
	}

	private String getPath(String appName, String profile, String profileSeparator, String pathPrefix) {
		return String.format("%s%s%s%s/", StringUtils.isEmpty(pathPrefix) ? "/" : pathPrefix, appName, profileSeparator, profile);
	}

	private List<Parameter> generateParameters(String path) {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Parameter().withName(path + UUID.randomUUID().toString()).withValue(UUID.randomUUID().toString()));
		parameters.add(new Parameter().withName(path + UUID.randomUUID().toString()).withValue(UUID.randomUUID().toString()));
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

	private void testEnvironment(String appName, String profiles, String customProfileSeparator, String customPathPrefix) {
		List<String> paths = new ArrayList<>();
		String profileSeparator = !StringUtils.isEmpty(customProfileSeparator) ? customProfileSeparator : "/";
		final Map<String, GetParametersByPathResult> results = new HashMap<>();
		Map<String, Map<String, String>> expectedParameters = new HashMap<>();

		//mock setups
		when(properties.getProfileSeparator()).thenReturn(profileSeparator);
		when(properties.getPathPrefix()).thenReturn(customPathPrefix);
		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).then(
			(Answer<GetParametersByPathResult>) invocationOnMock -> {
				GetParametersByPathRequest request = invocationOnMock.getArgument(0);

				return results.get(request.getPath());
			});

		//generate data for each profile
		for (String profile : profiles.split(",")) {
			String path = getPath(appName, profile, profileSeparator, customPathPrefix);
			paths.add(path);

			//generate data returned by AwsSSM
			List<Parameter> awsParameters = generateParameters(path);
			GetParametersByPathResult result = new GetParametersByPathResult();
			result.setParameters(awsParameters);
			results.put(path, result);

			//generate expected data from property sources
			expectedParameters.put(path, getExpectedParameters(awsParameters,
				path));
		}

		//test
		Environment environment = repository.findOne(appName, profiles, null, false);
		assertEnvironment(environment, expectedParameters, paths.toArray(new String[0]));
	}

	private void assertEnvironment(Environment environment, Map<String, Map<String, String>> expectedParameters, String[] paths) {
		assertThat(environment.getPropertySources().size(), equalTo(paths.length));
		for (String path : paths) {
			assertThat(environment.getPropertySources(), hasItem(
				allOf(
					hasProperty("name", endsWith(path)),
					hasProperty("source", equalTo(expectedParameters.get(path))))));
		}
	}

}
