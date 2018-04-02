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
    private final long battleMusicSwitchOffDelay = 7; // Delay (seconds) to wait between switching Battle -> Game music;
    private final long battleMusicSwitchOnDelay = 3; // Delay (seconds) to wait between switching Game -> Battle music;

    private int lastBattMusicIndex = 0; //for battle music
    private int lastGameMusicIndex = 0; //for game music

    private Timer timer;
    private Task gameMusicTask;
    private Task battleMusicTask;

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

        timer = new Timer();
        gameMusicTask = new Task(){public void run(){}};
        battleMusicTask = new Task(){public void run(){}};
        //musicSwitch;
    }

    /**
     * Start playing the music menu from the beginning of the track. The menu music loops continuously.
     */
    public void playMenuMusic(GameOptions options) {
       // saveCurrentlyPlayingPosition();
       // stopMusic();
        pauseMusic();
        if (currentlyPlaying != null) {
            if (currentlyPlaying != menuMusic || !currentlyPlaying.isPlaying()) {
                playMusic(menuMusic, options);
            }
        } else {
            playMusic(menuMusic, options);
        }
    }

    public void playGameMusic(final GameOptions options) {
        pauseMusic();
        if (currentlyPlaying != null && gameMusic.contains(currentlyPlaying)) {
                playMusic(gameMusic.get(lastGameMusicIndex), options);
                currentlyPlaying.setOnCompletionListener(music -> playNextGameMusic(options));
        }
        else {
            playMusic(gameMusic.get(lastGameMusicIndex), options);
        }
    }

    /**
     * //preceded by playNextBattleMusic() or update() in which case last song continues
     * @param options GameOptions
     */
    public void playBattleMusic(final GameOptions options) {
        pauseMusic();
        playMusic(battleMusic.get(lastBattMusicIndex), options);
        currentlyPlaying.setOnCompletionListener(music -> playNextBattleMusic(options));
    }

    /**
     * Switches battle and game music based on a EnemyWarn sign.
     * Uses libgdx Timer and Task to schedule music switching. This simulates delay so the music does not get
     * cut immediately after the EnemyWarn ceases for a second or vice versa.
     * @param game SolGame
     * @param options GameOptions
     */
    public void update(SolGame game, final GameOptions options) {
        //play or stop battle music based on EnemyWarning
        if (game.getScreens().mainScreen.isWarnUp(MainScreen.EnemyWarn.class, game)) {
           if (!battleMusic.contains(currentlyPlaying)) {
                if (!battleMusicTask.isScheduled()) {
                    //create new task to play battle music
                    battleMusicTask = new Task() {
                                       @Override
                                       public void run() {
                                           playBattleMusic(options, battleMusicBucketThreshold);
                                       }
                    };
                    //schedule battle music in bMSOnDelay seconds
                    timer.schedule(battleMusicTask,battleMusicSwitchOnDelay);
                }
                else;//nothing
            }else if (battleMusic.contains(currentlyPlaying) && gameMusicTask.isScheduled()) {
               gameMusicTask.cancel(); //battle music is and should be playing but gameMusic is planned for future so we need to cancel it
            }
        //warning is not up, battle music should not be playing
        } else {
            if (battleMusic.contains(currentlyPlaying)) {
                if (!gameMusicTask.isScheduled()) {
                    //create new task to play game music
                    gameMusicTask = new Task() {
                        @Override
                        public void run() {
                            playGameMusic(options);
                        }
                    };
                    //schedule game music in bMSOffDelay seconds
                    timer.schedule(gameMusicTask, battleMusicSwitchOffDelay);
                }
                else; //nothing
            //game music is and should be playing but battleMusic is planned for future so we need to cancel it
            }else if (gameMusic.contains(currentlyPlaying) && battleMusicTask.isScheduled()){
                battleMusicTask.cancel();
            //nothing is playing
            }else if (currentlyPlaying == null){
                if (battleMusicTask.isScheduled()) {//battleMusic is scheduled
                    battleMusicTask.cancel();
                }
                playGameMusic(options);
            }else;
            //playGameMusic(options);//stopBattleMusic(options, battleMusicBucketThreshold);
        }
    }

    /**
     * Stop playing all music.
     */
    public void stopMusic() {
        if (currentlyPlaying != null) {
            //saveCurrentlyPlayingPosition();
            currentlyPlaying.stop();
        }
    }

    /**
     * Pause currently playing music remembering it's position
     */
    public void pauseMusic(){
        if(currentlyPlaying != null) {
           currentlyPlaying.pause();
        }
    }

    /**
     * Reset volume for the currently playing music according to SolGame musicVolumeMultiplier
     * @param options - Options of the SolGame
     */
    public void resetVolume(GameOptions options) {
        currentlyPlaying.setVolume(options.musicVolumeMultiplier);
    }

    private void playNextGameMusic(GameOptions options){
        lastGameMusicIndex = gameMusic.indexOf(currentlyPlaying) + 1;
        if (gameMusic.size() - 1 >= lastGameMusicIndex) {
            lastGameMusicIndex = 0;
        }
        stopMusic();
        playGameMusic(options);
    }

    private void playNextBattleMusic(GameOptions  options){
        lastBattMusicIndex = battleMusic.indexOf(currentlyPlaying) + 1;
        if (battleMusic.size() - 1 >= lastBattMusicIndex) {
            lastBattMusicIndex = 0;
        }
        stopMusic();
        playBattleMusic(options);
    }

    private void playMusic(Music music, GameOptions options) {
        currentlyPlaying = music;
        currentlyPlaying.setVolume(options.musicVolumeMultiplier);
        currentlyPlaying.play();
    }

    private void playBattleMusic(final GameOptions options, int weight){
        //increase queue
        if ((this.battleMusicBucket + weight) < 0){
            this.battleMusicBucket = Integer.MAX_VALUE;
        } else {
            this.battleMusicBucket += weight;
        }
        //if greater than threshold - playBattleMusic - needs check for gameMusic playing - stop and play battle - should be in local playGameMusic
        if(shouldBattleMusicPlay()){
            playBattleMusic(options);
        }
    }

    private boolean shouldBattleMusicPlay(){
        return (battleMusicBucket >= battleMusicBucketThreshold);
    }

}
