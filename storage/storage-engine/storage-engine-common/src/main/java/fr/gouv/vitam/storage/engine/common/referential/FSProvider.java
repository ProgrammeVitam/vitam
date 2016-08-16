/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.storage.engine.common.referential;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;

import java.io.IOException;

/**
 * File system implementation of the storage strategy and storage offer provider
 */
class FSProvider implements StorageStrategyProvider, StorageOfferProvider {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FSProvider.class);
    private static final String STRATEGY_FILENAME = "static-strategy.json";
    private static final String OFFER_FILENAME = "static-offer.json";
    /*
     * TODO : Use custom ObjectMapper because the ObjectMapper used in JsonHandler requires UPPER Camel case
     * json attribute to be able to map automatically json properties (ie: without field annotation) to java
     * attributes. Need a discussion on this with Frédéric.
     * TODO you can force Json variable to start with UpperCamelCase too ! so using the default ObjectMapper (which is UpperCamelCase since Seda is so)
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private StorageStrategy storageStrategy;
    private StorageOffer storageOffer;

    /**
     * Package protected to avoid out of scope instance creation
     */
    FSProvider() {
        initReferentials();
    }

    /**
     * Convenient enum to describe the different kind of referential handled by thi provider
     */
    private enum ReferentialType {
        STRATEGY,
        OFFER
    }

    @Override
    public StorageStrategy getStorageStrategy(String idStrategy) throws StorageTechnicalException {
        // TODO : only 1 strategy for now, need to use this id in later implementation
        if (storageStrategy != null) {
            return storageStrategy;
        }
        try {
            loadReferential(ReferentialType.STRATEGY);
        } catch (IOException exc) {
            throw new StorageTechnicalException(exc);
        }
        return storageStrategy;

    }

    @Override
    public StorageOffer getStorageOffer(String idOffer) throws StorageTechnicalException {
        // TODO : only 1 offer for now, need to use this id in later implementation
        if (storageOffer != null) {
            return storageOffer;
        }
        try {
            loadReferential(ReferentialType.OFFER);
        } catch (IOException exc) {
            throw new StorageTechnicalException(exc);
        }
        return storageOffer;
    }


    private void initReferentials() {
        try {
            loadReferential(ReferentialType.STRATEGY);
        } catch (IOException exc) {
            LOGGER.warn("Couldn't load " + STRATEGY_FILENAME + " file", exc);
        }
        try {
            loadReferential(ReferentialType.OFFER);
        } catch (IOException exc) {
            LOGGER.warn("Couldn't load " + OFFER_FILENAME + " file", exc);
        }
    }

    private void loadReferential(ReferentialType type) throws IOException {
        switch (type) {
            case STRATEGY:
                storageStrategy = OBJECT_MAPPER.readValue(PropertiesUtils.findFile(STRATEGY_FILENAME),
                    StorageStrategy.class);
                break;
            case OFFER:
                storageOffer = OBJECT_MAPPER.readValue(PropertiesUtils.findFile(OFFER_FILENAME),
                    StorageOffer.class);
                break;
            default:
                LOGGER.error("Referential loading not implemented for type: " + type);
        }
    }

    /**
     * For Junit only
     *
     * @param strategy the new strategy
     */
    void setStorageStrategy(StorageStrategy strategy) {
        storageStrategy = strategy;
    }

    /**
     * For Junit only
     *
     * @param offer the new offer
     */
    void setStorageOffer(StorageOffer offer) {
        storageOffer = offer;
    }
}
