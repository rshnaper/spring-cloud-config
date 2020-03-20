package org.springframework.cloud.config.server.environment.aws;

import com.amazonaws.auth.BasicSessionCredentials;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;



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
