package org.wso2.gluu.client.kmclient;

import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;
import org.wso2.gluu.client.model.ClientInfo;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface DCRClient {

	@RequestLine("POST")
	@Headers("Content-Type: application/json")
	public ClientInfo createApplication(ClientInfo clientInfo) throws KeyManagerClientException;

	@RequestLine("GET /?client_id={clientId}")
	@Headers("Authorization: Bearer {registrationAccessToken}")
	public ClientInfo getApplication(@Param("clientId") String clientId,
			@Param("registrationAccessToken") String registrationAccessToken) throws KeyManagerClientException;

	@RequestLine("PUT /?client_id={clientId}")
	@Headers("Authorization: Bearer {registrationAccessToken}")
	public ClientInfo updateApplication(@Param("clientId") String clientId,
			@Param("registrationAccessToken") String registrationAccessToken, ClientInfo clientInfo)
			throws KeyManagerClientException;

	@RequestLine("DELETE /?client_id={clientId}")
	@Headers("Authorization: Bearer {registrationAccessToken}")
	public void deleteApplication(@Param("clientId") String clientId,
			@Param("registrationAccessToken") String registrationAccessToken) throws KeyManagerClientException;

}
