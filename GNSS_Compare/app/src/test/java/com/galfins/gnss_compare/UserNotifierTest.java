package com.galfins.gnss_compare;

import android.support.design.widget.Snackbar;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.when;

/**
 * Test for the user MainActivity's implementation of User Notifier
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class UserNotifierTest {

    private class TestUserNotifier extends UserNotifier{

        private String lastMessage = null;
        private int lastDuration;
        private String lastId = null;
        public static final int DURATION_LONG = 3500;
        public static final int DURATION_SHORT = 2000;

        @Override
        protected void displayMessage(String text, int duration, String id) {
            lastMessage = text;
            lastDuration = duration;
            lastId = id;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public int getLastDuration() {
            return lastDuration;
        }

        public String getLastId() {
            return lastId;
        }
    }

    /**
     * Testing creation from description
     * @throws Exception
     */
    @Test
    public void UserNotifierSnackbarTimeConversionTest() throws Exception{
        TestUserNotifier notifier = new TestUserNotifier();
        final String []messageText = {"test", "test2", "testtest\ntest"};
        final int[] duration = {1000, Snackbar.LENGTH_SHORT, Snackbar.LENGTH_LONG};
        final String []id = {"A", "B", "C"};

        for (int i=0; i<3; i++){

            notifier.notifyUser(messageText[i], duration[i], id[i]);

            assertEquals(messageText[i], notifier.getLastMessage());

            if(duration[i] == Snackbar.LENGTH_SHORT)
                assertEquals(TestUserNotifier.DURATION_SHORT, notifier.getLastDuration());
            if(duration[i] == Snackbar.LENGTH_LONG)
                assertEquals(TestUserNotifier.DURATION_LONG, notifier.getLastDuration());

            assertEquals(id[i], notifier.getLastId());
        }
    }
}
