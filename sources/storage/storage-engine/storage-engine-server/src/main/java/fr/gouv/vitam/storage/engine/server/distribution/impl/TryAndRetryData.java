/**
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
 */

package fr.gouv.vitam.storage.engine.server.distribution.impl;

import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * manage list ok and ko for retry storage feature
 */
public class TryAndRetryData {
    /**
     * Offers transfer OK
     */
    private List<String> okList;
    /**
     * Offers transfer KO
     */
    private List<String> koList;

    /**
     * Result
     */
    private Map<String, Response.Status> globalOfferResult;

    /**
     * Populate KO offer with offer list to start a new object
     * transfer
     *
     * @param storageOffers
     *            list of offer reference
     */
    public void populateFromOffers(List<StorageOffer> storageOffers) {
        globalOfferResult = new HashMap<>();
        if (okList == null) {
            okList = new ArrayList<>();
        }
        if (koList == null) {
            koList = new ArrayList<>();
        }
        for (StorageOffer storageOffer : storageOffers) {
            koList.add(storageOffer.getId());
            globalOfferResult.put(storageOffer.getId(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get OK offers list
     *
     * @return the ok offers list
     */
    public List<String> getOkList() {
        return okList;
    }

    /**
     * Set Ok offers list
     *
     * @param okList
     *            the ok offer list
     */
    public void setOkList(List<String> okList) {
        this.okList = okList;
    }

    /**
     * Get the KO offers list
     *
     * @return the KO offers list
     */
    public List<String> getKoList() {
        return new ArrayList<String>(koList);
    }

    /**
     * Set the KO offers list
     *
     * @param koList
     *            the KO offers list
     */
    public void setKoList(List<String> koList) {
        this.koList = koList;
    }

    /**
     * Get global result for storage distribution
     *
     * @return the map of global transfer result
     */
    public Map<String, Response.Status> getGlobalOfferResult() {
        return globalOfferResult;
    }

    /**
     * Pass offerId fro KO offers list to OK offers list
     *
     * @param offerId
     *            the offerId
     */
    public void koListToOkList(String offerId) {
        koList.remove(offerId);
        okList.add(offerId);
        globalOfferResult.put(offerId, Response.Status.CREATED);
    }

    /**
     * Change the status of an offer id transfer
     *
     * @param offerId the offerId
     * @param status the response status to set
     */
    public void changeStatus(String offerId, Response.Status status) {
        globalOfferResult.put(offerId, status);
    }
}
