package org.wso2.gluu.client.client;

import org.wso2.gluu.client.model.ClientInfo;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface GluuDCRClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    public ClientInfo createApplication(ClientInfo clientInfo);

    @RequestLine("GET /?client_id={clientId}")
    @Headers("Authorization: Bearer {registrationAccessToken}")
    public ClientInfo getApplication(@Param("clientId") String clientId,
            @Param("registrationAccessToken") String registrationAccessToken);

    @RequestLine("PUT /?client_id={clientId}")
    @Headers({ "Content-Type: application/json", "Authorization: Bearer {registrationAccessToken" })
    public ClientInfo updateApplication(ClientInfo clientInfo, @Param("clientId") String clientId,
            @Param("registrationAccessToken") String registrationAccessToken);

    @RequestLine("DELETE /?client_id={clientId}")
    @Headers("Authorization: Bearer (registrationAccessToken}")
    public void deleteApplication(@Param("clientId") String clientId,
            @Param("registrationAccessToken") String registrationAccessToken);
}
