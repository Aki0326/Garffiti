package org.ntlab.graffiti.common.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ntlab.graffiti.graffiti.ModeSelectActivity;

/**
 * Helper to receive the broadcast intent.
 * @author a-hongo
 */
public class ReceiverHelper extends BroadcastReceiver {

    /**
     * Broadcast Intentを受信した場合の処理を行う
     * Receive the broadcast intent.
     * @param context the application's context.
     * @param intent the application's next intent.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Context を使って、別の画面を起動する
        Intent nextIntent = new Intent(context, ModeSelectActivity.class);
        nextIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        context.startActivity(nextIntent);
    }

}
