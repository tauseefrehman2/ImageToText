package com.prepost.imagetotext.DAO;

import com.prepost.imagetotext.Models.ExtractedModel;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface ExtractedDAO {
    @Insert
    long insert(ExtractedModel model);

    @Delete
    void delete(ExtractedModel model);

    @Query("Select * from extracted_model order by id desc")
    List<ExtractedModel> getAllData();
}
