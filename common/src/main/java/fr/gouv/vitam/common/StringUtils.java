/**
 * This file is part of Vitam Project.
 * 
 * Copyright 2010, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Vitam. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 * String utils to unencode HTML string to normal string
 */
@SuppressWarnings("javadoc")

public class StringUtils {

    public static enum EMAIL_FIELDS {
        formatEML("eml"), formatMSG("msg"), formatMBOX("mbox"), formatPST("pst"),
        email, status, rankId,
        metadata, folder, folderName("name"), folderFile("file"),
        emailName("name"), emailAddress("address"),
        from, fromUnit("sender"),
        toRecipients, toUnit("to"), replyTo, ccRecipients, ccUnit("cc"), bccRecipients,
        bccUnit("bcc"),
        subject, conversationTopic, sentDate, receivedDate, receptionTrace, trace,
        emailSize("size"),
        encoding, description, contentType, contentTransferEncoding, contentLanguage,
        contentId, disposition, msgKeywords,
        messageId, inReplyTo, references, reference,
        properties, propForwarded("forwarded"), propReplied("replied"), propRead("read"),
        propUnsent("unsent"),
        propRecipientReassignmentProhibited("recipientReassignmentProhibited"),
        importance, priority, sensitivity, hasAttachment,
        attachments, attNumber, subidentity, creationTime, filename, attSize("size"),
        keywords, keywordRank("rank"), keywordOccur("occur"), keywordWord("word"),
        keywordValue("value");

        public String name;

        private EMAIL_FIELDS(String name) {
            this.name = name;
        }

        private EMAIL_FIELDS() {
            this.name = this.name();
        }
    }

    private StringUtils() {
    }

    public static enum CodeList {
        quot("\""),
        amp("&"),
        apos("'"),
        lt("<"),
        gt(">"),
        nbsp(" "),
        iexcl("¡"),
        cent("¢"),
        pound("£"),
        curren("¤"),
        yen("¥"),
        brvbar("¦"),
        sect("§"),
        uml("¨"),
        copy("©"),
        ordf("ª"),
        laquo("«"),
        not("¬"),
        shy(""),
        reg("®"),
        macr("¯"),
        deg("°"),
        plusmn("±"),
        sup2("²"),
        sup3("³"),
        acute("´"),
        micro("µ"),
        para("¶"),
        middot("·"),
        cedil("¸"),
        sup1("¹"),
        ordm("º"),
        raquo("»"),
        frac14("¼"),
        frac12("½"),
        frac34("¾"),
        iquest("¿"),
        Agrave("À"),
        Aacute("Á"),
        Acirc("Â"),
        Atilde("Ã"),
        Auml("Ä"),
        Aring("Å"),
        AElig("Æ"),
        Ccedil("Ç"),
        Egrave("È"),
        Eacute("É"),
        Ecirc("Ê"),
        Euml("Ë"),
        Igrave("Ì"),
        Iacute("Í"),
        Icirc("Î"),
        Iuml("Ï"),
        ETH("Ð"),
        Ntilde("Ñ"),
        Ograve("Ò"),
        Oacute("Ó"),
        Ocirc("Ô"),
        Otilde("Õ"),
        Ouml("Ö"),
        times("×"),
        Oslash("Ø"),
        Ugrave("Ù"),
        Uacute("Ú"),
        Ucirc("Û"),
        Uuml("Ü"),
        Yacute("Ý"),
        THORN("Þ"),
        szlig("ß"),
        agrave("à"),
        aacute("á"),
        acirc("â"),
        atilde("ã"),
        auml("ä"),
        aring("å"),
        aelig("æ"),
        ccedil("ç"),
        egrave("è"),
        eacute("é"),
        ecirc("ê"),
        euml("ë"),
        igrave("ì"),
        iacute("í"),
        icirc("î"),
        iuml("ï"),
        eth("ð"),
        ntilde("ñ"),
        ograve("ò"),
        oacute("ó"),
        ocirc("ô"),
        otilde("õ"),
        ouml("ö"),
        divide("÷"),
        oslash("ø"),
        ugrave("ù"),
        uacute("ú"),
        ucirc("û"),
        uuml("ü"),
        yacute("ý"),
        thorn("þ"),
        yuml("ÿ"),
        OElig("Œ"),
        oelig("œ"),
        Scaron("Š"),
        scaron("š"),
        Yuml("Ÿ"),
        fnof("ƒ"),
        circ("ˆ"),
        tilde("˜"),
        Alpha("Α"),
        Beta("Β"),
        Gamma("Γ"),
        Delta("Δ"),
        Epsilon("Ε"),
        Zeta("Ζ"),
        Eta("Η"),
        Theta("Θ"),
        Iota("Ι"),
        Kappa("Κ"),
        Lambda("Λ"),
        Mu("Μ"),
        Nu("Ν"),
        Xi("Ξ"),
        Omicron("Ο"),
        Pi("Π"),
        Rho("Ρ"),
        Sigma("Σ"),
        Tau("Τ"),
        Upsilon("Υ"),
        Phi("Φ"),
        Chi("Χ"),
        Psi("Ψ"),
        Omega("Ω"),
        alpha("α"),
        beta("β"),
        gamma("γ"),
        delta("δ"),
        epsilon("ε"),
        zeta("ζ"),
        eta("η"),
        theta("θ"),
        iota("ι"),
        kappa("κ"),
        lambda("λ"),
        mu("μ"),
        nu("ν"),
        xi("ξ"),
        omicron("ο"),
        pi("π"),
        rho("ρ"),
        sigmaf("ς"),
        sigma("σ"),
        tau("τ"),
        upsilon("υ"),
        phi("φ"),
        chi("χ"),
        psi("ψ"),
        omega("ω"),
        thetasym("ϑ"),
        upsih("ϒ"),
        piv("ϖ"),
        ensp(" "),
        emsp(" "),
        thinsp(" "),
        ndash("–"),
        mdash("—"),
        lsquo("‘"),
        rsquo("’"),
        sbquo("‚"),
        ldquo("“"),
        rdquo("”"),
        bdquo("„"),
        dagger("†"),
        Dagger("‡"),
        bull("•"),
        hellip("…"),
        permil("‰"),
        prime("′"),
        Prime("″"),
        lsaquo("‹"),
        rsaquo("›"),
        oline("‾"),
        frasl("⁄"),
        euro("€"),
        image("ℑ"),
        weierp("℘"),
        real("ℜ"),
        trade("™"),
        alefsym("ℵ"),
        larr("←"),
        uarr("↑"),
        rarr("→"),
        darr("↓"),
        harr("↔"),
        crarr("↵"),
        lArr("⇐"),
        uArr("⇑"),
        rArr("⇒"),
        dArr("⇓"),
        hArr("⇔"),
        forall("∀"),
        part("∂"),
        exist("∃"),
        empty("∅"),
        nabla("∇"),
        isin("∈"),
        notin("∉"),
        ni("∋"),
        prod("∏"),
        sum("∑"),
        minus("−"),
        lowast("∗"),
        radic("√"),
        prop("∝"),
        infin("∞"),
        ang("∠"),
        and("∧"),
        or("∨"),
        cap("∩"),
        cup("∪"),
        // int("∫"),
        there4("∴"),
        sim("∼"),
        cong("≅"),
        asymp("≈"),
        ne("≠"),
        equiv("≡"),
        le("≤"),
        ge("≥"),
        sub("⊂"),
        sup("⊃"),
        nsub("⊄"),
        sube("⊆"),
        supe("⊇"),
        oplus("⊕"),
        otimes("⊗"),
        perp("⊥"),
        sdot("⋅"),
        // lceil(""),
        // rceil(""),
        // lfloor(""),
        // rfloor(""),
        lang("〈"),
        rang("〉"),
        loz("◊"),
        spades("♠"),
        clubs("♣"),
        hearts("♥"),
        diams("♦");

        String result;

        private CodeList(String result) {
            this.result = result;
        }

    }

    public static enum FilteredCodeList {
        quot(" "),
        amp(" "),
        apos(" "),
        lt(" "),
        gt(" "),
        nbsp(" "),
        iexcl(" "),
        cent(" "),
        pound(" "),
        curren(" "),
        yen(" "),
        brvbar(" "),
        sect(" "),
        uml(" "),
        copy(" "),
        ordf(" "),
        laquo(" "),
        not(" "),
        shy(" "),
        reg(" "),
        macr(" "),
        deg(" "),
        plusmn(" "),
        sup2(" "),
        sup3(" "),
        acute(" "),
        micro(" "),
        para(" "),
        middot(" "),
        cedil(" "),
        sup1(" "),
        ordm(" "),
        raquo(" "),
        frac14(" "),
        frac12(" "),
        frac34(" "),
        iquest(" "),
        Agrave("À"),
        Aacute("Á"),
        Acirc("Â"),
        Atilde("Ã"),
        Auml("Ä"),
        Aring("Å"),
        AElig("Æ"),
        Ccedil("Ç"),
        Egrave("È"),
        Eacute("É"),
        Ecirc("Ê"),
        Euml("Ë"),
        Igrave("Ì"),
        Iacute("Í"),
        Icirc("Î"),
        Iuml("Ï"),
        ETH(" "),
        Ntilde("Ñ"),
        Ograve("Ò"),
        Oacute("Ó"),
        Ocirc("Ô"),
        Otilde("Õ"),
        Ouml("Ö"),
        times("×"),
        Oslash("Ø"),
        Ugrave("Ù"),
        Uacute("Ú"),
        Ucirc("Û"),
        Uuml("Ü"),
        Yacute("Ý"),
        THORN(" "),
        szlig("ß"),
        agrave("à"),
        aacute("á"),
        acirc("â"),
        atilde("ã"),
        auml("ä"),
        aring("å"),
        aelig("æ"),
        ccedil("ç"),
        egrave("è"),
        eacute("é"),
        ecirc("ê"),
        euml("ë"),
        igrave("ì"),
        iacute("í"),
        icirc("î"),
        iuml("ï"),
        eth("ð"),
        ntilde("ñ"),
        ograve("ò"),
        oacute("ó"),
        ocirc("ô"),
        otilde("õ"),
        ouml("ö"),
        divide("÷"),
        oslash("ø"),
        ugrave("ù"),
        uacute("ú"),
        ucirc("û"),
        uuml("ü"),
        yacute("ý"),
        thorn(" "),
        yuml("ÿ"),
        OElig("Œ"),
        oelig("œ"),
        Scaron("Š"),
        scaron("š"),
        Yuml("Ÿ"),
        fnof(" "),
        circ(" "),
        tilde(" "),
        Alpha("Α"),
        Beta("Β"),
        Gamma("Γ"),
        Delta("Δ"),
        Epsilon("Ε"),
        Zeta("Ζ"),
        Eta("Η"),
        Theta("Θ"),
        Iota("Ι"),
        Kappa("Κ"),
        Lambda("Λ"),
        Mu("Μ"),
        Nu("Ν"),
        Xi("Ξ"),
        Omicron("Ο"),
        Pi("Π"),
        Rho("Ρ"),
        Sigma("Σ"),
        Tau("Τ"),
        Upsilon("Υ"),
        Phi("Φ"),
        Chi("Χ"),
        Psi("Ψ"),
        Omega("Ω"),
        alpha("α"),
        beta("β"),
        gamma("γ"),
        delta("δ"),
        epsilon("ε"),
        zeta("ζ"),
        eta("η"),
        theta("θ"),
        iota("ι"),
        kappa("κ"),
        lambda("λ"),
        mu("μ"),
        nu("ν"),
        xi("ξ"),
        omicron("ο"),
        pi("π"),
        rho("ρ"),
        sigmaf("ς"),
        sigma("σ"),
        tau("τ"),
        upsilon("υ"),
        phi("φ"),
        chi("χ"),
        psi("ψ"),
        omega("ω"),
        thetasym("ϑ"),
        upsih("ϒ"),
        piv("ϖ"),
        ensp(" "),
        emsp(" "),
        thinsp(" "),
        ndash("–"),
        mdash("—"),
        lsquo(" "),
        rsquo(" "),
        sbquo(" "),
        ldquo(" "),
        rdquo(" "),
        bdquo(" "),
        dagger(" "),
        Dagger(" "),
        bull(" "),
        hellip(" "),
        permil(" "),
        prime(" "),
        Prime(" "),
        lsaquo(" "),
        rsaquo(" "),
        oline(" "),
        frasl(" "),
        euro("€"),
        image(" "),
        weierp(" "),
        real(" "),
        trade(" "),
        alefsym(" "),
        larr(" "),
        uarr(" "),
        rarr(" "),
        darr(" "),
        harr(" "),
        crarr(" "),
        lArr(" "),
        uArr(" "),
        rArr(" "),
        dArr(" "),
        hArr(" "),
        forall(" "),
        part(" "),
        exist(" "),
        empty(" "),
        nabla(" "),
        isin(" "),
        notin(" "),
        ni(" "),
        prod(" "),
        sum(" "),
        minus("-"),
        lowast(" "),
        radic(" "),
        prop(" "),
        infin(" "),
        ang(" "),
        and(" "),
        or(" "),
        cap(" "),
        cup(" "),
        // int("∫"),
        there4(" "),
        sim(" "),
        cong(" "),
        asymp(" "),
        ne(" "),
        equiv(" "),
        le(" "),
        ge(" "),
        sub(" "),
        sup(" "),
        nsub(" "),
        sube(" "),
        supe(" "),
        oplus(" "),
        otimes(" "),
        perp(" "),
        sdot(" "),
        // lceil(""),
        // rceil(""),
        // lfloor(""),
        // rfloor(""),
        lang(" "),
        rang(" "),
        loz(" "),
        spades(" "),
        clubs(" "),
        hearts(" "),
        diams(" ");

        String result;

        private FilteredCodeList(String result) {
            this.result = result;
        }

    }

    private static final HashMap<String, String> htmlEntities;
    private static final HashMap<String, String> htmlFilteredEntities;
    static {
        htmlEntities = new HashMap<String, String>();
        for (CodeList code : CodeList.values()) {
            htmlEntities.put("&" + code.name() + ";", code.result);
        }
        htmlFilteredEntities = new HashMap<String, String>();
        for (FilteredCodeList code : FilteredCodeList.values()) {
            htmlFilteredEntities.put("&" + code.name() + ";", code.result);
        }
    }

    public static final String undecodeString(String source, Charset charset) {
        if (charset == null) {
            return source;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(source.getBytes(charset));
        Writer writer = new StringWriter();
        char[] buffer = new char[65536];
        try {
            Reader reader = null;
            reader = new BufferedReader(new InputStreamReader(in, charset));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException e1) {
            }
            try {
                writer.close();
            } catch (IOException e1) {
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
            try {
                writer.flush();
            } catch (IOException e) {
            }
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
        String out = writer.toString();
        return out;
    }

    public static final String unescapeQuotedPrintable(String source, Charset charset)
            throws UnsupportedEncodingException, MessagingException {
        ByteArrayInputStream in = new ByteArrayInputStream(source.getBytes(charset));
        InputStream decodedIn = MimeUtility.decode(in, "quoted-printable");
        Writer writer = new StringWriter();
        char[] buffer = new char[65536];
        try {
            Reader reader = null;
            if (charset != null) {
                reader = new BufferedReader(new InputStreamReader(decodedIn, charset));
            } else {
                reader = new BufferedReader(new InputStreamReader(decodedIn));
            }
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            if (decodedIn != null) {
                try {
                    decodedIn.close();
                } catch (IOException e1) {
                }
            }
            try {
                in.close();
            } catch (IOException e1) {
            }
            try {
                writer.close();
            } catch (IOException e1) {
            }
        } finally {
            try {
                decodedIn.close();
            } catch (IOException e) {
            }
            try {
                in.close();
            } catch (IOException e) {
            }
            try {
                writer.flush();
            } catch (IOException e) {
            }
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
        String out = writer.toString();
        return out;
    }

    public static final String undecodeString(InputStream source, Charset charset) {
        if (charset == null) {
            try {
                byte[] c = new byte[source.available()];
                source.read(c);
                return new String(c);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                return "";
            }
        }
        Writer writer = new StringWriter();
        char[] buffer = new char[65536];
        try {
            Reader reader = null;
            reader = new BufferedReader(new InputStreamReader(source, charset));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            try {
                source.close();
            } catch (IOException e1) {
            }
            try {
                writer.close();
            } catch (IOException e1) {
            }
        } finally {
            try {
                source.close();
            } catch (IOException e) {
            }
            try {
                writer.flush();
            } catch (IOException e) {
            }
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
        String out = writer.toString();
        return out;
    }

    public static final String unescapeQuotedPrintable(InputStream source,
            Charset charset) throws UnsupportedEncodingException, MessagingException {
        InputStream decodedIn = MimeUtility.decode(source, "quoted-printable");
        Writer writer = new StringWriter();
        char[] buffer = new char[65536];
        try {
            Reader reader = null;
            if (charset != null) {
                reader = new BufferedReader(new InputStreamReader(decodedIn, charset));
            } else {
                reader = new BufferedReader(new InputStreamReader(decodedIn));
            }
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            if (decodedIn != null) {
                try {
                    decodedIn.close();
                } catch (IOException e1) {
                }
            }
            try {
                source.close();
            } catch (IOException e1) {
            }
            try {
                writer.close();
            } catch (IOException e1) {
            }
        } finally {
            try {
                decodedIn.close();
            } catch (IOException e) {
            }
            try {
                source.close();
            } catch (IOException e) {
            }
            try {
                writer.flush();
            } catch (IOException e) {
            }
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
        String out = writer.toString();
        return out;
    }

    private static final String DELIM = "=?";

    public static final String unescapeHTML(String source, boolean filtered,
            boolean cleaned) {
        int i, j;
        if (source == null) {
            return "";
        }
        // check ?utf-8?Q?xxx? ?UTF-8?B?xxx?
        String newsource = source.trim();
        if (newsource.startsWith(DELIM)) {
            try {
                newsource = MimeUtility.decodeText(newsource);
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        StringBuilder builder = new StringBuilder();
        boolean continueLoop;
        int skip = 0;
        HashMap<String, String> filter = htmlEntities;
        if (filtered) {
            filter = htmlFilteredEntities;
        }
        do {
            continueLoop = false;
            i = newsource.indexOf("&", skip);
            if (i > -1) {
                j = newsource.indexOf(";", i + 1);
                if (j > i) {
                    String entityToLookFor = newsource.substring(i, j + 1);
                    String value = filter.get(entityToLookFor);
                    if (skip < i) {
                        builder.append(newsource.substring(skip, i));
                    }
                    if (value != null) {
                        builder.append(value);
                    }
                    skip = j + 1;
                    continueLoop = true;
                } else {
                    builder.append(newsource.substring(skip));
                }
            } else {
                builder.append(newsource.substring(skip));
            }
        } while (continueLoop);
        if (cleaned) {
            return cleanString(builder.toString());
        } else {
            return cleanSpaces(builder.toString());
        }
    }

    public static final String selectChevron(String source) {
        if (source == null) {
            return "";
        }
        String value = source;
        int pos = value.indexOf('<');
        if (pos >= 0) {
            int pos2 = value.indexOf('>');
            if (pos2 > 0) {
                value = value.substring(pos + 1, pos2);
            }
        }
        return value;
    }

    private static final Pattern removeChevron = Pattern.compile("[\\<\\>]");

    public static final String removeChevron(String source) {
        if (source == null) {
            return "";
        }
        return removeChevron.matcher(source).replaceAll("");
    }

    public static final String cleanSpaces(String source) {
        if (source == null) {
            return "";
        }
        String result = source.trim().replaceAll(" +", " ");
        /*
         * int len = result.length(); while (true) { result = result.replace(
         * "  ", " "); if (result.length() == len) break; len = result.length();
         * }
         */
        return result;
    }

    // remove all punctuation and number
    private static final Pattern cleanString1 = Pattern.compile(
            "[\\s\\d\\«\\»\\.\\,\\?\\;\\.\\:\\!\\\"\\'\\(\\[\\`\\)\\]\\=\\{\\}\\<\\>\\~\\’\\°\\‟\\/\\_]");
    private static final Pattern cleanString2 = Pattern.compile("[\\.&&[^\\w]]");
    // remove all at most 2 letters
    private static final Pattern cleanString3 = Pattern.compile("\\b\\S{1,2}\\b");
    // remove all lower case at most 3 letters
    private static final Pattern cleanString4 = Pattern.compile("\\b[a-z]{1,3}\\b");
    // remove all special code
    private static final Pattern cleanString5 =
            Pattern.compile("\\b[\\\\\\•\\+\\-\\=\\*\\%\\€\\$\\§\\²\\-\\/\\&]\\b");
    private static final Pattern cleanString6 =
            Pattern.compile("\\s[\\\\\\•\\+\\-\\=\\*\\%\\€\\$\\§\\²\\-\\/\\&]\\s");

    public static final String cleanString(String source) {
        if (source == null || source.length() < 1) {
            return "";
        }
        String result = source;
        result = cleanString1.matcher(result).replaceAll(" ");
        result = cleanString2.matcher(result).replaceAll(" ");
        // remove all at most 2 letters
        result = cleanString3.matcher(result).replaceAll(" ");
        // remove all lower case at most 3 letters
        result = cleanString4.matcher(result).replaceAll("");
        // remove all special code
        result = cleanString5.matcher(result).replaceAll("");
        result = cleanString6.matcher(result).replaceAll(" ");
        return cleanSpaces(result);
    }

    private static final Pattern cleanPartiallyString1 = Pattern.compile(
            "[\\s\\«\\»\\,\\?\\;\\:\\!\\\"\\'\\(\\[\\`\\)\\]\\=\\{\\}\\<\\>\\~\\’\\°\\‟\\/\\_]");
    // remove all special code
    private static final Pattern cleanPartiallyString2 =
            Pattern.compile("\\b[\\\\\\•\\+\\=\\*\\%\\€\\$\\§\\²\\/\\&]\\b");

    public static final String cleanPartiallyString(String source) {
        if (source == null || source.length() < 1) {
            return "";
        }
        String result = source;
        result = cleanPartiallyString1.matcher(result).replaceAll(" ");
        // remove all special code
        result = cleanPartiallyString2.matcher(result).replaceAll("");
        result = cleanString6.matcher(result).replaceAll(" ");
        return cleanSpaces(result);
    }

    public static final String toFileName(String source) {
        if (source == null || source.length() < 1) {
            return "";
        }
        return cleanPartiallyString(unescapeHTML(source, true, false)).replaceAll(" ",
                "_");
    }

    public static void main(String args[]) throws Exception {
        String test = "&copy; 2007  R&eacute;el Test &lt;www.test.com&gt; test@test.com";
        System.out.println(test + "\n-->\n" + unescapeHTML(test, true, false));
        System.out.println(test + "\n-->\n" + unescapeHTML(test, true, true));
        System.out.println(test + "\n-->\n" + unescapeHTML(test, false, false));

        /*
         * output ((Windows DOS Shell): &copy; 2007 R&eacute;al Gagnon
         * &lt;www.rgagnon.com&gt; --> © 2007 Réal Gagnon <www.rgagnon.com>
         */
    }

    /**
     * Remove invalid XML character (but still UTF-8 characters)
     * 
     * @param s
     * @return the cleaned string
     */
    public static final String removeInvalidXMLCharacters(String s) {
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        int codePoint; // Used to reference the current character.
        // String ss = "\ud801\udc00"; // This is actualy one unicode character,
        // represented by two code units!!!.
        // System.out.println(ss.codePointCount(0, ss.length()));// See: 1
        int i = 0;
        int start = 0, stop = 0;
        while (i < s.length()) {
            codePoint = s.codePointAt(i); // This is the unicode code of the
                                          // character.
            if ((codePoint == 0x9) || // Consider testing larger ranges first to
                                      // improve speed.
                    (codePoint == 0xA) ||
                    (codePoint == 0xD) ||
                    ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                    ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||
                    ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF))) {
                // out.append(Character.toChars(codePoint));
                i += Character.charCount(codePoint); // Increment with the
                                                     // number of code
                                                     // units(java chars) needed
                                                     // to represent a
                // Unicode char.
            } else {
                out.append(s.substring(start, i));
                i += Character.charCount(codePoint); // Increment with the
                                                     // number of code
                                                     // units(java chars) needed
                                                     // to represent a
                // Unicode char.
                start = i;
            }
            stop = i;
        }
        if (start == 0) {
            return s;
        } else if (stop >= s.length()) {
            out.append(s.substring(start, stop));
        }
        return out.toString();
    }
}
