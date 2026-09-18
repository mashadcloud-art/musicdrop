import { Feather } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { albums, songs } from '@/data/music';
import { MiniPlayer } from '@/components/MiniPlayer';
import { useColors } from '@/hooks/useColors';
import { useMusic } from '@/context/MusicContext';

export default function AlbumScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { playTrack } = useMusic();
  const album = albums.find((item) => item.id === id) ?? albums[0];
  const albumSongs = album.songIds.map((songId) => songs.find((song) => song.id === songId)).filter(Boolean);

  return (
    <View style={[styles.screen, { backgroundColor: colors.background }]}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 150 }}>
        <View style={[styles.coverWrap, { paddingTop: insets.top + 12 }]}>
          <Image source={album.cover} style={styles.cover} />
          <Pressable onPress={() => router.back()} style={[styles.back, { backgroundColor: colors.background + 'AA' }]} hitSlop={8}><Feather name="arrow-left" size={20} color={colors.foreground} /></Pressable>
        </View>
        <View style={styles.header}>
          <Text style={[styles.title, { color: colors.foreground }]}>{album.title}</Text>
          <Text style={[styles.artist, { color: colors.mutedForeground }]}>{album.artist} · 2026</Text>
          <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>{album.subtitle}</Text>
          <View style={styles.actions}>
            <Pressable onPress={() => playTrack(albumSongs[0] ?? songs[0])} style={[styles.playAll, { backgroundColor: colors.primary }]}>
              <Feather name="play" size={17} color={colors.primaryForeground} />
              <Text style={[styles.playAllText, { color: colors.primaryForeground }]}>Play all</Text>
            </Pressable>
            <Pressable style={[styles.add, { backgroundColor: colors.card }]}><Feather name="plus" size={20} color={colors.foreground} /></Pressable>
          </View>
        </View>
        <View style={styles.list}>
          {albumSongs.map((song, index) => song && (
            <Pressable key={song.id} onPress={() => playTrack(song)} style={({ pressed }) => [styles.row, { opacity: pressed ? 0.7 : 1 }]}>
              <Text style={[styles.index, { color: colors.mutedForeground }]}>{String(index + 1).padStart(2, '0')}</Text>
              <View style={styles.songInfo}><Text style={[styles.songTitle, { color: colors.foreground }]}>{song.title}</Text><Text style={[styles.songMeta, { color: colors.mutedForeground }]}>{song.artist}</Text></View>
              <Text style={[styles.duration, { color: colors.mutedForeground }]}>{song.duration}</Text>
              <Feather name="more-vertical" size={18} color={colors.mutedForeground} />
            </Pressable>
          ))}
        </View>
      </ScrollView>
      <View style={[styles.miniWrap, { bottom: insets.bottom + 78 }]}><MiniPlayer /></View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  coverWrap: { height: 350, alignItems: 'center', justifyContent: 'center' },
  cover: { width: 270, height: 270, borderRadius: 18 },
  back: { position: 'absolute', left: 20, top: 55, width: 38, height: 38, borderRadius: 19, alignItems: 'center', justifyContent: 'center' },
  header: { paddingHorizontal: 20, paddingTop: 2 },
  title: { fontFamily: 'Inter_700Bold', fontSize: 29, letterSpacing: -0.8 },
  artist: { fontFamily: 'Inter_600SemiBold', fontSize: 13, marginTop: 8 },
  subtitle: { fontFamily: 'Inter_400Regular', fontSize: 12, marginTop: 5 },
  actions: { flexDirection: 'row', gap: 10, marginTop: 18 },
  playAll: { height: 44, borderRadius: 22, paddingHorizontal: 18, flexDirection: 'row', alignItems: 'center', gap: 8 },
  playAllText: { fontFamily: 'Inter_700Bold', fontSize: 13 },
  add: { height: 44, width: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
  list: { marginTop: 16 },
  row: { minHeight: 68, marginHorizontal: 20, flexDirection: 'row', alignItems: 'center', gap: 14 },
  index: { width: 20, fontFamily: 'Inter_500Medium', fontSize: 11 },
  songInfo: { flex: 1, gap: 5 },
  songTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 14 },
  songMeta: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  duration: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  miniWrap: { position: 'absolute', left: 16, right: 16 },
});