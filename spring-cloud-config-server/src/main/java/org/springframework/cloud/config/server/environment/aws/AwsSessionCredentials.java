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

import java.io.IOException;

import com.amazonaws.auth.BasicSessionCredentials;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.springframework.boot.jackson.JsonComponent;



@JsonComponent
@JsonSerialize(using = AwsSessionCredentials.Serializer.class)
@JsonDeserialize(using = AwsSessionCredentials.Deserializer.class)
public class AwsSessionCredentials extends BasicSessionCredentials {

	public AwsSessionCredentials(BasicSessionCredentials delegate) {
		super(delegate.getAWSAccessKeyId(), delegate.getAWSSecretKey(), delegate.getSessionToken());
	}

	static class Deserializer extends JsonDeserializer<AwsSessionCredentials> {

		@Override
		public AwsSessionCredentials deserialize(JsonParser jsonParser,
			DeserializationContext deserializationContext)
			throws IOException, JsonProcessingException {

			JsonNode json = jsonParser.getCodec().readTree(jsonParser);
			String accessKeyId = json.get("AccessKeyId").asText();
			String secretKey = json.get("SecretAccessKey").asText();
			String sessionToken = json.get("SessionToken").asText();

			return new AwsSessionCredentials(new BasicSessionCredentials(accessKeyId, secretKey, sessionToken));
		}
	}

	static class Serializer extends JsonSerializer<AwsSessionCredentials> {

		@Override
		public void serialize(AwsSessionCredentials awsSessionCredentials,
			JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
			throws IOException {
			jsonGenerator.writeStartObject();
			jsonGenerator.writeStringField("AccessKeyId", awsSessionCredentials.getAWSAccessKeyId());
			jsonGenerator.writeStringField("SecretAccessKey", awsSessionCredentials.getAWSSecretKey());
			jsonGenerator.writeStringField("SessionToken", awsSessionCredentials.getSessionToken());
			jsonGenerator.writeEndObject();
		}
	}
}
