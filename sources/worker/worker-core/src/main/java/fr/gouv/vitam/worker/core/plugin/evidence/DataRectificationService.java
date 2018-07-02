/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.evidence;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.DataRectificationException;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DataCorrectionService class
 */
public class DataRectificationService {

    private StorageClient storageClient;

    /**
     * Constructor
     */
    DataRectificationService() {
        this(StorageClientFactory.getInstance().getClient());
    }

    /**
     * @param storageClient storageClient
     */
    private DataRectificationService(StorageClient storageClient) {
        this.storageClient = storageClient;
    }


    /**
     * @param line EvidenceAuditReportLine
     * @return true | false  if  correction is done
     * @throws InvalidParseOperationException InvalidParseOperationException
     * @throws StorageServerClientException   StorageServerClientException
     */
    public boolean correct(EvidenceAuditReportLine line)
        throws InvalidParseOperationException, StorageServerClientException {

        MetadataType objectType = line.getObjectType();
        switch (objectType) {
            case OBJECTGROUP:
                return correctObjectGroups(line);
            case UNIT:
                return correctUnits(line);
            default:
                throw new IllegalStateException(objectType.getName());
        }

    }

    private boolean correctUnits(EvidenceAuditReportLine line)
        throws InvalidParseOperationException, StorageServerClientException {
        String securedHash = line.getSecuredHash();
        List<String> goodOffers = new ArrayList<>();
        List<String> badOffers = new ArrayList<>();

        if (!doCorrection(line.getOffersHashes(), securedHash, goodOffers, badOffers)) {
            return false;
        }

        storageClient
            .copyObjectToOneOfferAnother(line.getIdentifier() + ".json", DataCategory.UNIT, goodOffers.get(0),
                badOffers.get(0));
        return true;


    }

    private boolean correctObjectGroups(EvidenceAuditReportLine line)
        throws InvalidParseOperationException, StorageServerClientException {

        int nbObjectsCorrected = 0;
        String securedHash = line.getSecuredHash();

        List<String> goodOffers = new ArrayList<>();
        List<String> badOffers = new ArrayList<>();
        // nothing
        if (doCorrection(line.getOffersHashes(), securedHash, goodOffers, badOffers)) {

            storageClient
                .copyObjectToOneOfferAnother(line.getIdentifier() + ".json", DataCategory.OBJECTGROUP,
                    goodOffers.get(0),
                    badOffers.get(0));

            nbObjectsCorrected++;
        }

        for (EvidenceAuditReportObject object : line.getObjectsReports()) {
            goodOffers.clear();
            badOffers.clear();
            securedHash = object.getSecuredHash();

            if (!doCorrection(object.getOffersHashes(), securedHash, goodOffers, badOffers)) {
                continue;
            }
            storageClient.copyObjectToOneOfferAnother(object.getIdentifier(), DataCategory.OBJECT, goodOffers.get(0),
                badOffers.get(0));

            nbObjectsCorrected++;
        }
        return nbObjectsCorrected > 0;
    }


    private boolean doCorrection(Map<String, String> offers, String securedHash, List<String> goodOffers,
        List<String> badOffers) {
        if (offers.isEmpty()) {
            return false;
        }
        if (offers.size() == 1) {
            return false;
        }

        for (Map.Entry<String, String> currentOffer : offers.entrySet()) {

            if (securedHash.equals(currentOffer.getValue())) {

                goodOffers.add(currentOffer.getKey());
            } else {
                badOffers.add(currentOffer.getKey());
            }
        }

        return goodOffers.isEmpty() || badOffers.isEmpty() || badOffers.size() > 1;
    }



    /**
     * @param id                binary identifier
     * @param category          the category
     * @param offerSourceId     offerSourceId
     * @param offersDestination offeDestination
     * @throws DataRectificationException throw {@link DataRectificationException if something happen }
     */
    public void correctOffers(String id, DataCategory category, String offerSourceId, List<String> offersDestination)
        throws
        DataRectificationException {

        for (String offerId : offersDestination) {

            try {
                storageClient.copyObjectToOneOfferAnother(id, category, offerSourceId, offerSourceId);
            } catch (StorageServerClientException | InvalidParseOperationException e) {
                throw new DataRectificationException(e);
            }

        }
    }
}
