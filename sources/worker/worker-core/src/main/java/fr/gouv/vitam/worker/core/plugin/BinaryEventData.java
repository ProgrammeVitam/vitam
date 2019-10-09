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
    public BinaryEventData(@JsonProperty(MESSAGE_DIGEST) String messageDigest, @JsonProperty(FILE_NAME) String fileName, @JsonProperty(OFFERS) String offers, @JsonProperty(ALGORITHM) String algorithm) {
        this.messageDigest = messageDigest;
        this.fileName = fileName;
        this.offers = offers;
        this.algorithm = algorithm;
    }

    public static BinaryEventData from(String messageDigest) {
        return new BinaryEventData(messageDigest, null, null, null);
    }

    public static BinaryEventData from(StoredInfoResult storedInfoResult) {
        return new BinaryEventData(storedInfoResult.getDigest(), null, String.join(", ", storedInfoResult.getOfferIds()), storedInfoResult.getDigestType());
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
