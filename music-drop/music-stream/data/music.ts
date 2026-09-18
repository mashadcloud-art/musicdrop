import type { ImageSourcePropType } from 'react-native';

export type Song = {
  id: string;
  title: string;
  artist: string;
  album: string;
  duration: string;
  cover: ImageSourcePropType;
};

export const covers = {
  midnight: require('@/assets/images/cover-midnight.png'),
  afterglow: require('@/assets/images/cover-afterglow.png'),
  icon: require('@/assets/images/icon_2.png'),
};

export const songs: Song[] = [
  {
    id: 'midnight-drive',
    title: 'Midnight Drive',
    artist: 'The Coastline',
    album: 'After Hours',
    duration: '3:42',
    cover: covers.midnight,
  },
  {
    id: 'slow-motion',
    title: 'Slow Motion',
    artist: 'Nova Lane',
    album: 'After Hours',
    duration: '4:08',
    cover: covers.midnight,
  },
  {
    id: 'city-lights',
    title: 'City Lights',
    artist: 'The Coastline',
    album: 'After Hours',
    duration: '3:19',
    cover: covers.midnight,
  },
  {
    id: 'afterglow',
    title: 'Afterglow',
    artist: 'Mila June',
    album: 'Soft Focus',
    duration: '3:58',
    cover: covers.afterglow,
  },
  {
    id: 'golden-hour',
    title: 'Golden Hour',
    artist: 'Mila June',
    album: 'Soft Focus',
    duration: '3:31',
    cover: covers.afterglow,
  },
  {
    id: 'paper-planes',
    title: 'Paper Planes',
    artist: 'Lumen Club',
    album: 'Soft Focus',
    duration: '2:54',
    cover: covers.afterglow,
  },
  {
    id: 'ocean-bloom',
    title: 'Ocean Bloom',
    artist: 'Sage Arcade',
    album: 'Sunday Service',
    duration: '4:24',
    cover: covers.midnight,
  },
];

export const albums = [
  {
    id: 'after-hours',
    title: 'After Hours',
    artist: 'The Coastline',
    subtitle: 'A late-night collection',
    cover: covers.midnight,
    songIds: ['midnight-drive', 'slow-motion', 'city-lights'],
  },
  {
    id: 'soft-focus',
    title: 'Soft Focus',
    artist: 'Mila June',
    subtitle: 'Warm light, slow mornings',
    cover: covers.afterglow,
    songIds: ['afterglow', 'golden-hour', 'paper-planes'],
  },
];

export const findSong = (id: string) => songs.find((song) => song.id === id) ?? songs[0];