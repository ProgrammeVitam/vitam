package fr.gouv.vitam.common.json;

import java.util.List;

public class Differences {
    public final List<Difference> diff;

    public Differences(List<Difference> diff) {
        this.diff = diff;
    }

    public List<Difference> getDiff() {
        return diff;
    }
}
