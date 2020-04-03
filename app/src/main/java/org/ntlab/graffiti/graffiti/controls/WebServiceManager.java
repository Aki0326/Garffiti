package org.ntlab.graffiti.graffiti.controls;

import android.util.Log;

import com.google.common.base.Preconditions;

import org.ntlab.graffiti.entities.CloudAnchor;
import org.ntlab.graffiti.entities.Room;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class WebServiceManager {
    private static final String TAG = WebServiceManager.class.getSimpleName();

    private Map<String, Room> rooms = new HashMap<>();
    private String roomCode = null;
    private RoomUpdateListener roomUpdateListener;

    /** Listener for a new cloud anchor. */
    public interface RoomUpdateListener {

        /** Invoked when a new cloud anchor ID is available. */
        void onNewCloudAnchor(String cloudAnchorId, CloudAnchor cloudAnchor);

        /** Invoked when a update cloud anchor ID is available. */
        void onUpdateCloudAnchor(String cloudAnchorId, CloudAnchor cloudAnchor);
    }

//    private static final String ROOT_LAST_ROOM_CODE = "last_room_code";

    // Some common keys and values used when writing to the Firebase Database.
    private static final String DISPLAY_NAME_VALUE = "Android EAP Sample";

    private static final Timestamp LATEST = null;

    private final Retrofit retrofit;
    private final RoomsAPI roomsAPI;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    /**
     * Default constructor for the WebServiceManager.
     */
    public WebServiceManager() {
        //retrofitの処理
        retrofit = new Retrofit.Builder()
//                .baseUrl("http://192.168.2.109:8080/garffitiserver/")
                .baseUrl("http://localhost:8080/garffitiserver/")
//                .baseUrl("http://nitta-lab-www.is.konan-u.ac.jp:8080/garffitiserver/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        //interfaceから実装を取得
        roomsAPI = retrofit.create(RoomsAPI.class);

        if (retrofit != null) {
        } else {
            Log.d(TAG, "Could not connect to RoomsWebService!");
        }
    }

    public void createRoom(Long newRoomCode, RoomUpdateListener roomUpdateListener) {
        roomCode = String.valueOf(newRoomCode);
        this.roomUpdateListener = roomUpdateListener;
        Preconditions.checkNotNull(retrofit, "Retrofit was null");
//        clearRoomListener();
        Call<Void> createRoomCall = roomsAPI.createRoom(this.roomCode);
        //サーバからのレスポンス
        createRoomCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    rooms.put(WebServiceManager.this.roomCode, new Room(WebServiceManager.this.roomCode));
                    Log.d(TAG, "Success No." + WebServiceManager.this.roomCode + " createRoom!");
                    if(scheduledThreadPoolExecutor == null) {
                        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
                        TimerTask roomWactherTask = new RoomWatcherTask();
                        scheduledThreadPoolExecutor.scheduleWithFixedDelay(roomWactherTask, 0, 250, TimeUnit.MILLISECONDS);
                    }
                } else {
                    try {
                        Log.d(TAG, response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //onFailureでキャッチできないエラーの処理
                    Log.d(TAG, "Error createRoom connection.");
                }
            }

            //失敗時
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
                Log.d(TAG, "Failed createRoom." + t.getMessage());
            }
        });

    }

    private class RoomWatcherTask extends TimerTask {
        @Override
        public void run() {
//            Log.d(TAG, "Timer run!");
            final Call<Map<String, Timestamp>> getRoomCall = roomsAPI.getRoom(WebServiceManager.this.roomCode);
            Response<Map<String, Timestamp>> getRoomResponse;
            try {
                getRoomResponse = getRoomCall.execute();
                if (getRoomResponse.isSuccessful()) {
//                    Log.d(TAG, "Success getRoom connection.");
                    Room room = rooms.get(WebServiceManager.this.roomCode);
                    if (room != null) {
                        Map<String, Timestamp> anchorIdToTimestamp = getRoomResponse.body();
                        for (String anchorId : anchorIdToTimestamp.keySet()) {
                            CloudAnchor cashedCloudAnchor = room.getCloudAnchor(anchorId);
                            final Call<CloudAnchor> getCloudAnchorCall = roomsAPI.getCloudAnchor(WebServiceManager.this.roomCode, anchorId);
                            if (cashedCloudAnchor == null) {
                                room.putCloudAnchor(anchorId, new CloudAnchor(DISPLAY_NAME_VALUE, LATEST));
                                getCloudAnchorCall.enqueue(new Callback<CloudAnchor>() {
                                    @Override
                                    public void onResponse(Call<CloudAnchor> call, Response<CloudAnchor> response) {
                                        if (response.isSuccessful()) {
                                            CloudAnchor cloudAnchor = response.body();
                                            Log.d(TAG, "Success getCloudAnchor connection " + anchorId + " .");
                                            room.putCloudAnchor(anchorId, cloudAnchor);
                                            roomUpdateListener.onNewCloudAnchor(anchorId, cloudAnchor);
                                        } else {
                                            room.removeCloudAnchor(anchorId);
                                            try {
                                                System.out.println(response.errorBody().string());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            //onFailureでキャッチできないエラーの処理
                                            Log.d(TAG, "Error getCloudAnchor connection.");
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<CloudAnchor> call, Throwable t) {
                                        room.removeCloudAnchor(anchorId);
                                        t.printStackTrace();
                                        Log.d(TAG, "Failed getCloudAnchor.");
                                    }
                                });
                            } else if (cashedCloudAnchor.getUpdateTimestamp() != LATEST && cashedCloudAnchor.getUpdateTimestamp().before(anchorIdToTimestamp.get(anchorId))) {
                                final Timestamp backupUpdateTimestamp = cashedCloudAnchor.getUpdateTimestamp();
                                cashedCloudAnchor.setUpdateTimestamp(LATEST);
                                getCloudAnchorCall.enqueue(new Callback<CloudAnchor>() {
                                    @Override
                                    public void onResponse(Call<CloudAnchor> call, Response<CloudAnchor> response) {
                                        if (response.isSuccessful()) {
                                            CloudAnchor cloudAnchor = response.body();
                                            Log.d(TAG, "Success getCloudAnchor connection " + anchorId + " .");
                                            room.putCloudAnchor(anchorId, cloudAnchor);
                                            roomUpdateListener.onUpdateCloudAnchor(anchorId, cloudAnchor);
                                        } else {
                                            cashedCloudAnchor.setUpdateTimestamp(backupUpdateTimestamp);
                                            try {
                                                System.out.println(response.errorBody().string());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            //onFailureでキャッチできないエラーの処理
                                            Log.d(TAG, "Error getCloudAnchor connection.");
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<CloudAnchor> call, Throwable t) {
                                        cashedCloudAnchor.setUpdateTimestamp(backupUpdateTimestamp);
                                        t.printStackTrace();
                                        Log.d(TAG, "Failed getCloudAnchor.");
                                    }
                                });
                            }
                        }
                    }
                } else {
                    // onFailure
                    try {
                        System.out.println(getRoomResponse.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //onFailureでキャッチできないエラーの処理
                    Log.d(TAG, "Error getRoom connection.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Failed getRoom.");
            }
//            Log.d(TAG, "Timer run end.");
        }
    }

    /** Stores the given anchor ID in the given room code. */
    public synchronized void storeAnchorIdInRoom(String cloudAnchorId) {
        Preconditions.checkNotNull(retrofit, "Retrofit was null");
        Call<Void> updateCloudAnchorCall = roomsAPI.updateCloudAnchor(roomCode, cloudAnchorId, DISPLAY_NAME_VALUE);
        //サーバからのレスポンス
        updateCloudAnchorCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Room room = rooms.get(roomCode);
                    room.putCloudAnchor(cloudAnchorId, new CloudAnchor(DISPLAY_NAME_VALUE, LATEST));
                    Log.d(TAG, "Success No." + roomCode + " update!");
                } else {
                    //onFailureでキャッチできないエラーの処理
                    Log.d(TAG, "Error updateCloudAnchor connection.");
                    try {
                        Log.d(TAG, response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //失敗時
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
                Log.d(TAG, "Failed updateCloudAnchor." + t.getMessage());
            }
        });
    }

    /** Stores the given polygon in the given room code, cloudAnchorId. */
    public synchronized void storePolygonInRoom(String cloudAnchorId, float[] polygon) {
        Preconditions.checkNotNull(retrofit, "Retrofit was null");

        Call<Void> updatePlaneCall = roomsAPI.updatePlane(this.roomCode, cloudAnchorId, polygon);

        //サーバからのレスポンス
        updatePlaneCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Room room = rooms.get(WebServiceManager.this.roomCode);
//                    room.getCloudAnchor(cloudAnchorId).setPlane(polygon);
//                    Log.d(TAG, "Success No." + roomCode + " updatePlane!");
                } else {
                    //onFailureでキャッチできないエラーの処理
                    Log.d(TAG, "Error updatePlane connection.");
                    try {
                        Log.d(TAG, response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //失敗時
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
                Log.d(TAG, "Failed updatePlane." + t.getMessage());
            }
        });
    }

    /** Stores the given stroke in the given room code, cloudAnchorId. */
    public synchronized void storeStrokeInRoom(String cloudAnchorId, float texX, float texY) {
        Preconditions.checkNotNull(retrofit, "Retrofit was null");
        Call<Void> strokeCall = roomsAPI.stroke(roomCode, cloudAnchorId, texX, texY);

        //サーバからのレスポンス
        strokeCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
//                    Log.d(TAG, "Success No." + roomCode + " updateStroke!");
                } else {
                    //onFailureでキャッチできないエラーの処理
                    Log.d(TAG, "Error updateStroke connection.");
                    try {
                        Log.d(TAG, response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //失敗時
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
                Log.d(TAG, "Failed updateStroke." + t.getMessage());
            }
        });
    }

    /**
     * Resets the current room listener registered using {@link #createRoom(Long, RoomUpdateListener)}.
     */
    public void clearRoomListener() {
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
        }
        if (roomUpdateListener != null) {
            roomUpdateListener = null;
        }
    }
}
