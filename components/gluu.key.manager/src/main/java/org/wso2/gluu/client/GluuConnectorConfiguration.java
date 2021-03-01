package org.wso2.gluu.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.apimgt.api.model.ConfigurationDto;
import org.wso2.carbon.apimgt.api.model.KeyManagerConnectorConfiguration;

@Component(name = "gluu.configuration.component", immediate = true, service = KeyManagerConnectorConfiguration.class)
public class GluuConnectorConfiguration implements KeyManagerConnectorConfiguration {

    @Override
    public String getImplementation() {
        return GluuKeyManagerClient.class.getName();
    }

    @Override
    public String getJWTValidator() {
        return null;
    }

    @Override
    public List<ConfigurationDto> getConnectionConfigurations() {
        // TODO: implement
        return new ArrayList<>();
    }

    @Override
    public List<ConfigurationDto> getApplicationConfigurations() {
        List<ConfigurationDto> configurationDtoList = new ArrayList<>();

        // application types
        configurationDtoList.add(new ConfigurationDto("application_type", "Application Type", "select",
                "Type Of Application to create", "web", false, false, Arrays.asList("web", "native"), false));

        // response types
        configurationDtoList.add(new ConfigurationDto("response_types", "Response Type", "select",
                "Type of token response", "", false, false, Arrays.asList("token", "code", "id_token", "token code",
                        "code id_token", "token id_token", "token code id_token"),
                true));

        // token endpoint authentication method
        configurationDtoList.add(new ConfigurationDto("token_endpoint_auth_method",
                "Token endpoint Authentication Method", "select", "How to Authenticate Token Endpoint",
                "client_secret_basic", true, false,
                Arrays.asList("client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt"),
                false));

        return configurationDtoList;
    }

    @Override
    public String getType() {
        return GluuConstants.GLUU;
    }

    @Override
    public String getDisplayName() {
        return GluuConstants.GLUU_DISPLAY_NAME;
    }

    @Override
    public String getDefaultConsumerKeyClaim() {
        return GluuConstants.CONSUMER_KEY_CLAIM;
    }

    @Override
    public String getDefaultScopesClaim() {
        return GluuConstants.SCOPE_CLAIM;
    }

}
