package org.wso2.gluu.client.model;

import com.google.gson.annotations.SerializedName;

public class IntrospectInfo {

    @SerializedName("exp")
    private long expiryTime;
    @SerializedName("iat")
    private long issuedAt;
    @SerializedName("jti")
    private String jti;
    @SerializedName("iss")
    private String issuer;
    @SerializedName("aud")
    private String audience;
    @SerializedName("sub")
    private String subject;
    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("acr")
    private String acr;
    @SerializedName("scope")
    private String scope;
    @SerializedName("client_id")
    private String clientId;
    @SerializedName("username")
    private String username;
    @SerializedName("active")
    private boolean active;

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAcr() {
        return acr;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}
