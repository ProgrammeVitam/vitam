package fr.gouv.vitam.storage.offers.tape.order;

public class WriteOrder implements Order {
    private String bucket;
    private String filePath;

    public WriteOrder() {
    }

    public WriteOrder(String bucket, String filePath) {
        this.bucket = bucket;
        this.filePath = filePath;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
