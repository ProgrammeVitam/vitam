/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

public class BinaryEventData {

    public static final String MESSAGE_DIGEST = "MessageDigest";
    private static final String FILE_NAME = "FileName";
    private static final String OFFERS = "Offers";
    private static final String ALGORITHM = "Algorithm";

    private final String messageDigest;
    private final String fileName;
    private final String offers;
    private final String algorithm;

    @JsonCreator
    public BinaryEventData(
        @JsonProperty(MESSAGE_DIGEST) String messageDigest,
        @JsonProperty(FILE_NAME) String fileName,
        @JsonProperty(OFFERS) String offers,
        @JsonProperty(ALGORITHM) String algorithm
    ) {
        this.messageDigest = messageDigest;
        this.fileName = fileName;
        this.offers = offers;
        this.algorithm = algorithm;
    }

    public static BinaryEventData from(String messageDigest) {
        return new BinaryEventData(messageDigest, null, null, null);
    }

    public static BinaryEventData from(StoredInfoResult storedInfoResult) {
        return new BinaryEventData(
            storedInfoResult.getDigest(),
            null,
            String.join(", ", storedInfoResult.getOfferIds()),
            storedInfoResult.getDigestType()
        );
    }

    @JsonProperty(MESSAGE_DIGEST)
    public String getMessageDigest() {
        return messageDigest;
    }

    @JsonProperty(FILE_NAME)
    public String getFileName() {
        return fileName;
    }

    @JsonProperty(OFFERS)
    public String getOffers() {
        return offers;
    }

    @JsonProperty(ALGORITHM)
    public String getAlgorithm() {
        return algorithm;
    }
}
