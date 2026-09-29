package com.alpine.app.api.file;

import android.content.Intent;
import android.net.Uri;

import com.alpine.app.api.file.FileReceiverActivity;
import com.alpine.shared.data.IntentUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FileReceiverActivityTest {

    @Test
    public void testIsSharedTextAnUrl() {
        List<String> validUrls = new ArrayList<>();
        validUrls.add("http://example.com");
        validUrls.add("https://example.com");
        validUrls.add("https://example.com/path/parameter=foo");
        validUrls.add("magnet:?xt=urn:btih:d540fc48eb12f2833163eed6421d449dd8f1ce1f&dn=Ubuntu+desktop+19.04+%2864bit%29&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.ccc.de%3A80");
        for (String url : validUrls) {
            Assert.assertTrue(FileReceiverActivity.isSharedTextAnUrl(url));
        }

        List<String> invalidUrls = new ArrayList<>();
        invalidUrls.add("a test with example.com");
        invalidUrls.add("");
        invalidUrls.add(null);
        for (String url : invalidUrls) {
            Assert.assertFalse(FileReceiverActivity.isSharedTextAnUrl(url));
        }
    }

    @Test
    public void testReceivedFileNameMustBeABasename() {
        Assert.assertTrue(FileReceiverActivity.isSafeAttachmentFileName("photo 01.png"));
        Assert.assertTrue(FileReceiverActivity.isSafeAttachmentFileName(".hidden"));

        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName(null));
        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName(""));
        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName("."));
        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName(".."));
        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName("../escape.txt"));
        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName("subdir/file.txt"));
        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName("subdir\\file.txt"));
        Assert.assertFalse(FileReceiverActivity.isSafeAttachmentFileName("bad\0name"));
    }

    @Test
    public void testMalformedParcelableExtraFailsClosed() {
        Intent intent = new Intent();
        Uri expected = Uri.parse("content://example.test/item/1");
        intent.putExtra(Intent.EXTRA_STREAM, expected);
        Assert.assertEquals(expected,
            IntentUtils.getParcelableExtraIfSet(intent, Intent.EXTRA_STREAM, Uri.class));

        intent.putExtra(Intent.EXTRA_STREAM, "not-a-uri");
        Assert.assertNull(IntentUtils.getParcelableExtraIfSet(intent, Intent.EXTRA_STREAM, Uri.class));
    }

}
