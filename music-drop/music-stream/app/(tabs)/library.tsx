import { Feather } from '@expo/vector-icons';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MiniPlayer } from '@/components/MiniPlayer';
import { songs } from '@/data/music';
import { useColors } from '@/hooks/useColors';
import { useMusic } from '@/context/MusicContext';

export default function LibraryScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { likedIds, playTrack } = useMusic();
  const likedSongs = songs.filter((song) => likedIds.includes(song.id));

  return (
    <View style={[styles.screen, { backgroundColor: colors.background }]}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingTop: insets.top + 16, paddingBottom: 150 }}>
        <View style={styles.header}>
          <View>
            <Text style={[styles.eyebrow, { color: colors.primary }]}>YOUR MUSIC</Text>
            <Text style={[styles.title, { color: colors.foreground }]}>Library</Text>
          </View>
          <Pressable accessibilityLabel="Library options" hitSlop={10}><Feather name="more-horizontal" size={22} color={colors.mutedForeground} /></Pressable>
        </View>
        <View style={styles.filterRow}>
          <View style={[styles.filter, { backgroundColor: colors.primary }]}><Text style={[styles.filterText, { color: colors.primaryForeground }]}>All</Text></View>
          <View style={[styles.filter, { backgroundColor: colors.card }]}><Text style={[styles.filterText, { color: colors.mutedForeground }]}>Playlists</Text></View>
          <View style={[styles.filter, { backgroundColor: colors.card }]}><Text style={[styles.filterText, { color: colors.mutedForeground }]}>Albums</Text></View>
        </View>
        <Pressable onPress={() => router.push('/album/after-hours')} style={[styles.libraryCard, { backgroundColor: colors.card }]}>
          <View style={[styles.libraryIcon, { backgroundColor: colors.accent }]}><Feather name="heart" size={22} color={colors.accentForeground} /></View>
          <View style={styles.cardInfo}><Text style={[styles.cardTitle, { color: colors.foreground }]}>Liked songs</Text><Text style={[styles.cardMeta, { color: colors.mutedForeground }]}>{likedSongs.length} saved tracks</Text></View>
          <Feather name="chevron-right" size={19} color={colors.mutedForeground} />
        </Pressable>
        <Text style={[styles.sectionTitle, { color: colors.foreground }]}>Recently saved</Text>
        {likedSongs.length === 0 ? (
          <View style={[styles.empty, { backgroundColor: colors.card }]}>
            <Feather name="heart" size={25} color={colors.primary} />
            <Text style={[styles.emptyTitle, { color: colors.foreground }]}>Build your collection</Text>
            <Text style={[styles.emptyText, { color: colors.mutedForeground }]}>Tap the heart on any song to find it here.</Text>
          </View>
        ) : likedSongs.map((song) => (
          <Pressable key={song.id} onPress={() => playTrack(song)} style={({ pressed }) => [styles.songRow, { borderBottomColor: colors.border, opacity: pressed ? 0.7 : 1 }]}>
            <Image source={song.cover} style={styles.cover} />
            <View style={styles.songInfo}><Text style={[styles.songTitle, { color: colors.foreground }]}>{song.title}</Text><Text style={[styles.songMeta, { color: colors.mutedForeground }]}>{song.artist}</Text></View>
            <Feather name="play-circle" size={21} color={colors.primary} />
          </Pressable>
        ))}
      </ScrollView>
      <View style={[styles.miniWrap, { bottom: insets.bottom + 78 }]}><MiniPlayer /></View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  header: { paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  eyebrow: { fontFamily: 'Inter_700Bold', fontSize: 10, letterSpacing: 1.5, marginBottom: 7 },
  title: { fontFamily: 'Inter_700Bold', fontSize: 27, letterSpacing: -0.7 },
  filterRow: { flexDirection: 'row', gap: 8, marginHorizontal: 20, marginTop: 23 },
  filter: { paddingHorizontal: 16, paddingVertical: 9, borderRadius: 18 },
  filterText: { fontFamily: 'Inter_600SemiBold', fontSize: 12 },
  libraryCard: { marginHorizontal: 20, marginTop: 22, borderRadius: 16, padding: 14, flexDirection: 'row', alignItems: 'center', gap: 12 },
  libraryIcon: { width: 52, height: 52, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  cardInfo: { flex: 1, gap: 5 },
  cardTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 14 },
  cardMeta: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  sectionTitle: { marginHorizontal: 20, marginTop: 30, marginBottom: 12, fontFamily: 'Inter_700Bold', fontSize: 19 },
  empty: { marginHorizontal: 20, borderRadius: 16, padding: 24, alignItems: 'center', gap: 8 },
  emptyTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 15, marginTop: 4 },
  emptyText: { fontFamily: 'Inter_400Regular', fontSize: 12, textAlign: 'center' },
  songRow: { minHeight: 72, marginHorizontal: 20, borderBottomWidth: 1, flexDirection: 'row', alignItems: 'center', gap: 12 },
  cover: { width: 50, height: 50, borderRadius: 8 },
  songInfo: { flex: 1, gap: 5 },
  songTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 14 },
  songMeta: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  miniWrap: { position: 'absolute', left: 16, right: 16 },
});