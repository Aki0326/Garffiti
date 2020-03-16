package org.ntlab.graffiti.graffiti;

import android.graphics.PointF;
import android.util.Log;

import com.google.common.base.Preconditions;

import org.ntlab.graffiti.entities.CloudAnchor;
import org.ntlab.graffiti.entities.PointTex2D;
import org.ntlab.graffiti.entities.Room;
import org.ntlab.graffiti.resources.RoomsService;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class ServiceManager {
//    private static final String TAG = ColoringBattleActivity.class.getSimpleName() + "." + ServiceManager.class.getSimpleName();
    private static final String TAG = ColoringBattleActivity.class.getSimpleName() + "Shared";

    private Map<String, Room> rooms = new HashMap<>();

    /** Listener for a new room code. */
    interface RoomCodeListener {

        /** Invoked when a new room code is available from Firebase. */
        void onNewRoomCode(Long newRoomCode);

        /** Invoked if a Firebase Database Error happened while fetching the room code. */
//        void onError(DatabaseError error);
    }

    /** Listener for a new cloud anchor ID. */
    interface CloudAnchorIdListener {

        /** Invoked when a new cloud anchor ID is available. */
        void onNewCloudAnchorId(String cloudAnchorId, CloudAnchor cloudAnchor, PointF coordinate);
    }

    // Names of the nodes used in the Firebase Database
    private static final String ROOT_FIREBASE_HOTSPOTS = "hotspot_list";
//    private static final String ROOT_LAST_ROOM_CODE = "last_room_code";

    // Some common keys and values used when writing to the Firebase Database.
    private static final String KEY_ANCHOR_ID = "hosted_anchor_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_TIMESTAMP = "updated_at_timestamp";
    private static final String DRAW = "draw";
    private static final String DISPLAY_NAME_VALUE = "Android EAP Sample";
    private static final String X = "x";
    private static final String Y = "y";

    private final Retrofit retrofit;
    private final RoomsService roomsService;

//    private final FirebaseApp app;
//    private final DatabaseReference hotspotListRef;
    //    private final DatabaseReference roomCodeRef;
//    private DatabaseReference currentRoomRef = null;
//    private ChildEventListener currentRoomListener = null;
//    private ValueEventListener currentRoomListener = null;

//    private DatabaseReference currentIndexRef = null;
//    private ValueEventListener currentIndexListener = null;

//    private DatabaseReference currentDrawRef = null;
//    private ChildEventListener currentDrawListener = null;

//    private DatabaseReference currentCloudAnchorIdRef = null;
//    private ValueEventListener currentCloudAnchorIdListener = null;

    private Set<String> cloudAnchorIds = new HashSet<>();
    private Map<String, Set<PointF>> coordinates = new HashMap<>();

    private Timer timer;

    /**
     * Default constructor for the ServiceManager.
     */
    ServiceManager(/*Context context*/) {
        //retrofitの処理
        retrofit = new Retrofit.Builder()
//                .baseUrl("http://192.168.2.109:8080/garffitiserver/")
                .baseUrl("http://localhost:8080/garffitiserver/")
//                .baseUrl("http://nitta-lab-www.is.konan-u.ac.jp/garffitiserver/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        //interfaceから実装を取得
        roomsService = retrofit.create(RoomsService.class);

//        app = FirebaseApp.initializeApp(context);
        if (retrofit != null) {
//            DatabaseReference rootRef = FirebaseDatabase.getInstance(app).getReference();
//            hotspotListRef = rootRef.child(ROOT_FIREBASE_HOTSPOTS);
//            roomCodeRef = rootRef.child(ROOT_LAST_ROOM_CODE);

//            DatabaseReference.goOnline();
        } else {
            Log.d(TAG, "Could not connect to Rooms Service!");
//            hotspotListRef = null;
//            roomCodeRef = null;
        }
    }

    /**
     * Gets a new room code from the Firebase Database. Invokes the listener method when a new room
     * code is available.
     */
//    void getNewRoomCode(RoomCodeListener listener) {
//        Preconditions.checkNotNull(app, "Firebase App was null");
//        roomCodeRef.runTransaction(
//                new Transaction.Handler() {
//                    @Override
//                    public Transaction.Result doTransaction(MutableData currentData) {
//                        Long nextCode = Long.valueOf(1);
//                        Object currVal = currentData.getValue();
//                        if (currVal != null) {
//                            Long lastCode = Long.valueOf(currVal.toString());
//                            nextCode = lastCode + 1;
//                        }
//                        currentData.setValue(nextCode);
//                        return Transaction.success(currentData);
//                    }
//
//                    @Override
//                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
//                        if (!committed) {
//                            listener.onError(error);
//                            return;
//                        }
//                        Long roomCode = currentData.getValue(Long.class);
//                        listener.onNewRoomCode(roomCode);
//                    }
//                });
//    }

    /** Stores the given anchor ID in the given room code. */
    synchronized void storeAnchorIdInRoom(Long roomCode, String cloudAnchorId) {
        Preconditions.checkNotNull(retrofit, "Retrofit was null");
        String roomId = String.valueOf(roomCode);
        cloudAnchorIds.add(cloudAnchorId);

//        if (coordinates.get(cloudAnchorId) == null && coordinate != null) {
//            coordinates.put(cloudAnchorId, new HashSet<PointF>());
//            coordinates.get(cloudAnchorId).add(coordinate);
//        }
//        DatabaseReference roomRef = hotspotListRef.child(String.valueOf(roomCode));
//        DatabaseReference indexRef = roomRef.child(String.valueOf(cloudAnchorIds.size()));
//        DatabaseReference cloudAnchorIdRef = roomRef.child(cloudAnchorId);
//        cloudAnchorIdRef.child(KEY_DISPLAY_NAME).setValue(DISPLAY_NAME_VALUE);
//        cloudAnchorIdRef.child(KEY_ANCHOR_ID).setValue(cloudAnchorId);
//        cloudAnchorIdRef.child(KEY_TIMESTAMP).setValue(System.currentTimeMillis());
//        DatabaseReference drawRef = cloudAnchorIdRef.child(DRAW);
//        DatabaseReference indexRef = drawRef.child(String.valueOf(coordinates.get(cloudAnchorId).size()));
//        indexRef.child(X).setValue(coordinate.x);
//        indexRef.child(Y).setValue(coordinate.y);
        Call<Void> updateCloudAnchorCall = roomsService.updateCloudAnchor(roomId, cloudAnchorId, DISPLAY_NAME_VALUE);

        //サーバからのレスポンス
        updateCloudAnchorCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Room room = rooms.get(roomId);
                    room.putCloudAnchor(cloudAnchorId, new CloudAnchor(DISPLAY_NAME_VALUE, new Timestamp(System.currentTimeMillis())));
                    Log.d(TAG, "Success No." + roomId + " update!");
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
//    }
//});
    }

    /** Stores the given polygon in the given room code, cloudAnchorId. */
    synchronized void storePolygonInRoom(Long roomCode, String cloudAnchorId, float[] polygon) {
        Preconditions.checkNotNull(retrofit, "Retrofit was null");
        String roomId = String.valueOf(roomCode);

        Call<Void> updatePlaneCall = roomsService.updatePlane(roomId, cloudAnchorId, polygon);

        //サーバからのレスポンス
        updatePlaneCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Room room = rooms.get(roomId);
//                    room.getCloudAnchor(cloudAnchorId).setPlane(polygon);
                    Log.d(TAG, "Success No." + roomId + " updatePlane!");
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
    synchronized void storeStrokeInRoom(Long roomCode, String cloudAnchorId, float texX, float texY) {
        Preconditions.checkNotNull(retrofit, "Retrofit was null");
        String roomId = String.valueOf(roomCode);

        Call<Void> strokeCall = roomsService.stroke(roomId, cloudAnchorId, texX, texY);

        //サーバからのレスポンス
        strokeCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Success No." + roomId + " updateStroke!");
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
     * Registers a new listener for the given room code. The listener is invoked whenever the data for
     * the room code is changed.
     */
    // read
//    void registerNewListenerForRoom(Long roomCode, CloudAnchorIdListener listener) {
//        Preconditions.checkNotNull(app, "Firebase App was null");
//        clearRoomListener();
//        currentRoomRef = hotspotListRef.child(String.valueOf(roomCode));
//        currentRoomListener = new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        Object valObj = dataSnapshot.child(KEY_ANCHOR_ID).getValue();
//                        Object xObj = dataSnapshot.child(X).getValue();
//                        Object yObj = dataSnapshot.child(Y).getValue();
//                        if (valObj != null) {
//                            String anchorId = String.valueOf(valObj);
//                            PointF coordinate = new PointF(Float.parseFloat(String.valueOf(xObj)), Float.parseFloat(String.valueOf(yObj)));
//                            if (!anchorId.isEmpty()) {
//                                listener.onNewCloudAnchorId(anchorId, coordinate);
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//                        Log.w(TAG, "The Firebase operation was cancelled.", databaseError.toException());
//                    }
//                };
//        currentRoomRef.addValueEventListener(currentRoomListener);
//    }

    /**
     * Registers a new listener for the given room code. The listener is invoked whenever the data for
     * the room code is changed.
     */
    void registerNewListenerForRoom(Long roomCode, CloudAnchorIdListener onNewListener, CloudAnchorIdListener onUpdateListener) {
        Preconditions.checkNotNull(retrofit, "Retrofit was null");
//        clearRoomListener();
        String roomId = String.valueOf(roomCode);
        Call<Void> createRoomCall = roomsService.createRoom(roomId);
//        Log.d(TAG, roomId);
        //サーバからのレスポンス
        createRoomCall.enqueue(new Callback<Void>() {
            //成功時
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    rooms.put(roomId, new Room(roomId));
                    Log.d(TAG, "Success No." + roomId + " createRoom!");
                    if(timer == null) {
                        timer = new Timer();
//            new Thread(new Runnable() {
//                public void run() {
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                final Call<Map<String, Timestamp>> getRoomCall = roomsService.getRoom(roomId);
                                Response<Map<String, Timestamp>> getRoomResponse;
                                try {
                                    getRoomResponse = getRoomCall.execute();
                                    if (getRoomResponse.isSuccessful()) {
//                                        Log.d(TAG, "Success getRoom connection.");
                                        Room room = rooms.get(roomId);
                                        if (room != null) {
                                            Map<String, Timestamp> anchorToTimestamp = getRoomResponse.body();
                                            for (String anchorId : anchorToTimestamp.keySet()) {
                                                CloudAnchor cashedCloudAnchor = room.getCloudAnchor(anchorId);
                                                final Call<CloudAnchor> getCloudAnchorCall = roomsService.getCloudAnchor(roomId, anchorId);
                                                if (cashedCloudAnchor == null) {
                                                    getCloudAnchorCall.enqueue(new Callback<CloudAnchor>() {
                                                        @Override
                                                        public void onResponse(Call<CloudAnchor> call, Response<CloudAnchor> response) {
                                                            if (response.isSuccessful()) {
                                                                CloudAnchor cloudAnchor = response.body();
                                                                Log.d(TAG, "Success getCloudAnchor connection " + anchorId + " .");
                                                                room.putCloudAnchor(anchorId, cloudAnchor);
                                                                onNewListener.onNewCloudAnchorId(anchorId, cloudAnchor, null);
                                                            } else {
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
                                                            t.printStackTrace();
                                                            Log.d(TAG, "Failed getCloudAnchor.");
                                                        }
                                                    });
                                                } else if (cashedCloudAnchor != null && cashedCloudAnchor.getUpdateTimestamp().before(anchorToTimestamp.get(anchorId))) {
                                                    getCloudAnchorCall.enqueue(new Callback<CloudAnchor>() {
                                                        @Override
                                                        public void onResponse(Call<CloudAnchor> call, Response<CloudAnchor> response) {
                                                            if (response.isSuccessful()) {
                                                                CloudAnchor cloudAnchor = response.body();
                                                                Log.d(TAG, "Success getCloudAnchor connection " + anchorId + " .");
                                                                room.putCloudAnchor(anchorId, cloudAnchor);
                                                                onUpdateListener.onNewCloudAnchorId(anchorId, cloudAnchor, null);
                                                            } else {
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
                            }
                        };
                        timer.schedule(task, 0, 5000);
                    }
//                }
//        }).start();
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

//                    //サーバからのレスポンス
//                    getRoomCall.enqueue(new Callback<Map<String, Timestamp>>() {
//                        //成功時
//                        @Override
//                        public void onResponse(Call<Map<String, Timestamp>> call, Response<Map<String, Timestamp>> response) {
//                            if (response.isSuccessful()) {
//                                Room room = rooms.get(roomId);
//                                if (room != null) {
//                                    Map<String, Timestamp> result = response.body();
//                                    for (String anchorId : result.keySet()) {
//                                        CloudAnchor cloudAnchor = room.getCloudAnchor(anchorId);
//                                        if (cloudAnchor != null) {
//                                            if (cloudAnchor.getUpdateTimestamp().before(result.get(anchorId))) {
//
//                                            }
//                                        } else {
//
//                                        }
//                                    }
//                                }
//                                for (String cloudAnchorId: result.getCloudAnchors().keySet()) {
//                                    if (!cloudAnchorIds.contains(cloudAnchorId) && !cloudAnchorId.isEmpty()) {
//                                        for (PointTex2D drawCoordinate: result.getCloudAnchors().get(cloudAnchorId).getStroke()) {
//                                            PointF coordinate = new PointF(drawCoordinate.getX(), drawCoordinate.getY());
//                                            cloudAnchorIds.add(cloudAnchorId);
//                                            listener.onNewCloudAnchorId(cloudAnchorId, coordinate);
//                                        }
//                                    }
//                                }
//                            } else {
//                                try {
//                                    System.out.println(response.errorBody().string());
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                //onFailureでキャッチできないエラーの処理
//                                Log.d(TAG, "Error getRoom connection.");
//                            }
//                        }
//
//                        //失敗時
//                        @Override
//                        public void onFailure(Call<Map<String, Timestamp>> call, Throwable t) {
//                            t.printStackTrace();
//                            Log.d(TAG, "Failed getRoom.");
//                        }
//                    });
//                }
        }

//        currentRoomRef = hotspotListRef.child(String.valueOf(roomCode));
//        currentRoomListener = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                String cloudAnchorId = dataSnapshot.getKey();
//                if (!cloudAnchorIds.contains(cloudAnchorId) && !cloudAnchorId.isEmpty()) {
//                    currentDrawRef = currentRoomRef.child(cloudAnchorId).child(DRAW);
//                    currentDrawListener = new ChildEventListener() {
//                        @Override
//                        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                            String index = dataSnapshot.getKey();
//                            currentIndexRef = currentDrawRef.child(index);
//                            currentIndexListener = new ValueEventListener() {
//                                @Override
//                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                    Object xObj = dataSnapshot.child(X).getValue();
//                                    Object yObj = dataSnapshot.child(Y).getValue();
//                                    PointF coordinate = new PointF(Float.parseFloat(String.valueOf(xObj)), Float.parseFloat(String.valueOf(yObj)));
//                                    cloudAnchorIds.add(cloudAnchorId);
//                                    listener.onNewCloudAnchorId(cloudAnchorId, coordinate);
//                                }
//
//                                @Override
//                                public void onCancelled(@NonNull DatabaseError databaseError) {
//                                }
//                            };
//                            currentIndexRef.addValueEventListener(currentIndexListener);
//                        }
//
//                        @Override
//                        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                        }
//
//                        @Override
//                        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//                        }
//
//                        @Override
//                        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//                        }
//                    };
//                    currentDrawRef.addChildEventListener(currentDrawListener);
//                }
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.w(TAG, "The Firebase operation was cancelled.", databaseError.toException());
//            }
//        };
//        currentRoomRef.addChildEventListener(currentRoomListener);

//        currentRoomListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                Object indexObj = dataSnapshot.getKey();
//                String index = String.valueOf(indexObj);
//                Object valObj = dataSnapshot.child(String.valueOf(indexObj)).child(KEY_ANCHOR_ID).getValue();
//                Object xObj = dataSnapshot.child(String.valueOf(indexObj)).child(X).getValue();
//                Object yObj = dataSnapshot.child(String.valueOf(indexObj)).child(Y).getValue();
//                if (valObj != null) {
//                    String anchorId = String.valueOf(valObj);
//                    if(!cloudAnchorIds.contains(anchorId) && !anchorId.isEmpty()) {
//                        PointF coordinate = new PointF(Float.parseFloat(String.valueOf(xObj)), Float.parseFloat(String.valueOf(yObj)));
//                        cloudAnchorIds.add(anchorId);
//                        listener.onNewCloudAnchorId(anchorId, coordinate);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                Log.w(TAG, "The Firebase operation was cancelled.", databaseError.toException());
//            }
//        };
//        currentRoomRef.addValueEventListener(currentRoomListener);

//        currentRoomRef = hotspotListRef.child(String.valueOf(roomCode));
//        currentRoomListener = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                String index = dataSnapshot.getKey();
//                currentIndexRef = currentRoomRef.child(index);
//                currentIndexListener = new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        Object valObj = dataSnapshot.child(KEY_ANCHOR_ID).getValue();
//                        Object xObj = dataSnapshot.child(X).getValue();
//                        Object yObj = dataSnapshot.child(Y).getValue();
//                        if (valObj != null) {
//                            String anchorId = String.valueOf(valObj);
//                            if(!cloudAnchorIds.contains(anchorId) && !anchorId.isEmpty()) {
//                                PointF coordinate = new PointF(Float.parseFloat(String.valueOf(xObj)), Float.parseFloat(String.valueOf(yObj)));
//                                cloudAnchorIds.add(anchorId);
//                                listener.onNewCloudAnchorId(anchorId, coordinate);
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//                        Log.w(TAG, "The Firebase operation was cancelled.", databaseError.toException());
//                    }
//                };
//                currentIndexRef.addValueEventListener(currentIndexListener);
////                Object valObj = dataSnapshot.child(KEY_ANCHOR_ID).getValue();
////                Object xObj = dataSnapshot.child(X).getValue();
////                Object yObj = dataSnapshot.child(Y).getValue();
////                if (valObj != null) {
////                    String anchorId = String.valueOf(valObj);
////                    if(!cloudAnchorIds.contains(anchorId) && !anchorId.isEmpty()) {
////                        PointF coordinate = new PointF(Float.parseFloat(String.valueOf(xObj)), Float.parseFloat(String.valueOf(yObj)));
////                        cloudAnchorIds.add(anchorId);
////                        listener.onNewCloudAnchorId(anchorId, coordinate);
////                    }
////                }
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.w(TAG, "The Firebase operation was cancelled.", databaseError.toException());
//            }
//        };
//        currentRoomRef.addChildEventListener(currentRoomListener);

    /**
     * Resets the current room listener registered using {@link #registerNewListenerForRoom(Long,
     * CloudAnchorIdListener, CloudAnchorIdListener)}.
     */
    void clearRoomListener() {
        if (timer != null) {
            timer.cancel();
        }
//        if (currentRoomListener != null && currentRoomRef != null) {
//            currentRoomRef.removeEventListener(currentRoomListener);
//            currentRoomListener = null;
//            currentRoomRef = null;
//        }
    }
}
