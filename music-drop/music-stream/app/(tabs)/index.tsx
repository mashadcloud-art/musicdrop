import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MiniPlayer } from '@/components/MiniPlayer';
import { albums, songs } from '@/data/music';
import { useColors } from '@/hooks/useColors';
import { useMusic } from '@/context/MusicContext';
import { useTheme } from '@/context/ThemeContext';

export default function HomeScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { playTrack } = useMusic();
  const { mode, design, toggleMode, toggleDesign } = useTheme();
  const quickPicks = songs.slice(0, 4);

  if (design === 'greenroom') {
    return <GreenroomHome />;
  }

  return (
    <View style={[styles.screen, { backgroundColor: colors.background }]}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingTop: insets.top + 14, paddingBottom: 150 }}
      >
        <View style={styles.header}>
          <View>
            <Text style={[styles.brand, { color: colors.foreground }]}>
              {design === 'pulse' ? 'pulse.' : design === 'orbit' ? 'orbit.' : 'greenroom.'}
            </Text>
            <Text style={[styles.eyebrow, { color: colors.mutedForeground }]}>SUNDAY, 13 SEP · 7:42 PM</Text>
          </View>
          <View style={styles.headerActions}>
            <Pressable
              accessibilityLabel={`Switch to ${mode === 'dark' ? 'light' : 'dark'} mode`}
              onPress={toggleMode}
              style={[styles.iconButton, { backgroundColor: colors.card, borderColor: colors.border }]}
            >
              <Feather name={mode === 'dark' ? 'sun' : 'moon'} size={16} color={colors.foreground} />
            </Pressable>
            <Pressable
              accessibilityLabel="Switch to the next visual design"
              onPress={toggleDesign}
              style={[styles.avatar, { backgroundColor: colors.accent }]}
            >
              <Text style={[styles.avatarText, { color: colors.accentForeground }]}>
                {design === 'pulse' ? 'M' : design === 'orbit' ? 'O' : 'G'}
              </Text>
            </Pressable>
          </View>
        </View>

        <Pressable
          onPress={() => router.push('/search')}
          style={({ pressed }) => [styles.searchBar, { backgroundColor: colors.card, opacity: pressed ? 0.8 : 1 }]}
        >
          <Feather name="search" size={18} color={colors.mutedForeground} />
          <Text style={[styles.searchText, { color: colors.mutedForeground }]}>What do you want to listen to?</Text>
        </Pressable>

        <View style={styles.sectionHeading}>
          <Text style={[styles.sectionTitle, { color: colors.foreground }]}>Your evening mix</Text>
          <Pressable onPress={() => router.push('/album/after-hours')}>
            <Text style={[styles.seeAll, { color: colors.primary }]}>Open album</Text>
          </Pressable>
        </View>
        <Pressable
          onPress={() => playTrack(songs[0])}
          style={({ pressed }) => [styles.hero, { backgroundColor: colors.card, opacity: pressed ? 0.9 : 1 }]}
        >
          <Image source={albums[0].cover} style={styles.heroImage} />
          <LinearGradient
            colors={['transparent', colors.background + 'E8']}
            locations={[0.22, 1]}
            style={styles.heroShade}
          />
          <View style={styles.heroContent}>
            <View style={[styles.heroPill, { backgroundColor: colors.primary }]}>
              <View style={[styles.pillDot, { backgroundColor: colors.primaryForeground }]} />
              <Text style={[styles.heroKicker, { color: colors.primaryForeground }]}>MADE FOR YOU</Text>
            </View>
            <Text style={styles.heroTitle}>
              {design === 'pulse' ? 'After Hours' : design === 'orbit' ? 'Orbit Radio' : 'Daily Mix 01'}
            </Text>
            <Text style={styles.heroSubtitle}>
              {design === 'pulse'
                ? 'A smooth ride through midnight city lights'
                : design === 'orbit'
                  ? 'A rotating signal of songs worth keeping'
                  : 'A playlist made for your best hours'}
            </Text>
            <View style={[styles.heroButton, { backgroundColor: colors.primary }]}>
              <Feather name="play" size={16} color={colors.primaryForeground} />
              <Text style={[styles.heroButtonText, { color: colors.primaryForeground }]}>
                {design === 'pulse' ? 'Play mix' : design === 'orbit' ? 'Start radio' : 'Play playlist'}
              </Text>
            </View>
          </View>
        </Pressable>

        <View style={styles.sectionHeading}>
          <Text style={[styles.sectionTitle, { color: colors.foreground }]}>Quick picks</Text>
          <Pressable onPress={() => router.push('/search')}>
            <Text style={[styles.seeAll, { color: colors.primary }]}>See all</Text>
          </Pressable>
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.horizontalList}>
          {quickPicks.map((song) => (
            <Pressable
              key={song.id}
              onPress={() => playTrack(song)}
              style={({ pressed }) => [styles.pick, { opacity: pressed ? 0.72 : 1 }]}
            >
              <Image source={song.cover} style={styles.pickImage} />
              <Text numberOfLines={1} style={[styles.pickTitle, { color: colors.foreground }]}>{song.title}</Text>
              <Text numberOfLines={1} style={[styles.pickArtist, { color: colors.mutedForeground }]}>{song.artist}</Text>
            </Pressable>
          ))}
        </ScrollView>

        <View style={styles.sectionHeading}>
          <Text style={[styles.sectionTitle, { color: colors.foreground }]}>Recently played</Text>
          <Feather name="more-horizontal" size={21} color={colors.mutedForeground} />
        </View>
        {songs.slice(2, 5).map((song) => (
          <Pressable
            key={song.id}
            onPress={() => playTrack(song)}
            style={({ pressed }) => [styles.row, { borderBottomColor: colors.border, opacity: pressed ? 0.7 : 1 }]}
          >
            <Image source={song.cover} style={styles.rowImage} />
            <View style={styles.rowInfo}>
              <Text numberOfLines={1} style={[styles.rowTitle, { color: colors.foreground }]}>{song.title}</Text>
              <Text numberOfLines={1} style={[styles.rowMeta, { color: colors.mutedForeground }]}>{song.artist} · {song.album}</Text>
            </View>
            <Feather name="play-circle" size={22} color={colors.primary} />
          </Pressable>
        ))}
      </ScrollView>
      <View style={[styles.miniWrap, { bottom: insets.bottom + 78 }]}>
        <MiniPlayer />
      </View>
    </View>
  );
}

function GreenroomHome() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { mode, toggleMode, toggleDesign } = useTheme();
  const { playTrack } = useMusic();
  const gridSongs = songs.slice(0, 4);
  const rotationSongs = [...songs.slice(4), ...songs.slice(0, 2)].slice(0, 4);

  return (
    <View style={[greenStyles.screen, { backgroundColor: colors.background }]}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingTop: insets.top + 18, paddingBottom: 160 }}
      >
        <View style={greenStyles.header}>
          <View>
            <Text style={[greenStyles.eyebrow, { color: colors.mutedForeground }]}>GOOD EVENING</Text>
            <Text style={[greenStyles.title, { color: colors.foreground }]}>Maya</Text>
          </View>
          <View style={greenStyles.actions}>
            <Pressable
              accessibilityLabel={`Switch to ${mode === 'dark' ? 'light' : 'dark'} mode`}
              onPress={toggleMode}
              style={[greenStyles.actionButton, { backgroundColor: colors.card }]}
            >
              <Feather name={mode === 'dark' ? 'sun' : 'moon'} size={17} color={colors.foreground} />
            </Pressable>
            <Pressable
              accessibilityLabel="Switch to the next visual design"
              onPress={toggleDesign}
              style={[greenStyles.profile, { backgroundColor: colors.primary }]}
            >
              <Text style={[greenStyles.profileText, { color: colors.primaryForeground }]}>G</Text>
            </Pressable>
          </View>
        </View>

        <Pressable
          onPress={() => router.push('/search')}
          style={({ pressed }) => [greenStyles.search, { backgroundColor: colors.card, opacity: pressed ? 0.8 : 1 }]}
        >
          <Feather name="search" size={17} color={colors.mutedForeground} />
          <Text style={[greenStyles.searchText, { color: colors.mutedForeground }]}>Search your library</Text>
        </Pressable>

        <View style={greenStyles.heading}>
          <Text style={[greenStyles.headingText, { color: colors.foreground }]}>Jump back in</Text>
          <Pressable onPress={() => router.push('/search')}>
            <Text style={[greenStyles.link, { color: colors.primary }]}>See all</Text>
          </Pressable>
        </View>
        <View style={greenStyles.grid}>
          {gridSongs.map((song) => (
            <Pressable
              key={song.id}
              onPress={() => playTrack(song)}
              style={({ pressed }) => [greenStyles.gridCard, { backgroundColor: colors.card, opacity: pressed ? 0.74 : 1 }]}
            >
              <Image source={song.cover} style={greenStyles.gridImage} />
              <View style={greenStyles.gridCopy}>
                <Text numberOfLines={2} style={[greenStyles.gridTitle, { color: colors.foreground }]}>{song.title}</Text>
                <Feather name="play-circle" size={18} color={colors.primary} />
              </View>
            </Pressable>
          ))}
        </View>

        <View style={greenStyles.heading}>
          <Text style={[greenStyles.headingText, { color: colors.foreground }]}>Made for you</Text>
          <Feather name="more-horizontal" size={20} color={colors.mutedForeground} />
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={greenStyles.albumRail}>
          {albums.map((album) => (
            <Pressable
              key={album.id}
              onPress={() => router.push(`/album/${album.id}`)}
              style={({ pressed }) => [greenStyles.albumCard, { opacity: pressed ? 0.74 : 1 }]}
            >
              <Image source={album.cover} style={greenStyles.albumImage} />
              <Text numberOfLines={1} style={[greenStyles.albumTitle, { color: colors.foreground }]}>{album.title}</Text>
              <Text numberOfLines={1} style={[greenStyles.albumMeta, { color: colors.mutedForeground }]}>{album.artist}</Text>
            </Pressable>
          ))}
        </ScrollView>

        <View style={greenStyles.heading}>
          <Text style={[greenStyles.headingText, { color: colors.foreground }]}>Your rotation</Text>
          <Feather name="more-horizontal" size={20} color={colors.mutedForeground} />
        </View>
        {rotationSongs.map((song, index) => (
          <Pressable
            key={song.id}
            onPress={() => playTrack(song)}
            style={({ pressed }) => [greenStyles.rotationRow, { borderBottomColor: colors.border, opacity: pressed ? 0.74 : 1 }]}
          >
            <Text style={[greenStyles.number, { color: colors.mutedForeground }]}>0{index + 1}</Text>
            <Image source={song.cover} style={greenStyles.rotationImage} />
            <View style={greenStyles.rotationCopy}>
              <Text numberOfLines={1} style={[greenStyles.rotationTitle, { color: colors.foreground }]}>{song.title}</Text>
              <Text numberOfLines={1} style={[greenStyles.rotationMeta, { color: colors.mutedForeground }]}>{song.artist}</Text>
            </View>
            <Feather name="play" size={16} color={colors.primary} />
          </Pressable>
        ))}
      </ScrollView>
      <View style={[greenStyles.miniWrap, { bottom: insets.bottom + 78 }]}>
        <MiniPlayer />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  header: { paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  eyebrow: { fontFamily: 'Inter_700Bold', fontSize: 10, letterSpacing: 1.4, marginBottom: 7 },
  brand: { fontFamily: 'Inter_700Bold', fontSize: 25, letterSpacing: -1.3, marginBottom: 6 },
  headerActions: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  iconButton: { width: 38, height: 38, borderRadius: 19, borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  avatar: { width: 38, height: 38, borderRadius: 19, alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontFamily: 'Inter_700Bold', fontSize: 15 },
  searchBar: { marginHorizontal: 20, marginTop: 22, height: 48, borderRadius: 16, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 15, gap: 10 },
  searchText: { fontFamily: 'Inter_400Regular', fontSize: 13 },
  sectionHeading: { paddingHorizontal: 20, marginTop: 28, marginBottom: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sectionTitle: { fontFamily: 'Inter_700Bold', fontSize: 19, letterSpacing: -0.4 },
  seeAll: { fontFamily: 'Inter_600SemiBold', fontSize: 12 },
  hero: { height: 238, marginHorizontal: 20, borderRadius: 20, overflow: 'hidden' },
  heroImage: { ...StyleSheet.absoluteFill, width: '100%', height: '100%' },
  heroShade: { ...StyleSheet.absoluteFill },
  heroContent: { flex: 1, justifyContent: 'flex-end', padding: 20 },
  heroPill: { alignSelf: 'flex-start', borderRadius: 20, paddingVertical: 6, paddingHorizontal: 9, flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 9 },
  pillDot: { width: 5, height: 5, borderRadius: 3 },
  heroKicker: { fontFamily: 'Inter_700Bold', fontSize: 9, letterSpacing: 1.5 },
  heroTitle: { color: '#F5F5F7', fontFamily: 'Inter_700Bold', fontSize: 34, letterSpacing: -1.2 },
  heroSubtitle: { color: '#D8D6DF', fontFamily: 'Inter_400Regular', fontSize: 13, marginTop: 4, marginBottom: 14 },
  heroButton: { alignSelf: 'flex-start', borderRadius: 20, paddingVertical: 10, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 7 },
  heroButtonText: { fontFamily: 'Inter_700Bold', fontSize: 12 },
  horizontalList: { paddingHorizontal: 20, gap: 14 },
  pick: { width: 118 },
  pickImage: { width: 118, height: 118, borderRadius: 12, marginBottom: 9 },
  pickTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 13 },
  pickArtist: { fontFamily: 'Inter_400Regular', fontSize: 11, marginTop: 4 },
  row: { marginHorizontal: 20, minHeight: 68, borderBottomWidth: 1, flexDirection: 'row', alignItems: 'center', gap: 12 },
  rowImage: { width: 48, height: 48, borderRadius: 8 },
  rowInfo: { flex: 1, gap: 5 },
  rowTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 14 },
  rowMeta: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  miniWrap: { position: 'absolute', left: 16, right: 16 },
});

const greenStyles = StyleSheet.create({
  screen: { flex: 1 },
  header: { paddingHorizontal: 20, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  eyebrow: { fontFamily: 'Inter_700Bold', fontSize: 10, letterSpacing: 1.6, marginBottom: 6 },
  title: { fontFamily: 'Inter_700Bold', fontSize: 30, letterSpacing: -0.9 },
  actions: { flexDirection: 'row', alignItems: 'center', gap: 9 },
  actionButton: { width: 38, height: 38, borderRadius: 19, alignItems: 'center', justifyContent: 'center' },
  profile: { width: 38, height: 38, borderRadius: 19, alignItems: 'center', justifyContent: 'center' },
  profileText: { fontFamily: 'Inter_700Bold', fontSize: 14 },
  search: { height: 48, borderRadius: 8, marginHorizontal: 20, marginTop: 24, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', gap: 10 },
  searchText: { fontFamily: 'Inter_400Regular', fontSize: 13 },
  heading: { marginHorizontal: 20, marginTop: 30, marginBottom: 13, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  headingText: { fontFamily: 'Inter_700Bold', fontSize: 20, letterSpacing: -0.4 },
  link: { fontFamily: 'Inter_600SemiBold', fontSize: 12 },
  grid: { marginHorizontal: 20, flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  gridCard: { width: '48.5%', minHeight: 70, borderRadius: 7, overflow: 'hidden', flexDirection: 'row', alignItems: 'center' },
  gridImage: { width: 70, height: 70 },
  gridCopy: { flex: 1, minHeight: 70, paddingHorizontal: 9, paddingVertical: 10, justifyContent: 'space-between', alignItems: 'flex-start' },
  gridTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 12, lineHeight: 16 },
  albumRail: { paddingHorizontal: 20, gap: 16 },
  albumCard: { width: 145 },
  albumImage: { width: 145, height: 145, borderRadius: 8, marginBottom: 9 },
  albumTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 13 },
  albumMeta: { fontFamily: 'Inter_400Regular', fontSize: 11, marginTop: 4 },
  rotationRow: { minHeight: 66, marginHorizontal: 20, borderBottomWidth: 1, flexDirection: 'row', alignItems: 'center', gap: 11 },
  number: { width: 22, fontFamily: 'Inter_500Medium', fontSize: 11 },
  rotationImage: { width: 42, height: 42, borderRadius: 4 },
  rotationCopy: { flex: 1, gap: 4 },
  rotationTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 13 },
  rotationMeta: { fontFamily: 'Inter_400Regular', fontSize: 11 },
  miniWrap: { position: 'absolute', left: 16, right: 16 },
});