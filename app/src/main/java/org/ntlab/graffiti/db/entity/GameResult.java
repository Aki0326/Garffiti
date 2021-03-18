package org.ntlab.graffiti.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * GameResult Entity
 *
 * Created by a-hongo on 03,3月,2021
 * @author a-hongo
 */
@Entity(tableName = "game_results",
        indices = {@Index(value = {"name", "score"},
        unique = true)})
public class GameResult {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo
    public String name;

    // TODO Change type of score from long to Integer
    @ColumnInfo
    public long score;

    @Ignore
    public int color;

    public GameResult() {
        this(0);
    }

    @Ignore
    public GameResult(long score) {
        this.name = null;
        this.score = score;
    }

    @Ignore
    public GameResult(String name, long score) {
        this(score);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
