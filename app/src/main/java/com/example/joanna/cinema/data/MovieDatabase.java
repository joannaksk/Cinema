package com.example.joanna.cinema.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.OnConfigure;
import net.simonvt.schematic.annotation.OnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by joanna on 07/07/16.
 */
@Database(
         version = MovieDatabase.VERSION,
        packageName = "com.example.joanna.cinema.provider")
public final class MovieDatabase {

    private MovieDatabase(){

    }

    public static final int VERSION = 1;

    @Table(MovieContract.MovieColumns.class)
    public static final String MOVIES = "movies";

    @OnCreate
    public static void onCreate(Context context, SQLiteDatabase db) {
    }

    @OnUpgrade
    public static void onUpgrade(Context context, SQLiteDatabase db, int oldVersion,
                                 int newVersion) {
    }

    @OnConfigure
    public static void onConfigure(SQLiteDatabase db) {
    }

    @ExecOnCreate
    public static final String EXEC_ON_CREATE = "SELECT * FROM " + MOVIES;

}
