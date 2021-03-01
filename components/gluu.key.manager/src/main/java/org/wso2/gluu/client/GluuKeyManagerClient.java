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
import java.util.List;
import java.util.Map;
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
import org.wso2.carbon.apimgt.impl.kmclient.ApacheFeignHttpClient;
import org.wso2.carbon.apimgt.impl.kmclient.KMClientErrorDecoder;
import org.wso2.carbon.apimgt.impl.kmclient.KeyManagerClientException;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.gluu.client.kmclient.DCRClient;
import org.wso2.gluu.client.kmclient.IntrospectClient;
import org.wso2.gluu.client.model.AccessTokenResponse;
import org.wso2.gluu.client.model.ClientInfo;
import org.wso2.gluu.client.model.IntrospectInfo;

import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.slf4j.Slf4jLogger;

/**
 * Key manager implementation to integrate Gluu server with WSO2 API Manager
 */
public class GluuKeyManagerClient extends AbstractKeyManager {

    private DCRClient dcrClient;
    private IntrospectClient introspectClient;
    private static Map<String, String> registrationTokenMap = new HashMap<>();

    private static final Log log = LogFactory.getLog(GluuKeyManagerClient.class);

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
    public String getType() {
        return GluuConstants.GLUU;
    }

    @Override
    public void loadConfiguration(KeyManagerConfiguration keyManagerConfiguration) throws APIManagementException {
        this.configuration = keyManagerConfiguration;

        String dcrEndpoint = (String) this.configuration
                .getParameter(APIConstants.KeyManager.CLIENT_REGISTRATION_ENDPOINT);
        String introspectionEndpoint = (String) this.configuration
                .getParameter(APIConstants.KeyManager.INTROSPECTION_ENDPOINT);

        dcrClient = Feign.builder().client(new ApacheFeignHttpClient(APIUtil.getHttpClient(dcrEndpoint)))
                .encoder(new GsonEncoder()).decoder(new GsonDecoder()).errorDecoder(new KMClientErrorDecoder())
                .logger(new Slf4jLogger()).target(DCRClient.class, dcrEndpoint);

        introspectClient = Feign.builder()
                .client(new ApacheFeignHttpClient(APIUtil.getHttpClient(introspectionEndpoint)))
                .encoder(new GsonEncoder()).decoder(new GsonDecoder()).errorDecoder(new KMClientErrorDecoder())
                .logger(new Slf4jLogger()).target(IntrospectClient.class, introspectionEndpoint);
    }

    @Override
    public OAuthApplicationInfo createApplication(OAuthAppRequest oauthAppRequest) throws APIManagementException {

        OAuthApplicationInfo oauthApplicationInfo = oauthAppRequest.getOAuthApplicationInfo();
        if (oauthApplicationInfo != null) {
            ClientInfo clientInfo = createClientInfo(oauthApplicationInfo);
            ClientInfo createdApplication;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Creating Gluu client with object: " + clientInfo.toString());
                }

                createdApplication = dcrClient.createApplication(clientInfo);
                if (createdApplication != null) {
                    return createOAuthApplicationInfo(createdApplication);
                }
            } catch (KeyManagerClientException e) {
                handleException("Error occured while creating Gluu client", e);
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
    private ClientInfo createClientInfo(OAuthApplicationInfo oauthApplicationInfo) {

        ClientInfo clientInfo = new ClientInfo();

        String userId = (String) oauthApplicationInfo.getParameter(ApplicationConstants.OAUTH_CLIENT_USERNAME);
        String usernameForSp = MultitenantUtils.getTenantAwareUsername(userId);
        String domain = UserCoreUtil.extractDomainFromName(usernameForSp);

        if (!domain.isEmpty() && !UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME.equals(domain)) {
            usernameForSp = usernameForSp.replace(UserCoreConstants.DOMAIN_SEPARATOR, "_");
        }

        String applicationName = oauthApplicationInfo.getClientName();
        String keyType = (String) oauthApplicationInfo.getParameter(ApplicationConstants.APP_KEY_TYPE);
        String callBackUrl = oauthApplicationInfo.getCallBackURL();

        if (keyType != null) {
            applicationName = usernameForSp.concat("_").concat(applicationName).concat("_").concat(keyType);
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

        // set access token generation as JWT token when creating a client in the Gluu
        clientInfo.setAccessTokenAsJWT(true);

        Object parameter = oauthApplicationInfo.getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES);
        Map<String, Object> additionalProperties = new HashMap<>();
        if (parameter instanceof String) {
            additionalProperties = new Gson().fromJson((String) parameter, Map.class);
        }

        if (additionalProperties.containsKey(GluuConstants.APPLICATION_TYPE)) {
            clientInfo.setApplicationType((String) additionalProperties.get(GluuConstants.APPLICATION_TYPE));
        } else {
            clientInfo.setApplicationType(GluuConstants.DEFAULT_CLIENT_APPLICATION_TYPE);
        }

        if (additionalProperties.containsKey(GluuConstants.TOKEN_ENDPOINT_AUTH_METHOD)) {
            clientInfo.setTokenEndpointAuthMethod(
                    (String) additionalProperties.get(GluuConstants.TOKEN_ENDPOINT_AUTH_METHOD));
        }

        if (additionalProperties.containsKey(GluuConstants.RESPONSE_TYPES)
                && !(additionalProperties.get(GluuConstants.RESPONSE_TYPES) instanceof String)) {
            List<String> responseTypes = (List<String>) additionalProperties.get(GluuConstants.RESPONSE_TYPES);
            if (!responseTypes.isEmpty()) {
                clientInfo.setResponseTypes(responseTypes);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Gluu Client Object: " + clientInfo.toString());
        }

        return clientInfo;
    }

    /**
     * method to create {@code OAuthApplicationInfo} object from {@link ClientInfo}
     *
     * @param application response received from server as {@link ClientInfo}
     * @return {@code OAuthApplicationInfo} object
     */
    private OAuthApplicationInfo createOAuthApplicationInfo(ClientInfo application) {

        OAuthApplicationInfo appInfo = new OAuthApplicationInfo();
        appInfo.setClientName(application.getClientName());
        appInfo.setClientId(application.getClientId());
        appInfo.setClientSecret(application.getClientSecret());

        if (application.getRedirectUris() != null) {
            appInfo.setCallBackURL(String.join(",", application.getRedirectUris()));
        }

        if (application.getGrantTypes() != null) {
            appInfo.addParameter(ApplicationConstants.OAUTH_CLIENT_GRANT,
                    String.join(" ", application.getGrantTypes()));
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

            if (log.isDebugEnabled()) {
                log.debug("Invoking update operation to update Gluu client with consumer key: " + clientId);
            }

            String registrationAccessToken = getRegistrationAccessToken(clientId);
            if (registrationAccessToken != null) {
                ClientInfo clientInfo = createClientInfo(oauthApplicationInfo);

                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Updating Gluu client with consumer key: " + clientId + ", client: "
                                + clientInfo.toString());
                    }

                    ClientInfo updatedApplication = dcrClient.updateApplication(clientId, registrationAccessToken,
                            clientInfo);
                    if (updatedApplication != null) {
                        return createOAuthApplicationInfo(updatedApplication);
                    }
                } catch (KeyManagerClientException e) {
                    handleException("Error occured while updating Gluu client " + clientId, e);
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

            if (registrationTokenMap.containsKey(consumerKey)) {
                return registrationTokenMap.get(consumerKey);
            }

            ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();
            int applicationId = apiMgtDAO.getApplicationByClientId(consumerKey).getId();
            Set<APIKey> apiKeys = apiMgtDAO.getKeyMappingsFromApplicationId(applicationId);
            for (APIKey apiKey : apiKeys) {
                if (consumerKey.equals(apiKey.getConsumerKey()) && StringUtils.isNotEmpty(apiKey.getAppMetaData())) {
                    OAuthApplicationInfo storedOAuthApplicationInfo = new Gson().fromJson(apiKey.getAppMetaData(),
                            OAuthApplicationInfo.class);
                    String registrationAccessToken = (String) ((Map<String, Object>) storedOAuthApplicationInfo
                            .getParameter(APIConstants.JSON_ADDITIONAL_PROPERTIES))
                                    .get(GluuConstants.REGISTRATION_ACCESS_TOKEN);
                    registrationTokenMap.put(consumerKey, registrationAccessToken);
                    return registrationAccessToken;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Registration Access Token for Gluu client " + consumerKey + " is not found");
        }

        return null;
    }

    @Override
    public void deleteApplication(String consumerKey) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Invoking delete operation to delete Gluu client with consumer key: " + consumerKey);
        }

        String registrationAccessToken = getRegistrationAccessToken(consumerKey);
        if (registrationAccessToken != null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Deleting Gluu client " + consumerKey);
                }

                dcrClient.deleteApplication(consumerKey, registrationAccessToken);
            } catch (KeyManagerClientException e) {
                handleException("Error occured while deleting Gluu client " + consumerKey, e);
            }
        }
    }

    @Override
    public OAuthApplicationInfo retrieveApplication(String consumerKey) throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Invoking retrieve operation to retrieve the Gluu client with consumer key: " + consumerKey);
        }

        String registrationAccessToken = getRegistrationAccessToken(consumerKey);
        if (registrationAccessToken != null) {
            ClientInfo clientInfo;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Retrieving Gluu client " + consumerKey);
                }

                clientInfo = dcrClient.getApplication(consumerKey, registrationAccessToken);
                if (clientInfo != null) {
                    return createOAuthApplicationInfo(clientInfo);
                }
            } catch (KeyManagerClientException e) {
                handleException("Error occured while retrieving the Gluu client " + consumerKey, e);
            }
        }
        return null;
    }

    @Override
    public AccessTokenInfo getNewApplicationAccessToken(AccessTokenRequest tokenRequest) throws APIManagementException {

        String clientId = tokenRequest.getClientId();
        String clientSecret = tokenRequest.getClientSecret();
        Object grantType = tokenRequest.getGrantType();

        if (grantType == null) {
            grantType = GluuConstants.CLIENT_CREDENTIALS_GRANT_TYPE;
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
        AccessTokenInfo accessTokenInfo = new AccessTokenInfo();
        if (retrievedAccessTokenInfo != null) {
            accessTokenInfo.setConsumerKey(clientId);
            accessTokenInfo.setConsumerSecret(clientSecret);
            accessTokenInfo.setAccessToken(retrievedAccessTokenInfo.getAccessToken());
            if (retrievedAccessTokenInfo.getScope() != null) {
                accessTokenInfo.setScope(retrievedAccessTokenInfo.getScope().split("\\s+"));
            }
            accessTokenInfo.setValidityPeriod(retrievedAccessTokenInfo.getExpiry());
            return accessTokenInfo;
        }

        accessTokenInfo.setTokenValid(false);
        accessTokenInfo.setErrorcode(APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);

        if (log.isDebugEnabled()) {
            log.debug("Token validation/generation failed for consumer key: " + clientId);
        }

        return accessTokenInfo;
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
                handleException("Could not read HTTP entity for response: " + response);
            }

            if (statusCode == HttpStatus.SC_OK) {
                try (InputStream inputStream = entity.getContent()) {
                    String content = IOUtils.toString(inputStream);
                    return new Gson().fromJson(content, AccessTokenResponse.class);
                }
            }
        } catch (UnsupportedEncodingException e) {
            handleException("Encoding method is not supported", e);
        } catch (IOException e) {
            handleException("Error occured while reading or closing the buffer reader", e);
        }

        return null;
    }

    @Override
    public AccessTokenInfo getTokenMetaData(String accessToken) throws APIManagementException {

        AccessTokenInfo accessTokenInfo = new AccessTokenInfo();
        try {
            IntrospectInfo introspectInfo = introspectClient.introspect(accessToken);
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

                accessTokenInfo.addParameter(GluuConstants.ISSUER_CLAIM, introspectInfo.getIssuer());
                accessTokenInfo.addParameter(GluuConstants.JTI_CLAIM, introspectInfo.getJti());
            }
            return accessTokenInfo;
        } catch (KeyManagerClientException e) {
            throw new APIManagementException("Error occurred while introspecting an access token", e);
        }
    }

    @Override
    public Map<String, Set<Scope>> getScopesForAPIS(String apiIdsString) throws APIManagementException {
        return null;
    }

    @Override
    public OAuthApplicationInfo updateApplicationOwner(OAuthAppRequest appInfoDTO, String owner)
            throws APIManagementException {
        return appInfoDTO.getOAuthApplicationInfo();
    }

    /**
     * method to handle exceptions
     * 
     * @param msg error message
     * @throws APIManagementException
     */
    private void handleException(String msg) throws APIManagementException {
        log.error(msg);
        throw new APIManagementException(msg);
    }

    @Override
    public String getNewApplicationConsumerSecret(AccessTokenRequest tokenRequest) throws APIManagementException {
        return null;
    }

    @Override
    public KeyManagerConfiguration getKeyManagerConfiguration() throws APIManagementException {
        return this.configuration;
    }

    @Override
    public OAuthApplicationInfo mapOAuthApplication(OAuthAppRequest appInfoRequest) throws APIManagementException {
        // not applicable as the registration_access_token is required to retrieve the
        // client info
        return null;
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

}
