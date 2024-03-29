/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ntlab.graffiti.common.helpers;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.ntlab.graffiti.common.views.FuriganaView;

import java.util.Locale;

/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code, and exposes simpler
 * methods.
 * @author a-hongo
 */
public final class SnackbarHelper {
    private static final int BACKGROUND_COLOR = 0xbf323232;
    private Snackbar messageSnackbar;
    private enum DismissBehavior { HIDE, SHOW, FINISH };
    private int maxLines = 2;
    private String lastMessage = "";
    private View snackbarView;
    private Locale locale = Locale.getDefault();

    public boolean isShowing() {
        return messageSnackbar != null;
    }

    /** Shows a snackbar with a given message. */
    public void showMessage(Activity activity, String message) {
        if (!message.isEmpty() && (!isShowing() || !lastMessage.equals(message))) {
            lastMessage = message;
            show(activity, message, DismissBehavior.HIDE, 0);
        }
    }

    /** Shows a snackbar with a given message, and a dismiss button. */
    public void showMessageWithDismiss(Activity activity, String message) {
        show(activity, message, DismissBehavior.SHOW, 0);
    }

    /** Shows a snackbar with a given message, and a dismiss button. */
    public void showMessageWithDuration(Activity activity, String message, int duration) {
        show(activity, message, DismissBehavior.HIDE, duration);
    }

    /**
     * Shows a snackbar with a given error message. When dismissed, will finish the activity. Useful
     * for notifying errors, where no further interaction with the activity is possible.
     */
    public void showError(Activity activity, String errorMessage) {
        show(activity, errorMessage, DismissBehavior.FINISH, 0);
    }

    /**
     * Hides the currently showing snackbar, if there is one. Safe to call from any thread. Safe to
     * call even if snackbar is not shown.
     */
    public void hide(Activity activity) {
        if (!isShowing()) {
            return;
        }
        lastMessage = "";
        Snackbar messageSnackbarToHide = messageSnackbar;
        messageSnackbar = null;
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        messageSnackbarToHide.dismiss();
                    }
                });
    }

    public void setMaxLines(int lines) {
        maxLines = lines;
    }

    /**
     * Sets the view that will be used to find a suitable parent view to hold the Snackbar view.
     *
     * <p>To use the root layout ({@link android.R.id.content}), pass in {@code null}.
     *
     * @param snackbarView the view to pass to {@link
     *     com.google.android.material.snackbar.Snackbar#make(View, CharSequence, int)} (View, int, int)} which will be used to find a
     *     suitable parent, which is a {@link androidx.coordinatorlayout.widget.CoordinatorLayout}, or
     *     the window decor's content view, whichever comes first.
     */
    public void setParentView(View snackbarView) {
        this.snackbarView = snackbarView;
    }

    private void show(
            final Activity activity, final String message, final DismissBehavior dismissBehavior, int duration) {
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        messageSnackbar =
                                Snackbar.make(
                                        snackbarView == null
                                                ? activity.findViewById(android.R.id.content)
                                                : snackbarView,
                                        message,
                                        Snackbar.LENGTH_INDEFINITE);
                        messageSnackbar.getView().setBackgroundColor(BACKGROUND_COLOR);
                        if (dismissBehavior != DismissBehavior.HIDE) {
                            messageSnackbar.setAction(
                                    "Dismiss",
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            messageSnackbar.dismiss();
                                        }
                                    });
                            if (dismissBehavior == DismissBehavior.FINISH) {
                                messageSnackbar.addCallback(
                                        new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                            @Override
                                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                                super.onDismissed(transientBottomBar, event);
                                                activity.finish();
                                            }
                                        });
                            }
                        }

                        if (locale.equals(Locale.JAPAN)) {
                            // Get the Snackbar's layout view
                            Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) messageSnackbar.getView();
                            // Hide the text
                            TextView textView = layout.findViewById(com.google.android.material.R.id.snackbar_text);
                            textView.setVisibility(View.INVISIBLE);
                            FuriganaView furiganaView = new FuriganaView(activity);
                            furiganaView.setText(message);
                            furiganaView.setMaxLines(maxLines);
                            // Add the view to the Snackbar's layout
                            layout.addView(furiganaView, 0);
                        } else {
                            ((TextView)
                                    messageSnackbar
                                            .getView()
                                            .findViewById(com.google.android.material.R.id.snackbar_text))
                                    .setMaxLines(maxLines);
                        }
                        if (duration > 0) {
                            messageSnackbar.setDuration(duration);
                        }
                        messageSnackbar.show();
                    }
                });
    }
}
