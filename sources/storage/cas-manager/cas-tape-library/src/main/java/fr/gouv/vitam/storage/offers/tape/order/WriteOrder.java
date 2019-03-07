package fr.gouv.vitam.storage.offers.tape.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.storage.engine.common.model.QueueEntity;

public class WriteOrder extends QueueEntity implements Order {

    public static final String BUCKET = "bucket";
    public static final String FILE_PATH = "filePath";
    @JsonProperty(BUCKET)
    private String bucket;
    @JsonProperty(FILE_PATH)
    private String filePath;

    public WriteOrder() {
        super(GUIDFactory.newGUID().getId());
    }

    public WriteOrder(String bucket, String filePath) {
        this();
        this.bucket = bucket;
        this.filePath = filePath;
    }

    public String getBucket() {
        return bucket;
    }

    public WriteOrder setBucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public WriteOrder setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    @Override
    public boolean isWriteOrder() {
        return true;
    }

    @Override
    public boolean isReadOrder() {
        return false;
    }
}
