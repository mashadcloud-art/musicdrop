import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import { BlurView } from 'expo-blur';
import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useState } from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useColors } from '@/hooks/useColors';
import { useMusic } from '@/context/MusicContext';
import { useTheme } from '@/context/ThemeContext';

export default function PlayerScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { mode } = useTheme();
  const { currentTrack, isPlaying, togglePlay, toggleLike, isLiked, skipNext, skipPrevious } = useMusic();
  const [progress, setProgress] = useState<number>(0.28);

  useEffect(() => {
    if (!isPlaying) return undefined;
    const interval = setInterval(() => setProgress((value) => (value >= 0.98 ? 0 : value + 0.004)), 1000);
    return () => clearInterval(interval);
  }, [isPlaying, currentTrack.id]);

  return (
    <View style={[styles.screen, { backgroundColor: colors.background, paddingTop: insets.top + 12, paddingBottom: insets.bottom + 18 }]}>
      <Image source={currentTrack.cover} style={styles.backgroundArt} blurRadius={18} />
      <BlurView intensity={78} tint={mode === 'dark' ? 'dark' : 'light'} style={StyleSheet.absoluteFill} />
      <LinearGradient
        colors={[colors.background + '55', colors.background + 'D9']}
        style={StyleSheet.absoluteFill}
      />
      <View style={[styles.topBar, { backgroundColor: colors.card + '77', borderColor: colors.foreground + '20' }]}>
        <Pressable onPress={() => router.back()} style={styles.topBarButton} hitSlop={8}><Feather name="chevron-down" size={24} color={colors.foreground} /></Pressable>
        <Text style={[styles.nowPlaying, { color: colors.mutedForeground }]}>NOW PLAYING</Text>
        <Pressable style={styles.topBarButton} hitSlop={8}><Feather name="more-horizontal" size={21} color={colors.foreground} /></Pressable>
      </View>
      <View style={[styles.artWrap, { backgroundColor: colors.card + '77', borderColor: colors.foreground + '38' }]}>
        <Image source={currentTrack.cover} style={styles.art} />
      </View>
      <View style={[styles.trackHeader, { backgroundColor: colors.card + '8C', borderColor: colors.foreground + '20' }]}>
        <View style={styles.trackCopy}>
          <Text style={[styles.trackTitle, { color: colors.foreground }]}>{currentTrack.title}</Text>
          <Text style={[styles.trackArtist, { color: colors.mutedForeground }]}>{currentTrack.artist}</Text>
        </View>
        <Pressable onPress={() => toggleLike()} hitSlop={12}>
          <Feather name={isLiked(currentTrack.id) ? 'heart' : 'heart'} size={23} color={isLiked(currentTrack.id) ? colors.primary : colors.mutedForeground} />
        </Pressable>
      </View>
      <View style={[styles.progressWrap, { backgroundColor: colors.card + '70', borderColor: colors.foreground + '18' }]}>
        <View style={[styles.progressTrack, { backgroundColor: colors.foreground + '2E' }]}>
          <View style={[styles.progressFill, { backgroundColor: colors.primary, width: `${progress * 100}%` }]} />
          <View style={[styles.progressThumb, { backgroundColor: colors.primary, left: `${progress * 100}%` }]} />
        </View>
        <View style={styles.timeRow}>
          <Text style={[styles.time, { color: colors.mutedForeground }]}>1:04</Text>
          <Text style={[styles.time, { color: colors.mutedForeground }]}>{currentTrack.duration}</Text>
        </View>
      </View>
      <View style={styles.controls}>
        <Pressable onPress={skipPrevious} style={[styles.controlButton, { backgroundColor: colors.card + '9C', borderColor: colors.foreground + '20' }]} hitSlop={8}><Feather name="skip-back" size={22} color={colors.foreground} /></Pressable>
        <Pressable onPress={togglePlay} style={[styles.playButton, { backgroundColor: colors.primary, shadowColor: colors.primary }]} hitSlop={8}>
          <Feather name={isPlaying ? 'pause' : 'play'} size={25} color={colors.primaryForeground} />
        </Pressable>
        <Pressable onPress={skipNext} style={[styles.controlButton, { backgroundColor: colors.card + '9C', borderColor: colors.foreground + '20' }]} hitSlop={8}><Feather name="skip-forward" size={22} color={colors.foreground} /></Pressable>
      </View>
      <View style={[styles.bottomActions, { backgroundColor: colors.card + '77', borderColor: colors.foreground + '20' }]}>
        <Feather name="shuffle" size={19} color={colors.mutedForeground} />
        <Text style={[styles.albumText, { color: colors.mutedForeground }]}>{currentTrack.album}</Text>
        <Feather name="repeat" size={19} color={colors.mutedForeground} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: 22, justifyContent: 'space-between', overflow: 'hidden' },
  backgroundArt: { ...StyleSheet.absoluteFill, width: '100%', height: '100%', opacity: 0.72 },
  topBar: { height: 48, borderRadius: 24, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 6 },
  topBarButton: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
  nowPlaying: { fontFamily: 'Inter_700Bold', fontSize: 10, letterSpacing: 1.7 },
  artWrap: { width: '100%', aspectRatio: 1, maxHeight: 365, alignSelf: 'center', marginTop: 22, borderRadius: 30, overflow: 'hidden', borderWidth: 1, padding: 7 },
  art: { width: '100%', height: '100%' },
  trackHeader: { flexDirection: 'row', alignItems: 'center', marginTop: 20, paddingHorizontal: 16, paddingVertical: 14, borderRadius: 20, borderWidth: 1 },
  trackCopy: { flex: 1, gap: 7 },
  trackTitle: { fontFamily: 'Inter_700Bold', fontSize: 26, letterSpacing: -0.7 },
  trackArtist: { fontFamily: 'Inter_400Regular', fontSize: 14 },
  progressWrap: { marginTop: 18, paddingHorizontal: 15, paddingTop: 14, paddingBottom: 10, borderRadius: 18, borderWidth: 1 },
  progressTrack: { height: 4, borderRadius: 2, position: 'relative' },
  progressFill: { height: 4, borderRadius: 2 },
  progressThumb: { width: 11, height: 11, borderRadius: 6, position: 'absolute', top: -3, marginLeft: -5 },
  timeRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 },
  time: { fontFamily: 'Inter_400Regular', fontSize: 10 },
  controls: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 22, marginTop: 14 },
  controlButton: { width: 48, height: 48, borderRadius: 24, borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  playButton: { width: 68, height: 68, borderRadius: 34, alignItems: 'center', justifyContent: 'center', paddingLeft: 3, shadowOpacity: 0.36, shadowRadius: 14, shadowOffset: { width: 0, height: 8 }, elevation: 8 },
  bottomActions: { height: 48, borderRadius: 24, borderWidth: 1, paddingHorizontal: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 14 },
  albumText: { fontFamily: 'Inter_500Medium', fontSize: 12 },
});