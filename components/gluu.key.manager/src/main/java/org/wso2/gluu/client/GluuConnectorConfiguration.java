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
        return new ArrayList<>();
    }

    @Override
    public List<ConfigurationDto> getApplicationConfigurations() {
        List<ConfigurationDto> configurationDtoList = new ArrayList<>();
        configurationDtoList.add(new ConfigurationDto("response_types", "Response Type", "select",
                "Type of token response", "", false, false, Arrays.asList("token", "code", "id_token"), true));

        configurationDtoList.add(new ConfigurationDto("token_endpoint_auth_method",
                "Token endpoint Authentication Method", "select", "How to Authenticate Token Endpoint",
                "client_secret_basic", true, false, Arrays.asList("client_secret_basic", "client_secret_post",
                        "client_secret_jwt", "tls_client_auth", "private_key_jwt", "self_signed_tls_client_auth"),
                false));
        return configurationDtoList;
    }

    @Override
    public String getType() {
        return GluuConstants.GLUU_TYPE;
    }

    @Override
    public String getDisplayName() {
        return GluuConstants.GLUU_DISPLAY_NAME;
    }

}
