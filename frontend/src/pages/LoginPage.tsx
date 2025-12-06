import React, { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Container,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { Lock } from '@mui/icons-material';

export const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [role, setRole] = useState('ROLE_VOTER');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (!username.trim()) {
      setError('Por favor, informe um nome de usuário');
      return;
    }

    // Simulação de login - em produção isso chamaria um endpoint OAuth2
    const mockToken = `mock-jwt-token-${Date.now()}`;
    const mockUser = {
      username,
      roles: [role],
    };

    try {
      login(mockToken, mockUser);

      if (role === 'ROLE_ADMIN') {
        navigate('/admin');
      } else if (role === 'ROLE_AUDITOR') {
        navigate('/audit');
      } else {
        navigate('/elections');
      }
    } catch (err) {
      setError('Erro ao fazer login. Tente novamente.');
    }
  };

  return (
    <Container maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}>
            <Box
              sx={{
                m: 1,
                bgcolor: 'primary.main',
                p: 1,
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 48,
                height: 48,
              }}
            >
              <Lock sx={{ color: 'white' }} />
            </Box>
            <Typography component="h1" variant="h5" sx={{ mt: 2 }}>
              Entrar no Sistema
            </Typography>
          </Box>

          <Alert severity="info" sx={{ mb: 2 }}>
            Ambiente de Demonstração: Selecione seu papel.
          </Alert>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleLogin} sx={{ mt: 1, width: '100%' }}>
            <TextField
              margin="normal"
              required
              fullWidth
              id="username"
              label="Usuário"
              name="username"
              autoComplete="username"
              autoFocus
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <FormControl fullWidth margin="normal">
              <InputLabel>Papel</InputLabel>
              <Select
                value={role}
                label="Papel"
                onChange={(e) => setRole(e.target.value)}
              >
                <MenuItem value="ROLE_VOTER">Eleitor</MenuItem>
                <MenuItem value="ROLE_ADMIN">Administrador</MenuItem>
                <MenuItem value="ROLE_AUDITOR">Auditor</MenuItem>
              </Select>
            </FormControl>

            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              size="large"
            >
              Entrar
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};
