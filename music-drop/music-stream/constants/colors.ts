export type ThemeMode = 'dark' | 'light';
export type DesignMode = 'pulse' | 'orbit' | 'greenroom';

export type ColorPalette = {
  text: string;
  tint: string;
  background: string;
  foreground: string;
  card: string;
  cardForeground: string;
  primary: string;
  primaryForeground: string;
  secondary: string;
  secondaryForeground: string;
  muted: string;
  mutedForeground: string;
  accent: string;
  accentForeground: string;
  destructive: string;
  destructiveForeground: string;
  border: string;
  input: string;
};

const colors: Record<DesignMode, Record<ThemeMode, ColorPalette>> = {
  pulse: {
    dark: {
      text: '#F5F5F7',
      tint: '#D6FF4B',
      background: '#09090B',
      foreground: '#F5F5F7',
      card: '#15151A',
      cardForeground: '#F5F5F7',
      primary: '#D6FF4B',
      primaryForeground: '#09090B',
      secondary: '#202027',
      secondaryForeground: '#F5F5F7',
      muted: '#23232B',
      mutedForeground: '#A4A2B0',
      accent: '#8E7CFF',
      accentForeground: '#F5F5F7',
      destructive: '#FF6378',
      destructiveForeground: '#09090B',
      border: '#2D2D38',
      input: '#2D2D38',
    },
    light: {
      text: '#1B1A22',
      tint: '#5B49D8',
      background: '#F7F6F2',
      foreground: '#1B1A22',
      card: '#FFFFFF',
      cardForeground: '#1B1A22',
      primary: '#5B49D8',
      primaryForeground: '#FFFFFF',
      secondary: '#ECEAF7',
      secondaryForeground: '#2A2458',
      muted: '#EAE8E1',
      mutedForeground: '#777481',
      accent: '#FFB24A',
      accentForeground: '#1B1A22',
      destructive: '#D93F59',
      destructiveForeground: '#FFFFFF',
      border: '#E4E1D9',
      input: '#E4E1D9',
    },
  },
  orbit: {
    dark: {
      text: '#EBFFFC',
      tint: '#29D3C2',
      background: '#071A1C',
      foreground: '#EBFFFC',
      card: '#102B2D',
      cardForeground: '#EBFFFC',
      primary: '#29D3C2',
      primaryForeground: '#071A1C',
      secondary: '#173A3B',
      secondaryForeground: '#EBFFFC',
      muted: '#153133',
      mutedForeground: '#91B5B2',
      accent: '#FF8066',
      accentForeground: '#071A1C',
      destructive: '#FF8066',
      destructiveForeground: '#071A1C',
      border: '#255052',
      input: '#255052',
    },
    light: {
      text: '#102021',
      tint: '#147D77',
      background: '#F2FFFB',
      foreground: '#102021',
      card: '#FFFFFF',
      cardForeground: '#102021',
      primary: '#147D77',
      primaryForeground: '#FFFFFF',
      secondary: '#DDF5F0',
      secondaryForeground: '#103B39',
      muted: '#E4F2EF',
      mutedForeground: '#5C7774',
      accent: '#FF785D',
      accentForeground: '#FFFFFF',
      destructive: '#D94E58',
      destructiveForeground: '#FFFFFF',
      border: '#CDE4DF',
      input: '#CDE4DF',
    },
  },
  greenroom: {
    dark: {
      text: '#F5F5F5',
      tint: '#1ED760',
      background: '#0B0B0B',
      foreground: '#F5F5F5',
      card: '#181818',
      cardForeground: '#F5F5F5',
      primary: '#1ED760',
      primaryForeground: '#071108',
      secondary: '#282828',
      secondaryForeground: '#F5F5F5',
      muted: '#202020',
      mutedForeground: '#A7A7A7',
      accent: '#B7F35B',
      accentForeground: '#071108',
      destructive: '#F15E6C',
      destructiveForeground: '#0B0B0B',
      border: '#383838',
      input: '#383838',
    },
    light: {
      text: '#161616',
      tint: '#138A3D',
      background: '#F7FAF7',
      foreground: '#161616',
      card: '#FFFFFF',
      cardForeground: '#161616',
      primary: '#138A3D',
      primaryForeground: '#FFFFFF',
      secondary: '#DDF3E3',
      secondaryForeground: '#123C23',
      muted: '#EAF4EC',
      mutedForeground: '#647267',
      accent: '#7AAE2F',
      accentForeground: '#FFFFFF',
      destructive: '#C94152',
      destructiveForeground: '#FFFFFF',
      border: '#D6E4D9',
      input: '#D6E4D9',
    },
  },
};

export default colors;