package org.ntlab.graffiti.graffiti.controls;

import org.ntlab.graffiti.entities.CloudAnchor;
import org.ntlab.graffiti.entities.Room;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Rooms API
 * @author a-hongo
 */
public interface RoomsAPI {
    @GET("rooms")
    Call<Collection<Room>> getRooms();

    @GET("rooms/{roomId}")
    Call<Map<String, Timestamp>> getRoom(@Path("roomId") String roomId);

    @GET("rooms/{roomId}/{cloudAnchorId}")
    Call<CloudAnchor> getCloudAnchor(@Path("roomId") String roomId , @Path("cloudAnchorId") String cloudAnchorId);

    @PUT("rooms/{roomId}")
    Call<Void> createRoom(@Path("roomId") String roomId);

    @FormUrlEncoded
    @PUT("rooms/{roomId}/{cloudAnchorId}")
    Call<Void> updateCloudAnchor(@Path("roomId") String roomId, @Path("cloudAnchorId") String cloudAnchorId, @Nullable @Field("displayName") String displayName);

    @FormUrlEncoded
    @PUT("rooms/{roomId}/{cloudAnchorId}/plane")
    Call<Void> updatePlane(@Path("roomId") String roomId, @Path("cloudAnchorId") String cloudAnchorId, @Field("polygon") float[] polygon);

    @FormUrlEncoded
    @POST("rooms/{roomId}/{cloudAnchorId}/stroke")
    Call<Void> stroke(@Path("roomId") String roomId, @Path("cloudAnchorId") String cloudAnchorId, @Field("texX") float texX, @Field("texY") float texY);

    @DELETE("rooms/{roomId}")
    Call<Room> deleteRoom(@Path("roomId") String roomId);
}
