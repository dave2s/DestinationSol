/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.destinationsol.game.sound;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import org.destinationsol.GameOptions;
import org.destinationsol.assets.Assets;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.screens.MainScreen;
import org.destinationsol.game.screens.WarnDrawer;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that is responsible for playing all music throughout the game.
 *
 * @author SimonC4
 * @author Rulasmur
 */
public class OggMusicManager {
    private final Music menuMusic;
    private final List<Music> gameMusic;
    private final List<Music> battleMusic;
    private Music currentlyPlaying = null;
    private int battleMusicBucket = 0;
    private final byte battleMusicBucketThreshold = 1; //A value of battleMusicBucket at which the battlemusic starts to play
    private final long battleMusicSwitchOffDelay = 5; // Delay (seconds) to wait between switching Battle -> Game music;
    private final long battleMusicSwitchOnDelay = 2; // Delay (seconds) to wait between switching Game -> Battle music;

    private Timer timer = null;

    public OggMusicManager() {
        menuMusic = Assets.getMusic("engine:dreadnaught").getMusic();
        menuMusic.setLooping(true);

        gameMusic = new ArrayList<>();
        battleMusic = new ArrayList<>();
        gameMusic.add(Assets.getMusic("engine:cimmerianDawn").getMusic());
        gameMusic.add(Assets.getMusic("engine:intoTheDark").getMusic());
        gameMusic.add(Assets.getMusic("engine:spaceTheatre").getMusic());
        battleMusic.add(Assets.getMusic("engine:battle/defenseLine").getMusic());
        battleMusic.add(Assets.getMusic("engine:battle/powerBots").getMusic());
    }

    /**
     * Start playing the music menu from the beginning of the track. The menu music loops continuously.
     */
    public void playMenuMusic(GameOptions options) {
        if (currentlyPlaying != null) {
            if (currentlyPlaying != menuMusic || !currentlyPlaying.isPlaying()) {
                stopMusic();
                playMusic(menuMusic, options);
            }
        } else {
            stopMusic();
            playMusic(menuMusic, options);
        }
    }

    public void playGameMusic(final GameOptions options) {
        stopMusic();
        if (currentlyPlaying != null && gameMusic.contains(currentlyPlaying)) {
            int index = gameMusic.indexOf(currentlyPlaying) + 1;
            if (gameMusic.size() - 1 >= index) {
                playMusic(gameMusic.get(index), options);
                currentlyPlaying.setOnCompletionListener(music -> playGameMusic(options));//check whether this shouldn't rather call outside check for enemies

            } else {
                playMusic(gameMusic.get(0), options);
            }
        }
        else {
            playMusic(gameMusic.get(0), options);
        }
    }

    public void update(SolGame game, final GameOptions options) {
        //play or stop battle music based on EnemyWarning
        if (game.getScreens().mainScreen.isWarnUp(MainScreen.EnemyWarn.class, game)) {
           if (!battleMusic.contains(currentlyPlaying)) {
                if (timer == null) {
                    timer = new Timer();
                    timer.schedule(new Task() {
                                       @Override
                                       public void run() {
                                           playBattleMusic(options, battleMusicBucketThreshold);
                                           timer = null;
                                       }
                                   }
                            , battleMusicSwitchOnDelay
                    );
                }
                else;//nothing
            } else {
                //battle music is and should be playing;
               //if timer is set to any task - start or stop battle music - dispose of it because it is and should be playing
               if(timer != null) { timer = null;}
            }
            //playBattleMusic(options, battleMusicBucketThreshold);
        //warning is not up, battle music should not be playing
        } else {
            if (battleMusic.contains(currentlyPlaying)) {
                if (timer == null) {
                    timer.schedule(new Task() {
                                       @Override
                                       public void run() {
                                           playGameMusic(options);
                                           timer = null;
                                       }
                                   }
                            , battleMusicSwitchOffDelay        //    (delay)
                            //, amtOfSec     //    (seconds)
                    );
                }
                else; //nothing
            }else if (currentlyPlaying == null){
                playGameMusic(options);
            }
            else if (gameMusic.contains(currentlyPlaying)){
                //do nothing
            }else;
            //playGameMusic(options);//stopBattleMusic(options, battleMusicBucketThreshold);
        }
    }

    /**
     * Stop playing all music.
     */
    public void stopMusic() {
        if (currentlyPlaying != null) {
            currentlyPlaying.stop();
        }
    }

    public void resetVolume(GameOptions options) {
        currentlyPlaying.setVolume(options.musicVolumeMultiplier);
    }

    private void playMusic(Music music, GameOptions options) {
        currentlyPlaying = music;
        currentlyPlaying.setVolume(options.musicVolumeMultiplier);
        currentlyPlaying.play();
    }

    private void playBattleMusic(final GameOptions options, int weight){
        //increase queue
        if ((this.battleMusicBucket + weight) < 0){
            //TODO this might create a problem where a ship with high battleMusicWeigth gets truncated to int max
            //TODO and then if it lives long enough, other ships which trigger stopBattleMusic might get it under the
            //TODO battleMusicBucketThreshold even though the big ship still did not trigger stopBattleMusic and it's
            //TODO Weight is higher than battleMusicBucketThreshold (in other words the battlemusic should still be playing)
            this.battleMusicBucket = Integer.MAX_VALUE;
        } else {
            this.battleMusicBucket += weight;
        }
        //if greater than threshold - playBattleMusic - needs check for gameMusic playing - stop and play battle - should be in local playGameMusic
        if(shouldBattleMusicPlay()){
            playBattleMusic(options);
        }
    }

    private void playBattleMusic(final GameOptions options) {

        //battlemusic playing, continue
        if (currentlyPlaying != null && battleMusic.contains(currentlyPlaying)) {
            int index = battleMusic.indexOf(currentlyPlaying) + 1;
            if (battleMusic.size() - 1 >= index) {
                playMusic(battleMusic.get(index), options);
                currentlyPlaying.setOnCompletionListener(music -> playBattleMusic(options));//check whether this shouldn't rather call outside check for enemies
            } else {
                playMusic(battleMusic.get(0), options);
            }
            return;
        }//gamemusic playing, stop music and start playing new music
        else if (currentlyPlaying != null && gameMusic.contains(currentlyPlaying)){
            stopMusic();
        }
        playMusic(battleMusic.get(0), options);
        currentlyPlaying.setOnCompletionListener(music -> playBattleMusic(options)); //check whether this shouldn't rather call outside check for enemies
    }

    //
    private boolean shouldBattleMusicPlay(){
        return (battleMusicBucket >= battleMusicBucketThreshold);
    }

}
