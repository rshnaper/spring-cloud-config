/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.config.server.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.apache.http.client.HttpClient;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.composite.CompositeEnvironmentBeanFactoryPostProcessor;
import org.springframework.cloud.config.server.composite.ConditionalOnMissingSearchPathLocator;
import org.springframework.cloud.config.server.composite.ConditionalOnSearchPathLocator;
import org.springframework.cloud.config.server.environment.*;
import org.springframework.cloud.config.server.environment.aws.AwsParameterStoreEnvironmentRespositoryFactory;
import org.springframework.cloud.config.server.environment.aws.AwsParameterStoreRepository;
import org.springframework.cloud.config.server.environment.aws.AwsParameterStoreRepositoryCredentialsProvider;
import org.springframework.cloud.config.server.environment.aws.AwsParameterStoreRepositoryProperties;
import org.springframework.cloud.config.server.environment.vault.SpringVaultClientConfiguration;
import org.springframework.cloud.config.server.environment.vault.SpringVaultEnvironmentRepository;
import org.springframework.cloud.config.server.environment.vault.SpringVaultEnvironmentRepositoryFactory;
import org.springframework.cloud.config.server.support.GoogleCloudSourceSupport;
import org.springframework.cloud.config.server.support.TransportConfigCallbackFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.vault.core.VaultTemplate;
import org.tmatesoft.svn.core.SVNException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * @author Dave Syer
 * @author Ryan Baxter
 * @author Daniel Lavoie
 * @author Dylan Roberts
 * @author Alberto C. Ríos
 * @author Scott Frederick
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ SvnKitEnvironmentProperties.class,
		CredhubEnvironmentProperties.class, JdbcEnvironmentProperties.class,
		NativeEnvironmentProperties.class, VaultEnvironmentProperties.class,
		RedisEnvironmentProperties.class, AwsS3EnvironmentProperties.class, AwsParameterStoreRepositoryProperties.class })
@Import({ CompositeRepositoryConfiguration.class, JdbcRepositoryConfiguration.class,
		VaultConfiguration.class, VaultRepositoryConfiguration.class,
		SpringVaultRepositoryConfiguration.class, CredhubConfiguration.class,
		CredhubRepositoryConfiguration.class, SvnRepositoryConfiguration.class,
		NativeRepositoryConfiguration.class, GitRepositoryConfiguration.class,
		RedisRepositoryConfiguration.class, GoogleCloudSourceConfiguration.class,
		AwsS3RepositoryConfiguration.class, AwsParameterStoreRepositoryConfiguration.class,
		DefaultRepositoryConfiguration.class })
public class EnvironmentRepositoryConfiguration {

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.config.server.health.enabled",
			matchIfMissing = true)
	public ConfigServerHealthIndicator configServerHealthIndicator(
			EnvironmentRepository repository) {
		return new ConfigServerHealthIndicator(repository);
	}

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	public MultipleJGitEnvironmentProperties multipleJGitEnvironmentProperties() {
		return new MultipleJGitEnvironmentProperties();
	}

	@Bean
	@ConditionalOnMissingBean(ConfigTokenProvider.class)
	public ConfigTokenProvider defaultConfigTokenProvider(
			ObjectProvider<HttpServletRequest> httpRequest) {
		return new HttpRequestConfigTokenProvider(httpRequest);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty("spring.cloud.config.server.consul.watch.enabled")
	protected static class ConsulEnvironmentWatchConfiguration {

		@Bean
		public EnvironmentWatch environmentWatch() {
			return new ConsulEnvironmentWatch();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(EnvironmentWatch.class)
	protected static class DefaultEnvironmentWatch {

		@Bean
		public EnvironmentWatch environmentWatch() {
			return new EnvironmentWatch.Default();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(TransportConfigCallback.class)
	static class JGitFactoryConfig {

		@Bean
		public MultipleJGitEnvironmentRepositoryFactory gitEnvironmentRepositoryFactory(
				ConfigurableEnvironment environment, ConfigServerProperties server,
				Optional<ConfigurableHttpConnectionFactory> jgitHttpConnectionFactory,
				Optional<TransportConfigCallback> customTransportConfigCallback,
				Optional<GoogleCloudSourceSupport> googleCloudSourceSupport) {
			final TransportConfigCallbackFactory transportConfigCallbackFactory = new TransportConfigCallbackFactory(
					customTransportConfigCallback.orElse(null),
					googleCloudSourceSupport.orElse(null));
			return new MultipleJGitEnvironmentRepositoryFactory(environment, server,
					jgitHttpConnectionFactory, transportConfigCallbackFactory);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ HttpClient.class, TransportConfigCallback.class })
	static class JGitHttpClientConfig {

		@Bean
		public ConfigurableHttpConnectionFactory httpClientConnectionFactory() {
			return new HttpClientConfigurableHttpConnectionFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(AmazonS3.class)
	static class AwsS3FactoryConfig {

		@Bean
		public AwsS3EnvironmentRepositoryFactory awsS3EnvironmentRepositoryFactory(
				ConfigServerProperties server) {
			return new AwsS3EnvironmentRepositoryFactory(server);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SVNException.class)
	static class SvnFactoryConfig {

		@Bean
		public SvnEnvironmentRepositoryFactory svnEnvironmentRepositoryFactory(
				ConfigurableEnvironment environment, ConfigServerProperties server) {
			return new SvnEnvironmentRepositoryFactory(environment, server);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.vault.core.VaultTemplate")
	@SuppressWarnings("deprecation")
	static class VaultFactoryConfig {

		@Bean
		public VaultEnvironmentRepositoryFactory vaultEnvironmentRepositoryFactory(
				ObjectProvider<HttpServletRequest> request, EnvironmentWatch watch,
				Optional<VaultEnvironmentRepositoryFactory.VaultRestTemplateFactory> vaultRestTemplateFactory,
				ConfigTokenProvider tokenProvider) {
			return new VaultEnvironmentRepositoryFactory(request, watch,
					vaultRestTemplateFactory, tokenProvider);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HttpClient.class)
	@ConditionalOnMissingClass("org.springframework.vault.core.VaultTemplate")
	@SuppressWarnings("deprecation")
	static class VaultHttpClientConfig {

		@Bean
		public VaultEnvironmentRepositoryFactory.VaultRestTemplateFactory vaultRestTemplateFactory() {
			return new HttpClientVaultRestTemplateFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(VaultTemplate.class)
	@Import(SpringVaultClientConfiguration.class)
	static class SpringVaultFactoryConfig {

		@Bean
		public SpringVaultEnvironmentRepositoryFactory vaultEnvironmentRepositoryFactory(
				ObjectProvider<HttpServletRequest> request, EnvironmentWatch watch,
				SpringVaultClientConfiguration vaultClientConfiguration) {
			return new SpringVaultEnvironmentRepositoryFactory(request, watch,
					vaultClientConfiguration);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcTemplate.class)
	static class JdbcFactoryConfig {

		@Bean
		@ConditionalOnBean(JdbcTemplate.class)
		public JdbcEnvironmentRepositoryFactory jdbcEnvironmentRepositoryFactory(
				JdbcTemplate jdbc) {
			return new JdbcEnvironmentRepositoryFactory(jdbc);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(StringRedisTemplate.class)
	static class RedisFactoryConfig {

		@Bean
		@ConditionalOnBean(StringRedisTemplate.class)
		public RedisEnvironmentRepositoryFactory redisEnvironmentRepositoryFactory(
				StringRedisTemplate redis) {
			return new RedisEnvironmentRepositoryFactory(redis);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(CredHubOperations.class)
	static class CredhubFactoryConfig {

		@Bean
		public CredhubEnvironmentRepositoryFactory credhubEnvironmentRepositoryFactory(
				Optional<CredHubOperations> credHubOperations) {
			return new CredhubEnvironmentRepositoryFactory(
					credHubOperations.orElse(null));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NativeFactoryConfig {

		@Bean
		public NativeEnvironmentRepositoryFactory nativeEnvironmentRepositoryFactory(
				ConfigurableEnvironment environment, ConfigServerProperties properties) {
			return new NativeEnvironmentRepositoryFactory(environment, properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AwsParameterStoreFactoryConfig {
		@Bean
		public AwsParameterStoreRepositoryCredentialsProvider credentialsProvider(AwsParameterStoreRepositoryProperties properties,
			ConfigTokenProvider tokenProvider) {
			return new AwsParameterStoreRepositoryCredentialsProvider(properties, tokenProvider);
		}

		@Bean
		public AwsParameterStoreEnvironmentRespositoryFactory awsParameterStoreEnvironmentRespositoryFactory(
			AwsParameterStoreRepositoryCredentialsProvider credentialsProvider) {
			return new AwsParameterStoreEnvironmentRespositoryFactory(credentialsProvider);
		}
	}

}

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(value = EnvironmentRepository.class,
		search = SearchStrategy.CURRENT)
class DefaultRepositoryConfiguration {

	@Bean
	public MultipleJGitEnvironmentRepository defaultEnvironmentRepository(
			MultipleJGitEnvironmentRepositoryFactory gitEnvironmentRepositoryFactory,
			MultipleJGitEnvironmentProperties environmentProperties) throws Exception {
		return gitEnvironmentRepositoryFactory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@Profile("native")
class NativeRepositoryConfiguration {

	@Bean
	public NativeEnvironmentRepository nativeEnvironmentRepository(
			NativeEnvironmentRepositoryFactory factory,
			NativeEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@Profile("git")
class GitRepositoryConfiguration extends DefaultRepositoryConfiguration {

}

@Configuration(proxyBeanMethods = false)
@Profile("awss3")
class AwsS3RepositoryConfiguration {

	@Bean
	@ConditionalOnMissingBean(AwsS3EnvironmentRepository.class)
	public AwsS3EnvironmentRepository awsS3EnvironmentRepository(
			AwsS3EnvironmentRepositoryFactory factory,
			AwsS3EnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@Profile("subversion")
class SvnRepositoryConfiguration {

	@Bean
	public SvnKitEnvironmentRepository svnKitEnvironmentRepository(
			SvnEnvironmentRepositoryFactory factory,
			SvnKitEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingClass("org.springframework.vault.core.VaultTemplate")
@Profile("vault")
@SuppressWarnings("deprecation")
class VaultRepositoryConfiguration {

	@Bean
	public VaultEnvironmentRepository vaultEnvironmentRepository(
			VaultEnvironmentRepositoryFactory factory,
			VaultEnvironmentProperties environmentProperties) throws Exception {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(VaultTemplate.class)
@Profile("vault")
class SpringVaultRepositoryConfiguration {

	@Bean
	public SpringVaultEnvironmentRepository vaultEnvironmentRepository(
			SpringVaultEnvironmentRepositoryFactory factory,
			VaultEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@Profile("credhub")
class CredhubRepositoryConfiguration {

	@Bean
	public CredhubEnvironmentRepository credhubEnvironmentRepository(
			CredhubEnvironmentRepositoryFactory factory,
			CredhubEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@Profile("jdbc")
@ConditionalOnClass(JdbcTemplate.class)
class JdbcRepositoryConfiguration {

	@Bean
	@ConditionalOnBean(JdbcTemplate.class)
	public JdbcEnvironmentRepository jdbcEnvironmentRepository(
			JdbcEnvironmentRepositoryFactory factory,
			JdbcEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@Profile("redis")
@ConditionalOnClass(StringRedisTemplate.class)
class RedisRepositoryConfiguration {

	@Bean
	@ConditionalOnBean(StringRedisTemplate.class)
	public RedisEnvironmentRepository redisEnvironmentRepository(
			RedisEnvironmentRepositoryFactory factory,
			RedisEnvironmentProperties environmentProperties) {
		return factory.build(environmentProperties);
	}

}

@Configuration(proxyBeanMethods = false)
@Profile("composite")
class CompositeRepositoryConfiguration {

	@Bean
	public static CompositeEnvironmentBeanFactoryPostProcessor compositeEnvironmentRepositoryBeanFactoryPostProcessor(
			Environment environment) {
		return new CompositeEnvironmentBeanFactoryPostProcessor(environment);
	}

	@Primary
	@Bean
	@ConditionalOnSearchPathLocator
	public SearchPathCompositeEnvironmentRepository searchPathCompositeEnvironmentRepository(
			List<EnvironmentRepository> environmentRepositories) {
		return new SearchPathCompositeEnvironmentRepository(environmentRepositories);
	}

	@Primary
	@Bean
	@ConditionalOnMissingSearchPathLocator
	public CompositeEnvironmentRepository compositeEnvironmentRepository(
			List<EnvironmentRepository> environmentRepositories) {
		return new CompositeEnvironmentRepository(environmentRepositories);
	}

}

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(AWSSimpleSystemsManagement.class)
@Profile("awsparameterstore")
class AwsParameterStoreRepositoryConfiguration {

	@Bean
	public AwsParameterStoreRepository awsParameterStoreEnvironmentRepository(
		AwsParameterStoreEnvironmentRespositoryFactory factory,
		AwsParameterStoreRepositoryProperties environmentProperties) {
		return factory.build(environmentProperties);
	}
}
