package org.wso2.gluu.client.kmclient;

import java.util.Map;

import org.wso2.gluu.client.model.ClientInfo;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;

public interface DCRClient {
    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    public ClientInfo createApplication(ClientInfo clientInfo);

    @RequestLine("GET /?client_id={clientId}")
    @Headers("Authorization: Bearer {registrationAccessToken}")
    public ClientInfo getApplication(@Param("clientId") String clientId,
            @Param("registrationAccessToken") String registrationAccessToken);

    @RequestLine("PUT")
    @Headers("Content-Type: application/json")
    public ClientInfo updateApplication(@HeaderMap Map<String, Object> headerMap,
            @QueryMap Map<String, Object> queryMap, ClientInfo clientInfo);

    @RequestLine("DELETE /?client_id={clientId}")
    @Headers("Authorization: Bearer (registrationAccessToken}")
    public void deleteApplication(@Param("clientId") String clientId,
            @Param("registrationAccessToken") String registrationAccessToken);
}
