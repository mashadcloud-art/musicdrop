import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useColorScheme } from 'react-native';
import colors, { type DesignMode, type ThemeMode } from '@/constants/colors';

type ThemeContextValue = {
  mode: ThemeMode;
  design: DesignMode;
  toggleMode: () => void;
  toggleDesign: () => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);
const MODE_KEY = '@music-stream/theme-mode';
const DESIGN_KEY = '@music-stream/design-mode';

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const systemMode = useColorScheme() === 'light' ? 'light' : 'dark';
  const [mode, setMode] = useState<ThemeMode>(systemMode);
  const [design, setDesign] = useState<DesignMode>('pulse');
  const [hydrated, setHydrated] = useState<boolean>(false);

  useEffect(() => {
    Promise.all([AsyncStorage.getItem(MODE_KEY), AsyncStorage.getItem(DESIGN_KEY)]).then(([storedMode, storedDesign]) => {
      if (storedMode === 'light' || storedMode === 'dark') setMode(storedMode);
      if (storedDesign === 'pulse' || storedDesign === 'orbit' || storedDesign === 'greenroom') setDesign(storedDesign);
      setHydrated(true);
    });
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    void AsyncStorage.multiSet([[MODE_KEY, mode], [DESIGN_KEY, design]]);
  }, [design, hydrated, mode]);

  const value = useMemo<ThemeContextValue>(
    () => ({
      mode,
      design,
      toggleMode: () => setMode((current) => current === 'dark' ? 'light' : 'dark'),
      toggleDesign: () => setDesign((current) => current === 'pulse' ? 'orbit' : current === 'orbit' ? 'greenroom' : 'pulse'),
    }),
    [design, mode],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used inside ThemeProvider');
  return context;
}

export function getPalette(mode: ThemeMode, design: DesignMode) {
  return colors[design][mode];
}