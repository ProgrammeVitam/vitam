package fr.gouv.vitam.storage.offers.tape.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.storage.engine.common.model.QueueEntity;

public class ReadOrder extends QueueEntity implements Order {
    public static final String TAPE_CODE = "tapeCode";
    public static final String FILE_POSITION = "filePosition";
    @JsonProperty(TAPE_CODE)
    private String tapeCode;
    @JsonProperty(FILE_POSITION)
    private Integer filePosition;

    public ReadOrder() {
        super(GUIDFactory.newGUID().getId());
    }

    public ReadOrder(String tapeCode, Integer filePosition) {
        this();
        this.tapeCode = tapeCode;
        this.filePosition = filePosition;
    }

    public String getTapeCode() {
        return tapeCode;
    }

    public ReadOrder setTapeCode(String tapeCode) {
        this.tapeCode = tapeCode;
        return this;
    }

    public Integer getFilePosition() {
        return filePosition;
    }

    public ReadOrder setFilePosition(Integer filePosition) {
        this.filePosition = filePosition;
        return this;
    }

    @Override
    public boolean isWriteOrder() {
        return false;
    }

    @Override
    public boolean isReadOrder() {
        return true;
    }
}
