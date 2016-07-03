/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.core;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.api.AccessModule;
import fr.gouv.vitam.access.common.exception.AccessExecutionException;
import fr.gouv.vitam.access.common.model.AccessModuleBean;
import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;

/**
 * AccessModuleImpl implements AccessModule
 */
public class AccessModuleImpl implements AccessModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessModuleImpl.class);

    private final AccessConfiguration accessConfiguration;

    private MetaDataClientFactory metaDataClientFactory;

    private MetaDataClient metaDataClient;

    /**
     * AccessModuleImpl constructor
     *
     * @param configuration of mongoDB access
     */
    // constructor
    public AccessModuleImpl(AccessConfiguration configuration) {
        accessConfiguration = configuration;
    }

    /**
     * AccessModuleImpl constructor <br>
     * with metaDataClientFactory and configuration
     *
     * @param metaDataClientFactory {@link MetaDataClientFactory}
     * @param configuration {@link AccessConfiguration} access configuration
     */
    public AccessModuleImpl(MetaDataClientFactory metaDataClientFactory, AccessConfiguration configuration) {
        this.metaDataClientFactory = metaDataClientFactory;
        accessConfiguration = configuration;
    }

    /**
     * select Unit
     *
     * @param selectRequest as String { $query : query}
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessExecutionException Throw if error occurs when send Unit to database
     * @throws MetadataInvalidSelectException
     * @throws MetaDataDocumentSizeException
     */
    @Override
    public JsonNode selectUnit(String selectRequest)
        throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException,
        MetadataInvalidSelectException, MetaDataDocumentSizeException {

        if (StringUtils.isBlank(selectRequest)) {
            throw new IllegalArgumentException("the request is blank");
        }
        final GUID guid = GUIDFactory.newGUID();

        final AccessModuleBean accessModuleBean = new AccessModuleBean(selectRequest, guid);
        JsonNode jsonNode = null;

        try {
            GlobalDatasParser.sanityRequestCheck(selectRequest);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(e);
        }
        try {

            if (metaDataClientFactory == null) {
                metaDataClientFactory = new MetaDataClientFactory();
            }
            metaDataClient = metaDataClientFactory.create(accessConfiguration.getUrlMetaData());

            jsonNode = metaDataClient.selectUnits(accessModuleBean.getRequestDsl());

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("parsing error", e);
            throw e;
        } catch (final MetadataInvalidSelectException e) {
            LOGGER.error("invalid select", e);
            throw e;
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error("document size problem", e);
            throw e;
        } catch (final MetaDataExecutionException e) {
            LOGGER.error("metadata execution problem", e);
            throw new AccessExecutionException(e);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("illegal argument", e);
            throw e;
        } catch (final Exception e) {
            LOGGER.error("exeption thrown", e);
            throw new AccessExecutionException(e);
        }
        return jsonNode;
    }
}
