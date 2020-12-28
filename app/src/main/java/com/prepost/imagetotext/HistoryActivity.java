package com.prepost.imagetotext;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.prepost.imagetotext.Adapter.HistoryAdapter;
import com.prepost.imagetotext.Models.ExtractedModel;
import com.prepost.imagetotext.database.Database;

import java.util.ArrayList;
import java.util.List;

import static com.prepost.imagetotext.MainActivity.HISTORY_TEXT_CODE;

public class HistoryActivity extends AppCompatActivity {

    public static String SET_HISTORY_CONTENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setTitle("History");
        RecyclerView recyclerView = findViewById(R.id.history_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        List<ExtractedModel> models = new ArrayList<>();
        HistoryAdapter adapter = new HistoryAdapter(this);
        recyclerView.setAdapter(adapter);

        //Get All Data From Database
        models = Database.getInstance(this).extractedDAO().getAllData();
        adapter.addModel(models);

        //Adapter Click Listener
        adapter.setOnItemClickListener(model -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(SET_HISTORY_CONTENT, model.getContent());
            setResult(HISTORY_TEXT_CODE, intent);
            onBackPressed();
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}