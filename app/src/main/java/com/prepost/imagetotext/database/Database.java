package com.prepost.imagetotext.database;

import android.content.Context;

import com.prepost.imagetotext.DAO.ExtractedDAO;
import com.prepost.imagetotext.Models.ExtractedModel;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@androidx.room.Database(entities = {ExtractedModel.class},
        version = 1, exportSchema = false)
public abstract class Database extends RoomDatabase {

    private static Database instance;

    //Because we are initializing database here
    //Room automatically connect our code to this methods

    public abstract ExtractedDAO extractedDAO();


    //Create object using singleton method
    public static synchronized Database getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext()
                    , Database.class, "extract_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .addCallback(roomCallBack).build();
        }
        return instance;
    }

    private static Callback roomCallBack = new Callback() {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }
    };
}
