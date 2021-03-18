package org.ntlab.graffiti.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.ntlab.graffiti.db.entity.GameResult;

import java.util.List;

/**
 * GameResult DAO
 *
 * Created by a-hongo on 04,3月,2021
 * @author a-hongo
 */
@Dao
public interface GameResultDao {
    @Query("SELECT * FROM game_results")
    List<GameResult> getAll();

    @Query("SELECT * FROM game_results ORDER BY score DESC LIMIT :number")
    List<GameResult> getTopGameResults(int number);

    @Query("SELECT id FROM game_results ORDER BY id DESC LIMIT 1")
    long getMaxId();

    @Query("SELECT * FROM game_results WHERE id IN (:ids)")
    List<GameResult> findByIds(long[] ids);

    @Query("SELECT * FROM game_results WHERE name LIKE :name")
    List<GameResult> findByName(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(GameResult gameResult);

    @Insert
    void insertAll(List<GameResult> gameResults);

    @Delete
    void delete(GameResult gameResult);

    @Query("DELETE FROM game_results WHERE id = :id")
    void deleteById(long id);

}
