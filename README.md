## SyncPlayer [![](https://jitpack.io/v/netless-io/sync-player-android.svg)](https://jitpack.io/#netless-io/sync-player-android)

Plays multiple media(videos or whiteboards) at the same time with synchronized progress and speed.
Stops when the longest media ended.

## Overview

- `AtomPlayer`: Abstract class for anything that is playable.
    - `VideoPlayer`: For `ExoPlayer` supported media.
    - `WhiteboardPlayer`:
      For [Netless Whiteboard](https://developer.netless.link/android-zh/home/android-replay) replay
      room.
    - `OffsetPlayer`: Add blank offset before an `AtomPlayer`.
    - `SelectionPlayer`: Cherry-pick segments of an `AtomPlayer`.
    - `SyncPlayer`: Factory class for grouping AtomPlayers.

## Environment

### Requirements

* Android SDK Version >= 21
* Android Tools Build >= 4.1.0

### build.gradle

```groovy
// project build
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}

// app build
android {
    // ...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation "com.github.netless-io:sync-player-android:1.1.1"
}
```

## Usage

You may clone this repo and run the [app](./app).

### Basic

example [ClusterPlayerActivity](app/src/main/java/com/agora/netless/syncplayer/ClusterPlayerActivity.java)

```java
class Example {
    private void initPlayer() {
        VideoPlayer videoPlayer1 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[0]);
        // Note: videoPlayer need a container to display
        videoPlayer1.setPlayerContainer(playerContainer1);
        VideoPlayer videoPlayer2 = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer2.setPlayerContainer(playerContainer2);

        AtomPlayer finalPlayer = new ClusterPlayer(videoPlayer1, videoPlayer2);
        finalPlayer.addPlayerListener(new AtomPlayerListener() {
        });
        finalPlayer.play();
    }
}
```

### OffsetPlayer

You may add a time offset before any `AtomPlayer`:

```java
class Example {
    private void initPlayer() {
        VideoPlayer videoPlayer = new VideoPlayer(this, Constant.ALL_VIDEO_URL[0]);
        AtomPlayer finalPlayer = new OffsetPlayer(videoPlayer, 5000L);

        finalPlayer.addPlayerListener(new AtomPlayerListener() {
        });
        finalPlayer.play();
    }
}
```

example [OffsetPlayerActivity](app/src/main/java/com/agora/netless/syncplayer/OffsetPlayerActivity.java)

### SelectionPlayer

You may trim any `AtomPlayer` to selected parts by providing a selection list.

```java
class Example {
    private void initPlayer() {
        VideoPlayer videoPlayer = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);

        AtomPlayer finalPlayer = new SelectionPlayer(
                videoPlayer,
                new SelectionOptions(Arrays.asList(
                        new Selection(5_000, 10_000),
                        new Selection(15_000, 20_000),
                        new Selection(30_000, 40_000),
                        new Selection(60_000, 100_000))
                )
        );
        finalPlayer.addPlayerListener(new AtomPlayerListener() {
        });
        finalPlayer.play();
    }
}
```

example [SelectionPlayerActivity](app/src/main/java/com/agora/netless/syncplayer/SelectionPlayerActivity.java)

### Sync With Netless Whiteboard

Sync videos with Netless Whiteboard Replay.

```java
class Example {
    private void onWhitePlayerReady(Player player) {
        AtomPlayer videoPlayer = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        AtomPlayer selectionPlayer = new SelectionPlayer(new WhiteboardPlayer(player), new SelectionOptions(
                Arrays.asList(
                        new Selection(5_000, 10_000),
                        new Selection(15_000, 20_000),
                        new Selection(30_000, 40_000),
                        new Selection(60_000, 120_000)
                )
        ));
        AtomPlayer finalPlayer = new ClusterPlayer(selectionPlayer, videoPlayer);
        finalPlayer.addPlayerListener(new AtomPlayerListener() {
        });
        finalPlayer.play();
    }
}
```

see
example [WhiteSelectionClusterPlayerActivity](app/src/main/java/com/agora/netless/syncplayer/WhiteSelectionClusterPlayerActivity.java)

## API

All apis see [AtomPlayer](library/src/main/java/com/agora/netless/syncplayer/AtomPlayer.kt)

### play

```java
atomPlayer.play();
```

### pause

```java
atomPlayer.pause();
```

### stop

```java
atomPlayer.stop();
```

### seekTo

```java
atomPlayer.seekTo(200);
```

### duration

Duration(in millisecond) of the longest media.

### currentPosition

Player progress time(in millisecond).

### setPlaybackSpeed

Player playback speed.

### status

Player status.
see [AtomPlayer.AtomPlayerPhase](library/src/main/java/com/agora/netless/syncplayer/AtomPlayer.kt)

- `Idle` Player init status or error status
- `Ready` Player can play immediately.
- `Pause` Player paused by user invoking `player.pause()`.
- `Playing` Player is playing.
- `Buffering` Player is buffering.
- `Ended` Player ends.

### SyncPlayer.combine

A function create a AtomPlayer to play grouping AtomPlayers

```java
class Example {
    private void code() {
        AtomPlayer atomPlayer = SyncPlayer.combine(atomPlayer1, atomPlayer2, atomPlayer3,...);
    }
}
```

### AtomPlayerBuilder

A builder create AtomPlayer for less temporary AtomPlayer statement.

Note: here some constraints

* AtomPlayerBuilder can only create one VideoPlayer or one WhiteboardPlayer once.
* AtomPlayerBuilder only store one offset and one selection record.

```java
class Example {
    private void createWithNewOperator(Player player) {
        AtomPlayer videoPlayer = new VideoPlayer(this, Constant.ALL_VIDEO_URL[1]);
        videoPlayer.setPlayerContainer(playerContainer);

        WhiteboardPlayer whiteboardPlayer = new WhiteboardPlayer(player);
        SelectionPlayer selectionPlayer = new SelectionPlayer(whiteboardPlayer,
                new SelectionOptions(Arrays.asList(
                        new Selection(5_000, 10_000),
                        new Selection(15_000, 20_000),
                        new Selection(30_000, 40_000),
                        new Selection(60_000, 720_000)
                )));

        finalPlayer = SyncPlayer.combine(selectionPlayer, videoPlayer);
    }

    private void createWithAtomPlayerBuilder(Player player) {
        AtomPlayer videoPlayer = new AtomPlayerBuilder()
                .video(this, Constant.ALL_VIDEO_URL[1])
                .setPlayerContainer(playerContainer)
                .create();

        AtomPlayer whiteboardPlayer = new AtomPlayerBuilder()
                .whiteboard(player)
                .selection(new SelectionOptions(Arrays.asList(
                        new Selection(5_000, 10_000),
                        new Selection(15_000, 20_000),
                        new Selection(30_000, 40_000),
                        new Selection(60_000, 720_000)
                )))
                .create();

        finalPlayer = SyncPlayer.combine(whiteboardPlayer, videoPlayer);
    }
}
```
