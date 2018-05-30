package fr.gouv.vitam.metadata.api.model;

import java.util.Objects;

/**
 * ObjectGroupPerOriginatingAgency class describing ObjectGroup
 */
public class Symbolic {

    private String agency;
    private boolean symbolic;

    public Symbolic(String agency, boolean symbolic) {
        this.agency = agency;
        this.symbolic = symbolic;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public boolean isSymbolic() {
        return symbolic;
    }

    public void setSymbolic(boolean symbolic) {
        this.symbolic = symbolic;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Symbolic) {
            Symbolic ss = (Symbolic) obj;
            return Objects.equals(symbolic, ss.symbolic) && Objects.equals(agency, ss.agency);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(agency, symbolic);
    }
}
