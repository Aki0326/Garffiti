package org.ntlab.graffiti.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.ntlab.graffiti.db.dao.GameResultDao;
import org.ntlab.graffiti.db.entity.GameResult;

/**
 * Helper to use Room database.
 *
 * Created by a-hongo on 04,3月,2021
 * @author a-hongo
 */
@Database(entities = {GameResult.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance = null;

    // データーベース名
    private static final String DATABASE_NAME = "app_database.db";

    public abstract GameResultDao gameResultDao();

    private final MutableLiveData<Boolean> isDbCreated = new MutableLiveData<>();

    public static AppDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = buildDatabase(context.getApplicationContext());
                    instance.updateDatabaseCreated(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Build the database. {@link Builder#build()} only sets up the database configuration and
     * creates a new instance of the database.
     * The SQLite database is only created when it's accessed for the first time.
     */
    private static AppDatabase buildDatabase(final Context appContext) {
        return Room.databaseBuilder(appContext, AppDatabase.class, DATABASE_NAME)
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        AppDatabase database = AppDatabase.getInstance(appContext);
                        // notify that the database was created and it's ready to be used
                        database.setDatabaseCreated();
                    }
                })
                .build();
    }

    /**
     * Check whether the database already exists and expose it via {@link #getDatabaseCreated()}
     */
    private void updateDatabaseCreated(final Context context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            setDatabaseCreated();
        }
    }

    private void setDatabaseCreated(){
        isDbCreated.postValue(true);
    }

    public LiveData<Boolean> getDatabaseCreated() {
        return isDbCreated;
    }

}