import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { ThemeProvider, CssBaseline, Box, Typography, Paper, AppBar, Toolbar } from '@mui/material';
import { theme } from './theme';
import axios from 'axios';

const queryClient = new QueryClient();

function HealthCheck() {
  const { data, error, isLoading } = useQuery({
    queryKey: ['health'],
    queryFn: () => axios.get('/api/health').then(res => res.data)
  });

  return (
    <Paper sx={{ p: 3, mt: 4, maxWidth: 600, mx: 'auto' }}>
      <Typography variant="h6" gutterBottom>System Status</Typography>
      {isLoading && <Typography>Connecting to backend...</Typography>}
      {error && <Typography color="error">Backend connection failed. Ensure Spring Boot is running.</Typography>}
      {data && (
        <Typography color="success.main">
          Connected! Backend reports: {data.status} ({data.service})
        </Typography>
      )}
    </Paper>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box sx={{ flexGrow: 1, minHeight: '100vh' }}>
          <AppBar position="static" sx={{ backgroundColor: 'text.primary' }}>
            <Toolbar>
              <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                Aeliya Orbit - Development Foundation
              </Typography>
            </Toolbar>
          </AppBar>
          <Box p={3}>
            <HealthCheck />
          </Box>
        </Box>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;