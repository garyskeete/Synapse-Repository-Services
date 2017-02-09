package org.sagebionetworks.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.client.exceptions.SynapseConflictingUpdateException;
import org.sagebionetworks.client.exceptions.SynapseDeprecatedServiceException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseLockedException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.simpleHttpClient.Header;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.util.ValidateArgument;

public class ClientUtils {

	public static final String ERROR_REASON_TAG = "reason";

	/**
	 * Checks to see if the statusCode is in [200,300) range.
	 * 
	 * @param statusCode
	 * @return true if the statusCode is in range [200, 300)
	 */
	public static boolean is200sStatusCode(int statusCode) {
		return statusCode>=200 && statusCode<300;
	}

	/**
	 * Checks to see if the statusCode is in [200,300) range. If it is not,
	 * throws an exception.
	 * 
	 * This method is used to check the SimpleHttpResponse that has expected
	 * content in non JSON format.
	 * 
	 * @param response
	 * @throws SynapseException 
	 */
	public static void checkStatusCodeAndThrowException(SimpleHttpResponse response) throws SynapseException {
		ValidateArgument.required(response, "response");
		if (is200sStatusCode(response.getStatusCode())) {
			return;
		}
		try {
			JSONObject reason = new JSONObject();
			reason.append(ERROR_REASON_TAG, response.getContent());
			throwException(response.getStatusCode(), reason);
		} catch (JSONException e) {
			throw new SynapseClientException(e);
		}
	}

	/**
	 * Try to convert response body to a JSON object. Then check for status code.
	 * If statusCode is in [200,300) range, returns the JSON object. Otherwise,
	 * throw an exception using the converted JSON object.
	 * 
	 * This method is used to check the SimpleHttpResponse that has expected
	 * content in JSON format.
	 * 
	 * @param responseBody
	 * @param statusCode
	 * @return
	 * @throws SynapseException
	 */
	public static JSONObject convertResponseBodyToJSONAndThrowException(SimpleHttpResponse response) throws SynapseException {
		ValidateArgument.required(response, "response");
		JSONObject json = convertStringToJSONObject(response.getContent());
		if (!is200sStatusCode(response.getStatusCode())) {
			throwException(response.getStatusCode(), json);
		}
		return json;
	}

	/**
	 * Converts a String to a JSON object.
	 * 
	 * @param toConvert
	 * @return
	 * @throws SynapseClientException
	 */
	public static JSONObject convertStringToJSONObject(String toConvert) throws SynapseClientException {
		JSONObject json = null;
		if (null != toConvert && toConvert.length()>0) {
			try {
				json = new JSONObject(toConvert);
			} catch (JSONException e) {
				throw new SynapseClientException("responseBody: <<"+toConvert+">>", e);
			}
		}
		return json;
	}

	/**
	 * 
	 * @param statusCode
	 * @param responseBody
	 * @throws SynapseException
	 */
	public static void throwException(int statusCode, JSONObject responseBody) throws SynapseException {
		ValidateArgument.requirement(!is200sStatusCode(statusCode), "Only support non 200s statusCode.");
		String reasonStr = null;
		if (responseBody!=null) {
			try {
				reasonStr = responseBody.getString(ERROR_REASON_TAG);
			} catch (JSONException e) {
				throw new SynapseClientException(e);
			}
		}
		if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
			throw new SynapseUnauthorizedException(reasonStr);
		} else if (statusCode == HttpStatus.SC_FORBIDDEN) {
			throw new SynapseForbiddenException(reasonStr);
		} else if (statusCode == HttpStatus.SC_NOT_FOUND) {
			throw new SynapseNotFoundException(reasonStr);
		} else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
			throw new SynapseBadRequestException(reasonStr);
		} else if (statusCode == HttpStatus.SC_LOCKED) {
			throw new SynapseLockedException(reasonStr);
		} else if (statusCode == HttpStatus.SC_PRECONDITION_FAILED) {
			throw new SynapseConflictingUpdateException(reasonStr);
		} else if (statusCode == HttpStatus.SC_GONE) {
			throw new SynapseDeprecatedServiceException(reasonStr);
		} else if (statusCode == SynapseTooManyRequestsException.TOO_MANY_REQUESTS_STATUS_CODE){
			throw new SynapseTooManyRequestsException(reasonStr);
		}else {
			throw new SynapseServerException(statusCode, reasonStr);
		}
	}

	/**
	 * 
	 * @param response
	 * @return
	 */
	public static Charset getCharacterSetFromResponse(SimpleHttpResponse response) {
		ValidateArgument.required(response, "response");
		Header contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
		ContentType contentType = ContentType.parse(contentTypeHeader.getValue());
		return contentType.getCharset();
	}

	/**
	 * @param endpoint
	 * @param uri
	 * @param parameters
	 * @return
	 * @throws SynapseClientException
	 */
	public static String createRequestUrl(String endpoint, String uri, Map<String,String> parameters) throws SynapseClientException {
		ValidateArgument.required(endpoint, "endpoint");
		ValidateArgument.required(uri, "uri");

		URL requestUrl = null;
		URIBuilder builder = new URIBuilder();
		try {
			URL parsedEndpoint = new URL(endpoint);
			String endpointPrefix = parsedEndpoint.getPath();
			String endpointLocation = endpoint.substring(0, endpoint.length() - endpointPrefix.length());
			if (uri.startsWith(endpointPrefix)) {
				requestUrl = new URL(endpointLocation + uri);
			} else {
				requestUrl = new URL(endpoint + uri);
			}
			builder = new URIBuilder(requestUrl.toURI());
			if (parameters != null) {
				for (Map.Entry<String,String> entry : parameters.entrySet()) {
					builder.addParameter(entry.getKey(), entry.getValue());
				}
			}
		} catch(MalformedURLException mue) {
			throw new SynapseClientException("Invalid URI.", mue);
		} catch(URISyntaxException use) {
			throw new SynapseClientException("Invalid URI.", use);
		}
		return builder.toString();
	}

	/**
	 * 
	 * 
	 * @param simpleHttpClient
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public static SimpleHttpResponse performRequest(SimpleHttpClient simpleHttpClient,
			String requestUrl, Method requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws ClientProtocolException, IOException {
		ValidateArgument.required(simpleHttpClient, "simpleHttpClient");
		ValidateArgument.required(requestUrl, "requestUrl");
		ValidateArgument.required(requestMethod, "requestMethod");

		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(requestUrl);
		request.setHeaders(requestHeaders);
		switch (requestMethod) {
			case GET:
				return simpleHttpClient.get(request);
			case POST:
				return simpleHttpClient.post(request, requestContent);
			case PUT:
				return simpleHttpClient.put(request, requestContent);
			case DELETE:
				return simpleHttpClient.delete(request);
			default: 
				throw new IllegalArgumentException("Unsupported method: "+requestMethod);
		}
	}
}