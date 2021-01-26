package com.prepost.imagetotext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.prepost.imagetotext.Adapter.HistoryAdapter;
import com.prepost.imagetotext.Models.ExtractedModel;
import com.prepost.imagetotext.database.Database;

import java.util.ArrayList;
import java.util.List;

import static com.prepost.imagetotext.MainActivity.HISTORY_TEXT_CODE;

public class HistoryActivity extends AppCompatActivity {

    public static String SET_HISTORY_CONTENT;
    private TextView mNoHistory_tv;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setTitle("History");

        mNoHistory_tv = findViewById(R.id.history_no_content_tv);

        RecyclerView recyclerView = findViewById(R.id.history_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        List<ExtractedModel> models = new ArrayList<>();
        HistoryAdapter adapter = new HistoryAdapter(this);
        recyclerView.setAdapter(adapter);

        //Get All Data From Database
        models = Database.getInstance(this).extractedDAO().getAllData();
        adapter.addModel(models);
        if (models.size() > 0) mNoHistory_tv.setVisibility(View.GONE);

        //Adapter Click Listener
        adapter.setOnItemClickListener(model -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(SET_HISTORY_CONTENT, model.getContent());
            setResult(HISTORY_TEXT_CODE, intent);
            onBackPressed();
        });

    }

    private static final String TAG = "HistoryActivity";

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed: pressed");
        finish();
    }
}