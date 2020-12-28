package com.prepost.imagetotext.Models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "extracted_model")
public class ExtractedModel {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String content;

    public ExtractedModel(String content) {
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
