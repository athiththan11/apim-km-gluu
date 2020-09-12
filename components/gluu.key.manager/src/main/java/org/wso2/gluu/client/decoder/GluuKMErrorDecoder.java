package org.wso2.gluu.client.decoder;

import feign.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.kmclient.KMClientErrorDecoder;

public class GluuKMErrorDecoder extends KMClientErrorDecoder {

    private static final Log log = LogFactory.getLog(GluuKMErrorDecoder.class);

    @Override
    public Exception decode(String methodKey, Response response) {

        if (log.isDebugEnabled()) {
            log.debug("Response received for method: " + methodKey + ", response: " + response.toString());
        }

        return super.decode(methodKey, response);
    }
}
