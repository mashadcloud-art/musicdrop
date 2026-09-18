import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Haptics from 'expo-haptics';
import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { songs, type Song } from '@/data/music';

type MusicContextValue = {
  currentTrack: Song;
  isPlaying: boolean;
  likedIds: string[];
  playTrack: (song: Song) => void;
  togglePlay: () => void;
  toggleLike: (songId?: string) => void;
  skipNext: () => void;
  skipPrevious: () => void;
  isLiked: (songId: string) => boolean;
};

const MusicContext = createContext<MusicContextValue | null>(null);
const LIKES_KEY = '@music-stream/liked';

export function MusicProvider({ children }: { children: React.ReactNode }) {
  const [currentTrack, setCurrentTrack] = useState<Song>(songs[0]);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [likedIds, setLikedIds] = useState<string[]>([]);
  const [hydrated, setHydrated] = useState<boolean>(false);

  useEffect(() => {
    AsyncStorage.getItem(LIKES_KEY).then((stored) => {
      if (stored) setLikedIds(JSON.parse(stored) as string[]);
      setHydrated(true);
    });
  }, []);

  useEffect(() => {
    if (hydrated) {
      AsyncStorage.setItem(LIKES_KEY, JSON.stringify(likedIds));
    }
  }, [hydrated, likedIds]);

  const playTrack = (song: Song) => {
    setCurrentTrack(song);
    setIsPlaying(true);
    void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  };

  const togglePlay = () => {
    setIsPlaying((playing) => !playing);
    void Haptics.selectionAsync();
  };

  const toggleLike = (songId = currentTrack.id) => {
    setLikedIds((ids) =>
      ids.includes(songId) ? ids.filter((id) => id !== songId) : [...ids, songId],
    );
    void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  };

  const skip = (direction: 1 | -1) => {
    const index = songs.findIndex((song) => song.id === currentTrack.id);
    const nextIndex = (index + direction + songs.length) % songs.length;
    playTrack(songs[nextIndex]);
  };

  const value = useMemo<MusicContextValue>(
    () => ({
      currentTrack,
      isPlaying,
      likedIds,
      playTrack,
      togglePlay,
      toggleLike,
      skipNext: () => skip(1),
      skipPrevious: () => skip(-1),
      isLiked: (songId) => likedIds.includes(songId),
    }),
    [currentTrack, isPlaying, likedIds],
  );

  return <MusicContext.Provider value={value}>{children}</MusicContext.Provider>;
}

export function useMusic() {
  const context = useContext(MusicContext);
  if (!context) throw new Error('useMusic must be used inside MusicProvider');
  return context;
}