import React, { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Container,
  Alert,
  Link as MuiLink,
  CircularProgress,
  Stack,
} from '@mui/material';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { authService } from '../services/authService';
import { PersonAdd } from '@mui/icons-material';

export const RegisterPage: React.FC = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    fullName: '',
    email: '',
    description: '',
    party: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Validações
    if (!formData.username.trim() || !formData.password || !formData.fullName.trim() || !formData.email.trim()) {
      setError('Por favor, preencha todos os campos obrigatórios');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('As senhas não coincidem');
      return;
    }

    if (formData.password.length < 8) {
      setError('A senha deve ter pelo menos 8 caracteres');
      return;
    }

    if (!/[A-Z]/.test(formData.password)) {
      setError('A senha deve conter pelo menos uma letra maiúscula');
      return;
    }

    if (!/[a-z]/.test(formData.password)) {
      setError('A senha deve conter pelo menos uma letra minúscula');
      return;
    }

    if (!/[0-9]/.test(formData.password)) {
      setError('A senha deve conter pelo menos um número');
      return;
    }

    setLoading(true);
    try {
      const response = await authService.registerCandidate({
        username: formData.username,
        password: formData.password,
        fullName: formData.fullName,
        email: formData.email,
        description: formData.description || undefined,
        party: formData.party || undefined,
      });

      login(response.token, response.user);
      navigate('/candidate');
    } catch (err: any) {
      console.error('Registration error:', err);
      setError(
        err.response?.data?.message ||
        'Erro ao registrar candidato. Tente novamente.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm">
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
                bgcolor: 'secondary.main',
                p: 1,
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 48,
                height: 48,
              }}
            >
              <PersonAdd sx={{ color: 'white' }} />
            </Box>
            <Typography component="h1" variant="h5" sx={{ mt: 2 }}>
              Registro de Candidato
            </Typography>
          </Box>

          <Alert severity="info" sx={{ mb: 2 }}>
            Registre-se para se candidatar a eleições
          </Alert>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
            <Stack spacing={2}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  required
                  fullWidth
                  id="username"
                  label="Nome de Usuário"
                  name="username"
                  autoComplete="username"
                  autoFocus
                  value={formData.username}
                  onChange={handleChange}
                  disabled={loading}
                />

                <TextField
                  required
                  fullWidth
                  id="email"
                  label="E-mail"
                  name="email"
                  type="email"
                  autoComplete="email"
                  value={formData.email}
                  onChange={handleChange}
                  disabled={loading}
                />
              </Stack>

              <TextField
                required
                fullWidth
                id="fullName"
                label="Nome Completo"
                name="fullName"
                autoComplete="name"
                value={formData.fullName}
                onChange={handleChange}
                disabled={loading}
              />

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField
                  required
                  fullWidth
                  name="password"
                  label="Senha"
                  type="password"
                  id="password"
                  autoComplete="new-password"
                  value={formData.password}
                  onChange={handleChange}
                  disabled={loading}
                  helperText="Mín. 8 caracteres, maiúscula, minúscula e número"
                />

                <TextField
                  required
                  fullWidth
                  name="confirmPassword"
                  label="Confirmar Senha"
                  type="password"
                  id="confirmPassword"
                  autoComplete="new-password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  disabled={loading}
                />
              </Stack>

              <TextField
                fullWidth
                id="party"
                label="Partido (Opcional)"
                name="party"
                value={formData.party}
                onChange={handleChange}
                disabled={loading}
              />

              <TextField
                fullWidth
                id="description"
                label="Descrição / Biografia (Opcional)"
                name="description"
                multiline
                rows={3}
                value={formData.description}
                onChange={handleChange}
                disabled={loading}
              />
            </Stack>

            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              size="large"
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Registrar'}
            </Button>

            <Box sx={{ textAlign: 'center', mt: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Já tem uma conta?{' '}
                <MuiLink component={Link} to="/login" underline="hover">
                  Faça login aqui
                </MuiLink>
              </Typography>
            </Box>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};
