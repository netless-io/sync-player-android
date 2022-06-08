package com.agora.netless.syncplayer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agora.netless.syncplayer.misc.UseCase;
import com.agora.netless.syncplayer.misc.UseCaseAdapter;

import java.util.Arrays;
import java.util.List;

/**
 * @author fenglibin
 */
public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UseCaseAdapter useCaseAdapter;

    private final List<UseCase> ALL_USE_CASE = Arrays.asList(
            new UseCase("WhiteboardSelectionPlayer", "", WhiteboardSelectionPlayerActivity.class),
            new UseCase("SelectionPlayer", "", SelectionPlayerActivity.class),
            new UseCase("OffsetPlayer", "", OffsetPlayerActivity.class),
            new UseCase("VideoPlayer", "", VideoPlayerActivity.class),
            new UseCase("WhiteboardPlayer", "", WhiteboardPlayerActivity.class),
            new UseCase("ClusterPlayer", "", ClusterPlayerActivity.class),
            new UseCase("TriplePlayer", "", TriplePlayerActivity.class)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        useCaseAdapter = new UseCaseAdapter(ALL_USE_CASE);
        useCaseAdapter.setOnItemClickListener(this::gotoUserCase);
        recyclerView.setAdapter(useCaseAdapter);
    }

    private void gotoUserCase(UseCase useCase) {
        Intent intent = new Intent(this, useCase.displayClass);
        startActivity(intent);
    }
}