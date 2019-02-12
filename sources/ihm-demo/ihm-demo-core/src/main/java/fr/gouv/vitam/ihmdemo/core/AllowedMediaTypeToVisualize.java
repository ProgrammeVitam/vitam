package fr.gouv.vitam.ihmdemo.core;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;

public class AllowedMediaTypeToVisualize {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AllowedMediaTypeToVisualize.class);
    private static final String CONFIGURATION_FILE = "allowed-mediatypes.conf";
    private static AllowedMediaTypesConfiguration configuration = null;

    public static final boolean isAllowedMediaTypeToVisualize(MediaType mediaType) {
        if(configuration == null) {
            try {
                File configurationFile = PropertiesUtils.findFile(CONFIGURATION_FILE);
                configuration = PropertiesUtils.readYaml(configurationFile, AllowedMediaTypesConfiguration.class);
            } catch (IOException e) {
                LOGGER.debug("Error when retrieving configuration file : " + CONFIGURATION_FILE);
                return false;
            }
        }
        for(MediaType amt : configuration.getAllowedMediaTypes()) {
            if(amt.getType().equals(mediaType.getType()) && amt.getSubtype().equals(mediaType.getSubtype())) {
                 return true;
            }
        }
        return false;
    }
}
