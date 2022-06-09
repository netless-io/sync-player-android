package com.agora.netless.syncplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class VideoPlayerTest {
    private val videoUrl = "https://flat-storage.oss-cn-hangzhou.aliyuncs.com/temp/BigBuckBunny.mp4"

    class PlayerPlayCase constructor(
        private val countDownLatch: CountDownLatch
    ) : AtomPlayerListener {

        val playerPhaseList = ArrayList<AtomPlayerPhase>()

        override fun onPhaseChanged(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
            playerPhaseList.add(phaseChange)
            if (phaseChange == AtomPlayerPhase.Playing) {
                countDownLatch.countDown()
            }
        }
    }

    class PlayerSeekCase constructor(
        private val countDownLatch: CountDownLatch
    ) : AtomPlayerListener {

        var onSeekCount = 0;

        override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
            onSeekCount++;
            countDownLatch.countDown()
        }
    }


    @Test
    fun play_case_ready_and_playing_only() {
        val countDownLatch = CountDownLatch(1)
        val fakePlayerListener = PlayerPlayCase(countDownLatch)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val videoPlayer = VideoPlayer(appContext, videoUrl = videoUrl)
            videoPlayer.addPlayerListener(fakePlayerListener)
            videoPlayer.play()
        }
        countDownLatch.await()

        assert(fakePlayerListener.playerPhaseList.size == 2)
        assert(fakePlayerListener.playerPhaseList[0] == AtomPlayerPhase.Ready)
        assert(fakePlayerListener.playerPhaseList[1] == AtomPlayerPhase.Playing)
    }

    @Test
    fun seek_case_onSeek_call_once() {
        val countDownLatch = CountDownLatch(10)
        val playerSeekCase = PlayerSeekCase(countDownLatch)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        lateinit var videoPlayer: VideoPlayer
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            videoPlayer = VideoPlayer(appContext, videoUrl = videoUrl)
            videoPlayer.addPlayerListener(playerSeekCase)
            videoPlayer.play()
        }

        while (true) {
            Thread.sleep(2000)
            if (videoPlayer.currentPhase == AtomPlayerPhase.Playing) {
                for (i in 1..10) {
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        videoPlayer.seekTo(i * 10_000L)
                    }
                }
                break
            }
        }
        countDownLatch.await()

        assert(playerSeekCase.onSeekCount == 10)
    }
}