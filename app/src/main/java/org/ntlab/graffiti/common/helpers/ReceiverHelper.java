package org.ntlab.graffiti.common.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ntlab.graffiti.graffiti.ModeSelectActivity;

public class ReceiverHelper extends BroadcastReceiver {

    // BroadcastIntentを受信した場合の処理
    @Override
    public void onReceive(Context context, Intent intent) {
        // Context を使って、別の画面を起動する
        Intent nextIntent = new Intent(context, ModeSelectActivity.class);
        nextIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        context.startActivity(nextIntent);

    }
}