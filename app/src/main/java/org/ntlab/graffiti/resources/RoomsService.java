package org.ntlab.graffiti.resources;

import org.ntlab.graffiti.entities.Room;

import java.util.Collection;

import javax.annotation.Nullable;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RoomsService {
    @GET("rooms")
    Call<Collection<Room>> getRooms();

    @GET("rooms/{roomId}")
    Call<Room> getRoom(@Path("roomId") String roomId);

    @FormUrlEncoded
    @POST("rooms")
    Call<Room> createRoom(@Field("roomId") String roomId);

    @FormUrlEncoded
    @PUT("rooms/{roomId}")
    Call<Room> updateRoom(@Path("roomId") String roomId, @Field("cloudAnchorId") String cloudAnchorId, @Field("displayName") String displayName, @Field("x") float x, @Field("y") float y);

    @DELETE("rooms/{roomId}")
    Call<Room> deleteRoom(@Path("roomId") String roomId);
}
