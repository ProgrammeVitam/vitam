/**
 * Copyright (c) 2015 JSON.org
 *
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.worker.common.utils;

import java.util.HashSet;

/*
 * Copyright (c) 2015 JSON.org Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The
 * above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software. The Software shall be used for Good, not Evil. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XMLTokener;

/**
 * This provides static methods to convert an XML text into a JSONObject, and to covert a JSONObject into an XML text.
 *
 * @author JSON.org
 * @version 2016-01-30
 */
@SuppressWarnings("boxing")
public class XML {

    /** The Character '&amp;'. */
    public static final Character AMP = '&';

    /** The Character '''. */
    public static final Character APOS = '\'';

    /** The Character '!'. */
    public static final Character BANG = '!';

    /** The Character '='. */
    public static final Character EQ = '=';

    /** The Character '>'. */
    public static final Character GT = '>';

    /** The Character '&lt;'. */
    public static final Character LT = '<';

    /** The Character '?'. */
    public static final Character QUEST = '?';

    /** The Character '"'. */
    public static final Character QUOT = '"';

    /** The Character '/'. */
    public static final Character SLASH = '/';

    /**
     * List of PATH as key to force Array mode (note: currently it does not take into account full path but only
     * tokenName)
     */
    public static final Set<String> ARRAY_PATH = new HashSet<>();

    /**
     * Replace special characters with XML escapes:
     *
     * <pre>
     * &amp; <small>(ampersand)</small> is replaced by &amp;amp;
     * &lt; <small>(less than)</small> is replaced by &amp;lt;
     * &gt; <small>(greater than)</small> is replaced by &amp;gt;
     * &quot; <small>(double quote)</small> is replaced by &amp;quot;
     * </pre>
     *
     * @param string The string to be escaped.
     * @return The escaped string.
     */
    public static String escape(String string) {
        final StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, length = string.length(); i < length; i++) {
            final char c = string.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Throw an exception if the string contains whitespace. Whitespace is not allowed in tagNames and attributes.
     *
     * @param string A string.
     * @throws JSONException Thrown if the string contains whitespace or is empty.
     */
    public static void noSpace(String string) throws JSONException {
        int i;
        final int length = string.length();
        if (length == 0) {
            throw new JSONException("Empty string.");
        }
        for (i = 0; i < length; i += 1) {
            if (Character.isWhitespace(string.charAt(i))) {
                throw new JSONException("'" + string + "' contains a space character.");
            }
        }
    }

    private static void addArray(JSONObject json, String key, Object value) {
        final Object object = json.opt(key);
        if (object == null) {
            json.put(key, new JSONArray().put(value));
        } else if (object instanceof JSONArray) {
            ((JSONArray) object).put(value);
        } else {
            json.put(key, new JSONArray().put(object).put(value));
        }
    }

    /**
     * Scan the content following the named tag, attaching it to the context.
     *
     * @param x The XMLTokener containing the source string.
     * @param context The JSONObject that will include the new material.
     * @param name The tag name.
     * @return true if the close tag is processed.
     * @throws JSONException
     */
    private static boolean parse(XMLTokener x, JSONObject context, String name, boolean keepStrings)
        throws JSONException {
        char c;
        int i;
        JSONObject jsonobject = null;
        String string;
        String tagName;
        Object token;

        // Test for and skip past these forms:
        // <!-- ... -->
        // <! ... >
        // <![ ... ]]>
        // <? ... ?>
        // Report errors for these forms:
        // <>
        // <=
        // <<

        token = x.nextToken();

        // <!

        if (token == BANG) {
            c = x.next();
            if (c == '-') {
                if (x.next() == '-') {
                    x.skipPast("-->");
                    return false;
                }
                x.back();
            } else if (c == '[') {
                x.nextCDATA();
                return false;
                // Ignore CDATA
                /*
                 * token = x.nextToken(); if ("CDATA".equals(token)) { if (x.next() == '[') { string = x.nextCDATA(); if
                 * (string.length() > 0) { context.accumulate("content", string); } return false; } } throw
                 * x.syntaxError("Expected 'CDATA['");
                 */
            }
            i = 1;
            do {
                token = x.nextMeta();
                if (token == null) {
                    throw x.syntaxError("Missing '>' after '<!'.");
                } else if (token == LT) {
                    i += 1;
                } else if (token == GT) {
                    i -= 1;
                }
            } while (i > 0);
            return false;
        } else if (token == QUEST) {

            // <?
            x.skipPast("?>");
            return false;
        } else if (token == SLASH) {

            // Close tag </

            token = x.nextToken();
            if (name == null) {
                throw x.syntaxError("Mismatched close tag " + token);
            }
            if (!token.equals(name)) {
                throw x.syntaxError("Mismatched " + name + " and " + token);
            }
            if (x.nextToken() != GT) {
                throw x.syntaxError("Misshaped close tag");
            }
            return true;

        } else if (token instanceof Character) {
            throw x.syntaxError("Misshaped tag");

            // Open tag <

        } else {
            tagName = (String) token;
            token = null;
            jsonobject = new JSONObject();
            for (;;) {
                if (token == null) {
                    token = x.nextToken();
                }

                // attribute = value
                if (token instanceof String) {
                    string = (String) token;
                    token = x.nextToken();
                    if (token == EQ) {
                        token = x.nextToken();
                        if (!(token instanceof String)) {
                            throw x.syntaxError("Missing value");
                        }
                        // was jsonobject
                        context.accumulate(tagName + "@" + string,
                            keepStrings ? token : JSONObject.stringToValue((String) token));
                        token = null;
                    } else {
                        // was jsonobject
                        context.accumulate(tagName + "@" + string, "");
                    }


                } else if (token == SLASH) {
                    // Empty tag <.../>
                    if (x.nextToken() != GT) {
                        throw x.syntaxError("Misshaped tag");
                    }
                    if (jsonobject.length() > 0) {
                        if (ARRAY_PATH.contains(tagName)) {
                            addArray(context, tagName, jsonobject);
                        } else {
                            context.accumulate(tagName, jsonobject);
                        }
                    } else {
                        if (ARRAY_PATH.contains(tagName)) {
                            addArray(context, tagName, "");
                        } else {
                            context.accumulate(tagName, "");
                        }
                    }
                    return false;

                } else if (token == GT) {
                    // Content, between <...> and </...>
                    for (;;) {
                        token = x.nextContent();
                        if (token == null) {
                            if (tagName != null) {
                                throw x.syntaxError("Unclosed tag " + tagName);
                            }
                            return false;
                        } else if (token instanceof String) {
                            string = (String) token;
                            if (string.length() > 0) {
                                jsonobject.accumulate("content",
                                    keepStrings ? token : JSONObject.stringToValue(string));
                            }

                        } else if (token == LT) {
                            // Nested element
                            if (parse(x, jsonobject, tagName, keepStrings)) {
                                if (jsonobject.length() == 0) {
                                    if (ARRAY_PATH.contains(tagName)) {
                                        addArray(context, tagName, "");
                                    } else {
                                        context.accumulate(tagName, "");
                                    }
                                } else if (jsonobject.length() == 1 && jsonobject.opt("content") != null) {
                                    if (ARRAY_PATH.contains(tagName)) {
                                        addArray(context, tagName, jsonobject.opt("content"));
                                    } else {
                                        context.accumulate(tagName,
                                            jsonobject.opt("content"));
                                    }
                                } else {
                                    if (ARRAY_PATH.contains(tagName)) {
                                        addArray(context, tagName, jsonobject);
                                    } else {
                                        context.accumulate(tagName, jsonobject);
                                    }
                                }
                                return false;
                            }
                        }
                    }
                } else {
                    throw x.syntaxError("Misshaped tag");
                }
            }
        }
    }

    /**
     * This method has been deprecated in favor of the {@link JSONObject.stringToValue(String)} method. Use it instead.
     *
     * @deprecated Use {@link JSONObject#stringToValue(String)} instead.
     * @param string String to convert
     * @return JSON value of this string or the string
     */
    @Deprecated
    public static Object stringToValue(String string) {
        return JSONObject.stringToValue(string);
    }

    /**
     * Convert a well-formed (but not necessarily valid) XML string into a JSONObject. Some information may be lost in
     * this transformation because JSON is a data format and XML is a document format. XML uses elements, attributes,
     * and content text, while JSON uses unordered collections of name/value pairs and arrays of values. JSON does not
     * does not like to distinguish between elements and attributes. Sequences of similar elements are represented as
     * JSONArrays. Content text may be placed in a "content" member. Comments, prologs, DTDs, and
     * <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param string The source string.
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(String string) throws JSONException {
        return toJSONObject(string, false);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a JSONObject. Some information may be lost in
     * this transformation because JSON is a data format and XML is a document format. XML uses elements, attributes,
     * and content text, while JSON uses unordered collections of name/value pairs and arrays of values. JSON does not
     * does not like to distinguish between elements and attributes. Sequences of similar elements are represented as
     * JSONArrays. Content text may be placed in a "content" member. Comments, prologs, DTDs, and
     * <code>&lt;[ [ ]]></code> are ignored.
     *
     * All values are converted as strings, for 1, 01, 29.0 will not be coerced to numbers but will instead be the exact
     * value as seen in the XML document.
     *
     * @param string The source string.
     * @param keepStrings If true, then values will not be coerced into boolean or numeric values and will instead be
     *        left as strings
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(String string, boolean keepStrings) throws JSONException {
        final JSONObject jo = new JSONObject();
        final XMLTokener x = new XMLTokener(string);
        while (x.more() && x.skipPast("<")) {
            parse(x, jo, null, keepStrings);
        }
        return jo;
    }

    /**
     * Convert a JSONObject into a well-formed, element-normal XML string.
     *
     * @param object A JSONObject.
     * @return A string.
     * @throws JSONException Thrown if there is an error parsing the string
     */
    public static String toString(Object object) throws JSONException {
        return toString(object, null);
    }

    /**
     * Convert a JSONObject into a well-formed, element-normal XML string.
     *
     * @param object A JSONObject.
     * @param tagName The optional name of the enclosing tag.
     * @return A string.
     * @throws JSONException Thrown if there is an error parsing the string
     */
    public static String toString(Object object, String tagName)
        throws JSONException {
        final StringBuilder sb = new StringBuilder();
        JSONArray ja;
        JSONObject jo;
        String key;
        Iterator<String> keys;
        String string;
        Object value;

        if (object instanceof JSONObject) {

            // Emit <tagName>
            if (tagName != null) {
                sb.append('<');
                sb.append(tagName);
                sb.append('>');
            }

            // Loop thru the keys.
            jo = (JSONObject) object;
            keys = jo.keys();
            while (keys.hasNext()) {
                key = keys.next();
                final boolean isAttribute = key.charAt(0) == '@';
                value = jo.opt(key);
                if (value == null) {
                    value = "";
                } else if (value.getClass().isArray()) {
                    value = new JSONArray(value);
                }
                string = value instanceof String ? (String) value : null;

                // Emit content in body
                if ("content".equals(key)) {
                    if (value instanceof JSONArray) {
                        ja = (JSONArray) value;
                        int i = 0;
                        for (final Object val : ja) {
                            if (i > 0) {
                                sb.append('\n');
                            }
                            sb.append(escape(val.toString()));
                            i++;
                        }
                    } else {
                        sb.append(escape(value.toString()));
                    }

                    // Emit an array of similar keys

                } else if (value instanceof JSONArray) {
                    ja = (JSONArray) value;
                    for (final Object val : ja) {
                        if (val instanceof JSONArray) {
                            sb.append('<');
                            sb.append(key);
                            sb.append('>');
                            sb.append(toString(val));
                            sb.append("</");
                            sb.append(key);
                            sb.append(">\n");
                        } else {
                            sb.append(toString(val, key));
                        }
                    }
                } else if ("".equals(value)) {
                    sb.append('<');
                    sb.append(key);
                    sb.append("/>\n");

                    // Emit a new tag <k>

                } else {
                    sb.append(toString(value, key));
                }
            }
            if (tagName != null) {

                // Emit the </tagname> close tag
                sb.append("</");
                sb.append(tagName);
                sb.append(">\n");
            }
            return sb.toString();

        }

        if (object != null) {
            if (object.getClass().isArray()) {
                object = new JSONArray(object);
            }

            if (object instanceof JSONArray) {
                ja = (JSONArray) object;
                for (final Object val : ja) {
                    // XML does not have good support for arrays. If an array
                    // appears in a place where XML is lacking, synthesize an
                    // <array> element.
                    sb.append(toString(val, tagName == null ? "array" : tagName));
                }
                return sb.toString();
            }
        }

        string = object == null ? "null" : escape(object.toString());
        return tagName == null ? "\"" + string + "\""
            : string.length() == 0 ? "<" + tagName + "/>\n" : "<" + tagName + ">" + string + "</" + tagName + ">\n";

    }
}
