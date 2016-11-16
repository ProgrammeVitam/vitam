package fr.gouv.vitam.common.model;

import fr.gouv.vitam.common.thread.VitamThreadFactory;
import org.junit.Assert;
import org.junit.Test;


public class VitamSessionTest {

    // Write access from another thread as the owning thread should produce an exception.

    @Test(expected = IllegalStateException.class)
    public void testSetRequestIdInDifferentThread() throws Exception {
        final VitamSession session = new VitamSession(new VitamThreadFactory.VitamThread(null, 0));
        session.setRequestId("toto");
    }

    @Test(expected = IllegalStateException.class)
    public void testMutateFromInDifferentThread() throws Exception {
        final VitamSession session1 = new VitamSession(new VitamThreadFactory.VitamThread(null, 0));
        final VitamSession session2 = new VitamSession(new VitamThreadFactory.VitamThread(null, 0));
        session1.mutateFrom(session2);
    }

    @Test(expected = IllegalStateException.class)
    public void testEraseInDifferentThread() throws Exception {
        final VitamSession session = new VitamSession(new VitamThreadFactory.VitamThread(null, 0));
        session.erase();
    }

    // Read access not, though.

    @Test
    public void testGetRequestIdInDifferentThread() throws Exception {
        final VitamSession session = new VitamSession(new VitamThreadFactory.VitamThread(null, 0));
        Assert.assertNull(session.getRequestId());
    }


}