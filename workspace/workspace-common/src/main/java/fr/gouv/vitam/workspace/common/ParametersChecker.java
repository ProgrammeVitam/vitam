package fr.gouv.vitam.workspace.common;

import com.google.common.base.Strings;
// TODO REVIEW missing licence
/**
 * Workspace Checker Parameters
 * 
 *
 */
public class ParametersChecker {

    // FIXME REVIEW Explicitly add IllegalArgumentException in the signature
    // TODO REVIEW comment
    public static final void checkParamater(String errorMessage, String... parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        for (String parameter : parameters) {
            if (Strings.isNullOrEmpty(parameter)) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }

}
