package fr.gouv.vitam.common.i18n;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import fr.gouv.vitam.common.i18n.Messages;

public class MessagesTest {

    @Test
    public void testWithoutLocale() {
        final Messages messages = new Messages("messages");
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
        final String mesg3 = messages.getString("HelloWorld.OK");
        assertEquals("!HelloWorld.OK!", mesg3);
        final String mesg4 = messages.getString("HelloWorld.OK", "value1", "value2");
        assertEquals("!HelloWorld.OK! value1 value2", mesg4);
    }

    @Test
    public void testWithEmptyLocale() {
        final Messages messages = new Messages("messages", null);
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
    }

    @Test
    public void testWithLocale() {
        final Messages messages = new Messages("messages", Locale.FRENCH);
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
    }

}
