/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.elastic.kibana.interceptor.rest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * ReplacePatternUtils
 */
public class ReplacePatternUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InterceptorResource.class);
    private static final String SHARP_REPLACEMENT = "\"#$1";
    private static final String UNDERSCORE_REPLACEMENT = "\"_$1";
    private static final String REGEXP_START_SHARP = "\"#(";
    private static final String REGEXP_START_UNDERSCORE = "\"_(";
    private static final String END_REGEXP = ")\\b";
    private final Pattern sharpPattern;
    private final Pattern underscorePattern;

    /**
     * Constructor
     *
     * @param whiteList white list define in configuration file.
     */
    public ReplacePatternUtils(List<String> whiteList) {
        sharpPattern = Pattern.compile(buildRegexPatternWithWhiteList(whiteList, REGEXP_START_SHARP));
        underscorePattern = Pattern.compile(buildRegexPatternWithWhiteList(whiteList, REGEXP_START_UNDERSCORE));
    }

    private String buildRegexPatternWithWhiteList(List<String> whiteList, String regexStart) {
        int i = 0;
        StringBuilder regexExpressionBuilder = new StringBuilder(regexStart);
        for (String whiteListArgument : whiteList) {
            if (whiteList.size() - 1 == i) {
                regexExpressionBuilder.append(whiteListArgument);
            } else {
                regexExpressionBuilder.append(whiteListArgument).append("|");
            }
            i++;
        }
        regexExpressionBuilder.append(END_REGEXP);
        return regexExpressionBuilder.toString();
    }

    /**
     * Replace Underscore By Sharp
     *
     * @param textToReplace textToReplace
     * @return the text replaced with sharp
     */
    public String replaceUnderscoreBySharp(String textToReplace) {
        LOGGER.debug("textToReplace" + textToReplace);
        String replacedString = replaceStringWithPattern(textToReplace, underscorePattern, SHARP_REPLACEMENT);
        LOGGER.debug("replacedString" + replacedString);
        return replacedString.replaceAll("_nbc", "#nbc");
    }

    /**
     * Replace Sharp By Underscore
     *
     * @param textToReplace textToReplace
     * @return the text replaced with underscore
     */
    public String replaceSharpByUnderscore(String textToReplace) {
        LOGGER.debug("textToReplace" + textToReplace);
        String replacedString = replaceStringWithPattern(textToReplace, sharpPattern, UNDERSCORE_REPLACEMENT);
        LOGGER.debug("replacedString" + replacedString);
        return replacedString.replaceAll("#nbc", "_nbc");
    }

    private String replaceStringWithPattern(String textToReplace, Pattern pattern, String replacement) {
        Matcher m = pattern.matcher(textToReplace);
        if (m.find()) {
            return m.replaceAll(replacement);
        } else {
            return textToReplace;
        }
    }
}
