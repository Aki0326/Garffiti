// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.ntlab.graffiti.graffiti;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;


import org.ntlab.graffiti.BuildConfig;
import org.ntlab.graffiti.R;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = AboutActivity.class.getSimpleName();

    @SuppressWarnings("FieldCanBeLocal")
    private final int SPLASH_DISPLAY_LENGTH = 5000;

    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(AboutActivity.this, ModeSelectActivity.class);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        findViewById(R.id.container_privacy_policy).setOnClickListener(this);
        ((TextView) findViewById(R.id.about_version)).setText("version" + BuildConfig.VERSION_NAME);
        startModeSelectDelayed();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startModeSelectDelayed();
    }
    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.container_privacy_policy:
                openUrl(R.string.privacy_policy_url);
                break;
        }
    }

    private void openUrl(int urlRes) {
        Intent privacyIntent = new Intent(Intent.ACTION_VIEW);
        privacyIntent.setData(Uri.parse(getString(urlRes)));
        startActivity(privacyIntent);
    }

    private void startModeSelectDelayed() {
        //hold here for 5 seconds
        handler.postDelayed(runnable, SPLASH_DISPLAY_LENGTH);
    }
}