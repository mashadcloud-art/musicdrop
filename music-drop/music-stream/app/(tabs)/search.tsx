import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo, useState } from 'react';
import { Image, Keyboard, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MiniPlayer } from '@/components/MiniPlayer';
import { songs } from '@/data/music';
import { useColors } from '@/hooks/useColors';
import { useMusic } from '@/context/MusicContext';

const genres = ['Chill', 'Indie pop', 'Focus', 'New releases'];

export default function SearchScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { playTrack } = useMusic();
  const [query, setQuery] = useState<string>('');
  const results = useMemo(
    () => songs.filter((song) => `${song.title} ${song.artist} ${song.album}`.toLowerCase().includes(query.toLowerCase())),
    [query],
  );

  return (
    <View style={[styles.screen, { backgroundColor: colors.background }]}>
      <ScrollView keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingTop: insets.top + 16, paddingBottom: 150 }}>
        <View style={styles.topRow}>
          <View>
            <Text style={[styles.eyebrow, { color: colors.primary }]}>DISCOVER</Text>
            <Text style={[styles.title, { color: colors.foreground }]}>Find your sound</Text>
          </View>
          <Pressable onPress={() => router.back()} hitSlop={10}>
            <Feather name="x" size={22} color={colors.mutedForeground} />
          </Pressable>
        </View>
        <View style={[styles.inputWrap, { backgroundColor: colors.card, borderColor: query ? colors.primary : colors.card }]}>
          <Feather name="search" size={19} color={colors.mutedForeground} />
          <TextInput
            autoFocus
            value={query}
            onChangeText={setQuery}
            placeholder="Artists, songs, or albums"
            placeholderTextColor={colors.mutedForeground}
            returnKeyType="search"
            onSubmitEditing={() => Keyboard.dismiss()}
            style={[styles.input, { color: colors.foreground }]}
          />
          {query.length > 0 && (
            <Pressable onPress={() => setQuery('')} hitSlop={10}>
              <Feather name="x-circle" size={17} color={colors.mutedForeground} />
            </Pressable>
          )}
        </View>
        {query.length === 0 ? (
          <>
            <Text style={[styles.sectionTitle, { color: colors.foreground }]}>Browse all</Text>
            <View style={styles.genreGrid}>
              {genres.map((genre, index) => (
                <Pressable key={genre} onPress={() => setQuery(genre.split(' ')[0])} style={[styles.genre, { backgroundColor: index % 2 ? colors.primary : colors.secondary }]}>
                  <Text style={[styles.genreText, { color: index % 2 ? colors.primaryForeground : colors.secondaryForeground }]}>{genre}</Text>
                  <Feather name={index % 2 ? 'music' : 'headphones'} size={28} color={index % 2 ? colors.primaryForeground : colors.secondaryForeground} />
                </Pressable>
              ))}
            </View>
            <Text style={[styles.sectionTitle, { color: colors.foreground, marginTop: 30 }]}>Popular right now</Text>
          </>
        ) : (
          <Text style={[styles.resultLabel, { color: colors.mutedForeground }]}>{results.length} results for “{query}”</Text>
        )}
        {results.map((song) => (
          <Pressable key={song.id} onPress={() => playTrack(song)} style={({ pressed }) => [styles.resultRow, { borderBottomColor: colors.border, opacity: pressed ? 0.7 : 1 }]}>
            <Image source={song.cover} style={styles.cover} />
            <View style={styles.resultInfo}>
              <Text style={[styles.songTitle, { color: colors.foreground }]}>{song.title}</Text>
              <Text style={[styles.songMeta, { color: colors.mutedForeground }]}>{song.artist} · {song.album}</Text>
            </View>
            <Feather name="play" size={18} color={colors.primary} />
          </Pressable>
        ))}
      </ScrollView>
      <View style={[styles.miniWrap, { bottom: insets.bottom + 78 }]}><MiniPlayer /></View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  topRow: { paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  eyebrow: { fontFamily: 'Inter_700Bold', fontSize: 10, letterSpacing: 1.5, marginBottom: 7 },
  title: { fontFamily: 'Inter_700Bold', fontSize: 26, letterSpacing: -0.7 },
  inputWrap: { height: 52, borderRadius: 14, marginHorizontal: 20, marginTop: 22, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 10, borderWidth: 1 },
  input: { flex: 1, fontFamily: 'Inter_400Regular', fontSize: 14 },
  sectionTitle: { marginHorizontal: 20, marginTop: 30, fontFamily: 'Inter_700Bold', fontSize: 19, letterSpacing: -0.4 },
  genreGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginHorizontal: 20, marginTop: 14 },
  genre: { width: '47%', height: 82, borderRadius: 15, padding: 14, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  genreText: { fontFamily: 'Inter_700Bold', fontSize: 14, maxWidth: 80 },
  resultLabel: { marginHorizontal: 20, marginTop: 24, fontFamily: 'Inter_400Regular', fontSize: 12 },
  resultRow: { marginHorizontal: 20, minHeight: 74, borderBottomWidth: 1, flexDirection: 'row', alignItems: 'center', gap: 12 },
  cover: { width: 50, height: 50, borderRadius: 8 },
  resultInfo: { flex: 1, gap: 5 },
  songTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 14 },
  songMeta: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  miniWrap: { position: 'absolute', left: 16, right: 16 },
});