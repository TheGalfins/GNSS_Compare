package com.galfins.gnss_compare;

import android.support.design.widget.Snackbar;

/**
 * Created by pc1 on 30-Dec-18.
 * This class is for:
 */
public abstract class UserNotifier {

    /**
     * This method should implement a way to display the text to the user
     * @param text Text do be displayed to be user
     * @param duration duration of the message (ms)
     * @param id id of the message (for advanced handling)
     */
    protected abstract void displayMessage(String text, int duration, String id);

    public void notifyUser(String text, int duration, String id){
        if(duration == Snackbar.LENGTH_SHORT)
            duration = 2000;
        else if (duration == Snackbar.LENGTH_LONG)
            duration = 3500;

        displayMessage(text, duration, id);
    }
}
