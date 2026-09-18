import { Feather } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useColors } from '@/hooks/useColors';
import { useMusic } from '@/context/MusicContext';
import { useTheme } from '@/context/ThemeContext';

export function MiniPlayer() {
  const colors = useColors();
  const { currentTrack, isPlaying, togglePlay, skipNext } = useMusic();
  const { mode } = useTheme();

  return (
    <Pressable testID="mini-player" onPress={() => router.push('/player')} style={({ pressed }) => [styles.container, { borderColor: colors.foreground + '28', opacity: pressed ? 0.92 : 1 }]}>
      <BlurView intensity={55} tint={mode === 'dark' ? 'dark' : 'light'} style={StyleSheet.absoluteFill} />
      <View style={[styles.tint, { backgroundColor: colors.card + 'C7' }]} />
      <Image source={currentTrack.cover} style={styles.cover} />
      <View style={styles.info}>
        <Text numberOfLines={1} style={[styles.title, { color: colors.foreground }]}>
          {currentTrack.title}
        </Text>
        <Text numberOfLines={1} style={[styles.artist, { color: colors.mutedForeground }]}>
          {currentTrack.artist}
        </Text>
      </View>
      <Pressable
        accessibilityLabel={isPlaying ? 'Pause' : 'Play'}
        hitSlop={10}
        onPress={(event) => {
          event.stopPropagation();
          togglePlay();
        }}
        style={[styles.control, { backgroundColor: colors.secondary }]}
      >
        <Feather name={isPlaying ? 'pause' : 'play'} size={18} color={colors.foreground} />
      </Pressable>
      <Pressable
        accessibilityLabel="Next track"
        hitSlop={10}
        onPress={(event) => {
          event.stopPropagation();
          skipNext();
        }}
        style={styles.control}
      >
        <Feather name="skip-forward" size={18} color={colors.foreground} />
      </Pressable>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    height: 64,
    borderRadius: 18,
    borderWidth: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 9,
    gap: 10,
    overflow: 'hidden',
  },
  tint: { ...StyleSheet.absoluteFill },
  cover: { width: 46, height: 46, borderRadius: 11 },
  info: { flex: 1, gap: 3 },
  title: { fontFamily: 'Inter_600SemiBold', fontSize: 13 },
  artist: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  control: { width: 34, height: 34, borderRadius: 17, alignItems: 'center', justifyContent: 'center' },
});