package org.wso2.gluu.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIKey;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.AccessTokenRequest;
import org.wso2.carbon.apimgt.api.model.ApplicationConstants;
import org.wso2.carbon.apimgt.api.model.KeyManagerConfiguration;
import org.wso2.carbon.apimgt.api.model.OAuthAppRequest;
import org.wso2.carbon.apimgt.api.model.OAuthApplicationInfo;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.AbstractKeyManager;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.gluu.client.client.GluuDCRClient;
import org.wso2.gluu.client.client.IntrospectionClient;
import org.wso2.gluu.client.model.AccessTokenResponse;
import org.wso2.gluu.client.model.ClientInfo;
import org.wso2.gluu.client.model.IntrospectInfo;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

/**
 * Key manager implementation to integrate Gluu server with WSO2 API Manager
 */
public class GluuOAuthClient extends AbstractKeyManager {

    private GluuDCRClient gluuDCRClient;
    private IntrospectionClient introspectionClient;

    private static final Log log = LogFactory.getLog(GluuOAuthClient.class);

    @Override
    public OAuthApplicationInfo createApplication(OAuthAppRequest oauthAppRequest) throws APIManagementException {
        OAuthApplicationInfo oauthApplicationInfo = oauthAppRequest.getOAuthApplicationInfo();
        if (oauthApplicationInfo != null) {
            ClientInfo clientInfo = createClientInfoFromOAuthApplicationInfo(oauthApplicationInfo);
            ClientInfo createdApplication = gluuDCRClient.createApplication(clientInfo);
            if (createdApplication != null) {
                return createOAuthAppInfoFromResponse(createdApplication);
            }
        }
        return null;
    }

    /**
     * method to create a {@link ClientInfo} object using an object of
     * {@code OAuthApplicationInfo} to create and update the client
     * 
     * @param oauthApplicationInfo object which needs to be converted
     * @return {@link ClientInfo} object
     */
    @SuppressWarnings("unchecked")
    private ClientInfo createClientInfoFromOAuthApplicationInfo(OAuthApplicationInfo oauthApplicationInfo) {
        ClientInfo clientInfo = new ClientInfo();
        String userId = (String) oauthApplicationInfo.getParameter(ApplicationConstants.OAUTH_CLIENT_USERNAME);
        String userNameForSp = MultitenantUtils.getTenantAwareUsername(userId);
        String domain = UserCoreUtil.extractDomainFromName(userNameForSp);

        if (!domain.isEmpty() && !UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME.equals(domain)) {
            userNameForSp = userNameForSp.replace(UserCoreConstants.DOMAIN_SEPARATOR, "_");
        }

        String applicationName = oauthApplicationInfo.getClientName();
        String keyType = (String) oauthApplicationInfo.getParameter(ApplicationConstants.APP_KEY_TYPE);
        String callBackUrl = oauthApplicationInfo.getCallBackURL();

        if (keyType != null) {
            applicationName = userNameForSp.concat(applicationName).concat("_").concat(keyType);
        }

        List<String> grantTypes = new ArrayList<>();
        if (oauthApplicationInfo.getParameter(APIConstants.JSON_GRANT_TYPES) != null) {
            grantTypes = Arrays
                    .asList(((String) oauthApplicationInfo.getParameter(APIConstants.JSON_GRANT_TYPES)).split(","));
        }

        clientInfo.setClientName(applicationName);
        if (grantTypes != null && !grantTypes.isEmpty()) {
            clientInfo.setGrantTypes(grantTypes);
        }
        if (StringUtils.isNotEmpty(callBackUrl)) {
            String[] redirectUris = callBackUrl.split(",");
            clientInfo.setRedirectUris(Arrays.asList(redirectUris));
        }

        Object parameter = oauthApplicationInfo.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
        Map<String, Object> additionalProperties = new HashMap<>();
        if (parameter instanceof String) {
            additionalProperties = new Gson().fromJson((String) parameter, Map.class);
        }

        if (additionalProperties.containsKey(GluuConstants.APP_TYPE)) {
            clientInfo.setApplicationType((String) additionalProperties.get(GluuConstants.APP_TYPE));
        } else {
            clientInfo.setApplicationType(GluuConstants.DEFAULT_CLIENT_APPLICATION_TYPE);
        }

        if (additionalProperties.containsKey(GluuConstants.TOKEN_ENDPOINT_AUTH_METHOD)) {
            clientInfo.setTokenEndpointAuthMethod(
                    (String) additionalProperties.get(GluuConstants.TOKEN_ENDPOINT_AUTH_METHOD));
        }

        return clientInfo;
    }

    /**
     * method to create {@code OAuthApplicationInfo} object from {@link ClientInfo}
     * 
     * @param application response received from server as {@link ClientInfo}
     * @return {@code OAuthApplicationInfo} object
     */
    private OAuthApplicationInfo createOAuthAppInfoFromResponse(ClientInfo application) {
        OAuthApplicationInfo appInfo = new OAuthApplicationInfo();
        appInfo.setClientName(application.getClientName());
        appInfo.setClientId(application.getClientId());
        appInfo.setClientSecret(application.getClientSecret());

        if (application.getRedirectUris() != null) {
            appInfo.setCallBackURL(String.join(",", application.getRedirectUris()));
        }
        if (application.getGrantTypes() != null) {
            appInfo.addParameter(GluuConstants.GRANT_TYPES, String.join(" ", application.getGrantTypes()));
        }
        if (StringUtils.isNotEmpty(application.getClientName())) {
            appInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_NAME, application.getClientName());
        }
        if (StringUtils.isNotEmpty(application.getClientId())) {
            appInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_ID, application.getClientId());
        }
        if (StringUtils.isNotEmpty(application.getClientSecret())) {
            appInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_SECRET, application.getClientSecret());
        }

        String additionalProperties = new Gson().toJson(application);
        appInfo.addParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES,
                new Gson().fromJson(additionalProperties, Map.class));

        return appInfo;
    }

    @Override
    public OAuthApplicationInfo updateApplication(OAuthAppRequest oauthAppRequest) throws APIManagementException {
        OAuthApplicationInfo oauthApplicationInfo = oauthAppRequest.getOAuthApplicationInfo();
        if (oauthApplicationInfo != null) {
            String clientId = oauthApplicationInfo.getClientId();
            String registrationAccessToken = getRegistrationAccessToken(clientId);
            if (registrationAccessToken != null) {
                ClientInfo clientInfo = createClientInfoFromOAuthApplicationInfo(oauthApplicationInfo);
                ClientInfo updatedApplication = gluuDCRClient.updateApplication(clientInfo, clientId,
                        registrationAccessToken);
                if (updatedApplication != null) {
                    return createOAuthAppInfoFromResponse(updatedApplication);
                }
            }
        }
        return null;
    }

    /**
     * method to get registration_access_token of a client application
     * 
     * @param consumerKey client-id of the client application
     * @return {@code String} Registration access token
     * @throws APIManagementException
     */
    @SuppressWarnings("unchecked")
    private String getRegistrationAccessToken(String consumerKey) throws APIManagementException {
        if (StringUtils.isNotEmpty(consumerKey)) {
            ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
            int applicationId = apiMgtDAO.getApplicationByClientId(consumerKey).getId();
            Set<APIKey> apiKeys = apiMgtDAO.getKeyMappingsFromApplicationId(applicationId);
            for (APIKey apiKey : apiKeys) {
                if (consumerKey.equals(apiKey.getConsumerKey()) && StringUtils.isNotEmpty(apiKey.getAppMetaData())) {
                    OAuthApplicationInfo storedOAuthApplicationInfo = new Gson().fromJson(apiKey.getAppMetaData(),
                            OAuthApplicationInfo.class);
                    return (String) ((Map<String, Object>) storedOAuthApplicationInfo
                            .getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES))
                                    .get(GluuConstants.REGISTRATION_ACCESS_TOKEN);
                }
            }
        }
        return null;
    }

    @Override
    public void deleteApplication(String consumerKey) throws APIManagementException {
        String registrationAccessToken = getRegistrationAccessToken(consumerKey);
        if (registrationAccessToken != null) {
            gluuDCRClient.deleteApplication(consumerKey, registrationAccessToken);
        }
    }

    @Override
    public OAuthApplicationInfo retrieveApplication(String consumerKey) throws APIManagementException {
        String registrationAccessToken = getRegistrationAccessToken(consumerKey);
        if (registrationAccessToken != null) {
            ClientInfo clientInfo = gluuDCRClient.getApplication(consumerKey, registrationAccessToken);
            return createOAuthAppInfoFromResponse(clientInfo);
        }
        return null;
    }

    @Override
    public AccessTokenInfo getNewApplicationAccessToken(AccessTokenRequest tokenRequest) throws APIManagementException {
        String clientId = tokenRequest.getClientId();
        String clientSecret = tokenRequest.getClientSecret();
        Object grantType = tokenRequest.getGrantType();
        if (grantType == null) {
            grantType = GluuConstants.GRANT_TYPE_CLIENT_CREDENTIALS;
        }

        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair(GluuConstants.GRANT_TYPE, (String) grantType));

        String scopes = "";
        if (tokenRequest.getScope() != null && (tokenRequest.getScope().length > 0)) {
            scopes = String.join(" ", tokenRequest.getScope());
        }
        if (StringUtils.isNotEmpty(scopes)) {
            parameters.add(new BasicNameValuePair(GluuConstants.SCOPE, scopes));
        }

        AccessTokenResponse retrievedAccessTokenInfo = getAccessToken(clientId, clientSecret, parameters);
        if (retrievedAccessTokenInfo != null) {
            AccessTokenInfo accessTokenInfo = new AccessTokenInfo();
            accessTokenInfo.setConsumerKey(clientId);
            accessTokenInfo.setConsumerSecret(clientSecret);
            accessTokenInfo.setAccessToken(retrievedAccessTokenInfo.getAccessToken());
            if (retrievedAccessTokenInfo.getScope() != null) {
                accessTokenInfo.setScope(retrievedAccessTokenInfo.getScope().split("\\s+"));
            }
            accessTokenInfo.setValidityPeriod(retrievedAccessTokenInfo.getExpiry());
            return accessTokenInfo;
        }

        return null;
    }

    /**
     * method to get an access token from Gluu server
     * 
     * @param clientId     consumer key of the client
     * @param clientSecret consumer secret of the client
     * @param parameters   list containing {@code NameValuePairs} presenting the
     *                     parameters
     * @return {@code AccessTokenResponse}
     * @throws APIManagementException
     */
    private AccessTokenResponse getAccessToken(String clientId, String clientSecret, List<NameValuePair> parameters)
            throws APIManagementException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String tokenEndpoint = (String) this.configuration.getParameter(APIConstants.KeyManager.TOKEN_ENDPOINT);
            HttpPost httpPost = new HttpPost(tokenEndpoint);
            httpPost.setEntity(new UrlEncodedFormEntity(parameters));

            String encodedCredentials = getEncodedCredentials(clientId, clientSecret);
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, GluuConstants.BASIC + encodedCredentials);
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, GluuConstants.CONTENT_TYPE_URL_ENCODED);

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new APIManagementException(GluuConstants.ERROR_COULD_NOT_READ_HTTP_ENTITY + response);
            }

            if (statusCode == HttpStatus.SC_OK) {
                try (InputStream inputStream = entity.getContent()) {
                    String content = IOUtils.toString(inputStream);
                    return new Gson().fromJson(content, AccessTokenResponse.class);
                }
            }
        } catch (UnsupportedEncodingException e) {
            handleException(GluuConstants.ERROR_UNSUPPORTED_ENCODING_METHOD, e);
        } catch (IOException e) {
            handleException(GluuConstants.ERROR_ERROR_OCCURED_WHILE_READING, e);
        }

        return null;
    }

    /**
     * method to base64 encode credentials (basic auth)
     * 
     * @param clientId     consumer-key
     * @param clientSecret consumer-secret
     * @return {@code String} encoded string
     */
    private static String getEncodedCredentials(String clientId, String clientSecret) {
        return Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getNewApplicationConsumerSecret(AccessTokenRequest tokenRequest) throws APIManagementException {
        return null;
    }

    @Override
    public AccessTokenInfo getTokenMetaData(String accessToken) throws APIManagementException {
        AccessTokenInfo accessTokenInfo = new AccessTokenInfo();
        IntrospectInfo introspectInfo = introspectionClient.introspect(accessToken);
        if (introspectInfo != null) {
            accessTokenInfo.setAccessToken(accessToken);
            accessTokenInfo.setTokenValid(introspectInfo.isActive());
            accessTokenInfo.setIssuedTime(introspectInfo.getIssuedAt());
            accessTokenInfo.setValidityPeriod(introspectInfo.getExpiryTime() - introspectInfo.getIssuedAt());
            accessTokenInfo.setEndUserName(introspectInfo.getUsername());
            accessTokenInfo.setConsumerKey(introspectInfo.getClientId());

            if (StringUtils.isNotEmpty(introspectInfo.getScope())) {
                accessTokenInfo.setScope(introspectInfo.getScope().split("\\s+"));
            }

            accessTokenInfo.addParameter(GluuConstants.ACCESS_TOKEN_ISSUER, introspectInfo.getIssuer());
            accessTokenInfo.addParameter(GluuConstants.ACCESS_TOKEN_IDENTIFIER, introspectInfo.getJti());
        }
        return accessTokenInfo;
    }

    @Override
    public KeyManagerConfiguration getKeyManagerConfiguration() throws APIManagementException {
        return this.configuration;
    }

    @Override
    public OAuthApplicationInfo mapOAuthApplication(OAuthAppRequest appInfoRequest) throws APIManagementException {
        return null;
    }

    @Override
    public void loadConfiguration(KeyManagerConfiguration keyManagerConfiguration) throws APIManagementException {
        this.configuration = keyManagerConfiguration;

        String clientRegistrationEndpoint = (String) this.configuration
                .getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT);
        String introspectionEndpoint = (String) this.configuration
                .getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT);

        // FIXME: OkHttpClient with SSL
        try {
            OkHttpUtil.init(true);
        } catch (Exception e) {
            log.error(e, e);
        }

        gluuDCRClient = Feign.builder()
                // .client(new OkHttpClient())
                .client(new OkHttpClient(OkHttpUtil.getClient())).encoder(new GsonEncoder()).decoder(new GsonDecoder())
                .logger(new Slf4jLogger()).target(GluuDCRClient.class, clientRegistrationEndpoint);

        introspectionClient = Feign.builder()
                // .client(new OkHttpClient())
                .client(new OkHttpClient(OkHttpUtil.getClient())).encoder(new GsonEncoder()).decoder(new GsonDecoder())
                .logger(new Slf4jLogger()).target(IntrospectionClient.class, introspectionEndpoint);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean registerNewResource(API api, Map resourceAttributes) throws APIManagementException {
        return false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map getResourceByApiId(String apiId) throws APIManagementException {
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean updateRegisteredResource(API api, Map resourceAttributes) throws APIManagementException {
        return false;
    }

    @Override
    public void deleteRegisteredResourceByAPIId(String apiID) throws APIManagementException {
        // not applicable
    }

    @Override
    public void deleteMappedApplication(String consumerKey) throws APIManagementException {
        // not applicable
    }

    @Override
    public Set<String> getActiveTokensByConsumerKey(String consumerKey) throws APIManagementException {
        return Collections.emptySet();
    }

    @Override
    public AccessTokenInfo getAccessTokenByConsumerKey(String consumerKey) throws APIManagementException {
        return null;
    }

    @Override
    public Map<String, Set<Scope>> getScopesForAPIS(String apiIdsString) throws APIManagementException {
        Map<String, Set<Scope>> scopes = new HashMap<>();
        Map<String, Set<String>> apiToScopeMapping = ApiMgtDAO.getInstance().getScopesForAPIS(apiIdsString);
        for (Entry<String, Set<String>> entry : apiToScopeMapping.entrySet()) {
            Set<Scope> apiScopes = new LinkedHashSet<>();
            for (String scopeName : entry.getValue()) {
                apiScopes.add(getScopeByName(scopeName));
            }
            scopes.put(entry.getKey(), apiScopes);
        }
        return scopes;
    }

    @Override
    public void registerScope(Scope scope) throws APIManagementException {
        // not applicable
    }

    @Override
    public Scope getScopeByName(String name) throws APIManagementException {
        return null;
    }

    @Override
    public Map<String, Scope> getAllScopes() throws APIManagementException {
        return null;
    }

    @Override
    public void deleteScope(String scopeName) throws APIManagementException {
        // not applicable
    }

    @Override
    public void updateScope(Scope scope) throws APIManagementException {
        // not applicable
    }

    @Override
    public boolean isScopeExists(String scopeName) throws APIManagementException {
        return false;
    }

    @Override
    public String getType() {
        return GluuConstants.GLUU_TYPE;
    }
}
