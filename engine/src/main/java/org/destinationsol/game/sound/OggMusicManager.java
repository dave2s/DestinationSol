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
import org.destinationsol.GameOptions;
import org.destinationsol.assets.Assets;

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
        }//if battle music was playing, stop and play gamemusic
        else if (currentlyPlaying != null && battleMusic.contains(currentlyPlaying)){
            stopMusic();
            playMusic(gameMusic.get(0), options);
        }
        else {
            playMusic(gameMusic.get(0), options);
        }
    }

    private void playBattleMusic(final GameOptions options) {

        //battlemusic playing, continue
        if (currentlyPlaying != null && battleMusic.contains(currentlyPlaying)) {
            //do nothing
            return;
        }//menumusic playing, stop music and start playing new music
        else if (currentlyPlaying != null && gameMusic.contains(currentlyPlaying)){
            stopMusic();
        }
        //else {
        playMusic(battleMusic.get(0), options);
        currentlyPlaying.setOnCompletionListener(music -> playGameMusic(options)); //check whether this shouldn't rather call outside check for enemies
        //}
    }

    public void playBattleMusic(final GameOptions options, int weight){
        //increase queue
        //if greater than threshold - playBattleMusic - needs check for gameMusic playing - stop and play battleMusic - should be in local playBattleMusic
    }
    //weight is to represent (in future updates) the size of the ship that is calling this

    public void stopBattleMusic(final GameOptions options, int weight){
        //increase queue

        //if less than threshold - playGameMusic - needs check for battleMusic playing - stop and play gameMusic - should be in local playGameMusic
    }

    public void playMusic(Music music, GameOptions options) {
        currentlyPlaying = music;
        currentlyPlaying.setVolume(options.musicVolumeMultiplier);
        currentlyPlaying.play();
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
}
