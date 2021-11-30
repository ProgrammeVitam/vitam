package fr.gouv.vitam.worker.core.plugin.evidence;

import java.util.List;
import java.util.Map;

public class DataRectificationHelper {

    private DataRectificationHelper() {
        // Empty
    }

    public static boolean doCorrection(Map<String, String> offers, String securedHash, List<String> goodOffers,
        List<String> badOffers) {
        if (offers.isEmpty()) {
            return false;
        }
        if (offers.size() == 1) {
            return false;
        }

        for (Map.Entry<String, String> currentOffer : offers.entrySet()) {

            if (securedHash.equals(currentOffer.getValue())) {

                goodOffers.add(currentOffer.getKey());
            } else {
                badOffers.add(currentOffer.getKey());
            }
        }

        return !goodOffers.isEmpty() && !badOffers.isEmpty() && badOffers.size() == 1;
    }
}
