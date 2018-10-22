package fr.gouv.vitam.common.model.unit;

/**
 * This object content the new technical object group guid and the an boolean. It is created when the BDO not
 * contains an GO with isVisited=false. When the list of AU is browsed, if an AU referenced and the BDO not contains
 * an GO, the boolean of this object change to true
 */
public class GotObj {
    private String gotGuid;
    private boolean isVisited;

    public GotObj(String gotGuid, boolean isVisited) {
        this.gotGuid = gotGuid;
        this.isVisited = isVisited;
    }

    public String getGotGuid() {
        return gotGuid;
    }

    @SuppressWarnings("unused")
    public void setGotGuid(String gotGuid) {
        this.gotGuid = gotGuid;
    }

    @SuppressWarnings("unused")
    public boolean isVisited() {
        return isVisited;
    }

    public void setVisited(boolean visited) {
        isVisited = visited;
    }
}
