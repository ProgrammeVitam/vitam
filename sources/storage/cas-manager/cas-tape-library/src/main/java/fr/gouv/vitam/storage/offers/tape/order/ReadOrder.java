package fr.gouv.vitam.storage.offers.tape.order;

public class ReadOrder implements Order {
    private String tapeCode;
    private Integer filePosition;

    public ReadOrder() {
    }

    public ReadOrder(String tapeCode, Integer filePosition) {
        this.tapeCode = tapeCode;
        this.filePosition = filePosition;
    }

    public String getTapeCode() {
        return tapeCode;
    }

    public void setTapeCode(String tapeCode) {
        this.tapeCode = tapeCode;
    }

    public Integer getFilePosition() {
        return filePosition;
    }

    public void setFilePosition(Integer filePosition) {
        this.filePosition = filePosition;
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
