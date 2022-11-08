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
    private final List<UseCase> ALL_USE_CASE = Arrays.asList(
            new UseCase("Whiteboard + MultiVideoPlayer", "", WhiteMultiVideoPlayerActivity.class),
            new UseCase("MultiVideoPlayerActivity", "", MultiVideoPlayerActivity.class),
            new UseCase("WhiteSelectionClusterPlayer", "", WhiteSelectionClusterPlayerActivity.class),
            new UseCase("TriplePlayer", "", TriplePlayerActivity.class),
            new UseCase("ClusterPlayer", "", ClusterPlayerActivity.class),
            new UseCase("WhiteboardSelectionPlayer", "", WhiteboardSelectionPlayerActivity.class),
            new UseCase("SelectionPlayer", "", SelectionPlayerActivity.class),
            new UseCase("OffsetPlayer", "", OffsetPlayerActivity.class),
            new UseCase("WhiteboardPlayer", "", WhiteboardPlayerActivity.class),
            new UseCase("VideoPlayer", "", VideoPlayerActivity.class)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        UseCaseAdapter useCaseAdapter = new UseCaseAdapter(ALL_USE_CASE);
        useCaseAdapter.setOnItemClickListener(this::gotoUserCase);
        recyclerView.setAdapter(useCaseAdapter);
    }

    private void gotoUserCase(UseCase useCase) {
        Intent intent = new Intent(this, useCase.displayClass);
        startActivity(intent);
    }
}