package org.wso2.gluu.client.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ClientInfo {

    @SerializedName("client_id")
    private String clientId;
    @SerializedName("client_secret")
    private String clientSecret;
    @SerializedName("registration_access_token")
    private String registrationAccessToken;
    @SerializedName("application_type")
    private String applicationType;
    @SerializedName("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;
    @SerializedName("client_id_issued_at")
    private Long clientIdIssuedTime;
    @SerializedName("client_name")
    private String clientName;
    @SerializedName("grant_types")
    private List<String> grantTypes = new ArrayList<>();
    @SerializedName("redirect_uris")
    private List<String> redirectUris = new ArrayList<>();
    @SerializedName("client_secret_expires_at")
    private Long clientSecretExpiresAt;
    @SerializedName("subject_type")
    private String subjectType;
    @SerializedName("response_types")
    private List<String> responseTypes;
    @SerializedName("access_token_as_jwt")
    private boolean accessTokenAsJWT;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public Long getClientIdIssuedTime() {
        return clientIdIssuedTime;
    }

    public void setClientIdIssuedTime(Long clientIdIssuedTime) {
        this.clientIdIssuedTime = clientIdIssuedTime;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Long getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(Long clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public boolean isAccessTokenAsJWT() {
        return accessTokenAsJWT;
    }

    public void setAccessTokenAsJWT(boolean accessTokenAsJWT) {
        this.accessTokenAsJWT = accessTokenAsJWT;
    }

    @Override
    public String toString() {
        return "ClientInfo [applicationType=" + applicationType + ", clientId=" + clientId + ", clientName="
                + clientName + ", grantTypes=" + grantTypes + ", redirectUris=" + redirectUris + ", responseTypes="
                + responseTypes + ", subjectType=" + subjectType + ", tokenEndpointAuthMethod="
                + tokenEndpointAuthMethod + "]";
    }

}
