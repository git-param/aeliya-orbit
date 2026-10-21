import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: {
      main: '#4A8B8C', // Muted teal
    },
    background: {
      default: '#F7F5F2', // Warm neutral
      paper: '#FFFFFF',
    },
    text: {
      primary: '#2A2A2A', // Charcoal
    }
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  }
});