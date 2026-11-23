package com.csm.mushafi.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Mp3CheckTest {

    @Test
    public void recognisesId3AndFrameSync() {
        assertTrue(Mp3Check.looksLikeMp3(new byte[]{'I', 'D', '3', 4}));
        assertTrue(Mp3Check.looksLikeMp3(new byte[]{(byte) 0xFF, (byte) 0xFB, 0x90, 0x00}));
    }

    @Test
    public void rejectsHtmlAndShortInput() {
        assertFalse(Mp3Check.looksLikeMp3("<html>".getBytes()));
        assertFalse(Mp3Check.looksLikeMp3(new byte[]{'I'}));
        assertFalse(Mp3Check.looksLikeMp3(null));
    }

    @Test
    public void rangeHeaderOnlyForPartialFiles() {
        assertNull(Mp3Check.rangeHeader(0));
        assertEquals("bytes=1024-", Mp3Check.rangeHeader(1024));
    }
}
