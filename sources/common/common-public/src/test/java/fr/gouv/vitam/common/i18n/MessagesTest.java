package fr.gouv.vitam.common.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import org.junit.Test;

public class MessagesTest {

    @Test
    public void testWithoutLocale() {
        final Messages messages = new Messages("messages");
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
        final String mesg3 = messages.getString("HelloWorld.OK", "éèàùôî");
        assertEquals("OK avec des accentués et d'apostrophe éèàùôî !", mesg3);
        final String mesg4 = messages.getString("HelloWorld.FATAL");
        assertEquals("!HelloWorld.FATAL!", mesg4);
        final String mesg5 = messages.getString("HelloWorld.FATAL", "value1", "value2");
        assertEquals("!HelloWorld.FATAL! value1 value2", mesg5);
        assertNotNull(messages.getAllMessages());
    }

    @Test
    public void testWithEmptyLocale() {
        final Messages messages = new Messages("messages", null);
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
        final String mesg3 = messages.getString("HelloWorld.OK", "éèàùôî");
        assertEquals("OK avec des accentués et d'apostrophe éèàùôî !", mesg3);
        assertNotNull(messages.getAllMessages());
    }

    @Test
    public void testWithLocale() {
        final Messages messages = new Messages("messages", Locale.FRENCH);
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
        final String mesg3 = messages.getString("HelloWorld.OK", "éèàùôî");
        assertEquals("OK avec des accentués et d'apostrophe éèàùôî !", mesg3);
        assertNotNull(messages.getAllMessages());
    }

}
