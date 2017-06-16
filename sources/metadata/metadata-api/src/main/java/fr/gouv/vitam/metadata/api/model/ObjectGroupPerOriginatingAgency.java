package fr.gouv.vitam.metadata.api.model;

public class ObjectGroupPerOriginatingAgency {

    private String originatingAgency;

    private int numberOfObject;

    private int numberOfGOT;

    private int size;

    public ObjectGroupPerOriginatingAgency() {
    }

    public ObjectGroupPerOriginatingAgency(String originatingAgency, int numberOfObject, int numberOfGOT, int size) {
        this.originatingAgency = originatingAgency;
        this.numberOfObject = numberOfObject;
        this.numberOfGOT = numberOfGOT;
        this.size = size;
    }

    public String getOriginatingAgency() {
        return originatingAgency;
    }

    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public int getNumberOfObject() {
        return numberOfObject;
    }

    public void setNumberOfObject(int numberOfObject) {
        this.numberOfObject = numberOfObject;
    }

    public int getNumberOfGOT() {
        return numberOfGOT;
    }

    public void setNumberOfGOT(int numberOfGOT) {
        this.numberOfGOT = numberOfGOT;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}
