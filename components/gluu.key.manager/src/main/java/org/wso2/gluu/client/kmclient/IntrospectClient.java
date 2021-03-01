package org.wso2.gluu.client.kmclient;

import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;
import org.wso2.gluu.client.model.IntrospectInfo;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface IntrospectClient {

    @RequestLine("POST")
    @Headers("Content-type: application/x-www-form-urlencoded")
    public IntrospectInfo introspect(@Param("token") String token) throws KeyManagerClientException;

}
