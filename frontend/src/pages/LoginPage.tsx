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
    MenuItem
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { Lock } from 'lucide-react';

export const LoginPage: React.FC = () => {
    const [username, setUsername] = useState('');
    const [role, setRole] = useState('ROLE_VOTER');
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleLogin = (e: React.FormEvent) => {
        e.preventDefault();
        // Simulation of login - in production this would hit an OAuth2 endpoint
        // For demo purposes we create a dummy token
        const mockToken = `mock-jwt-token-${Date.now()}`;
        const mockUser = {
            username,
            roles: [role]
        };

        login(mockToken, mockUser);

        if (role === 'ROLE_ADMIN') {
            navigate('/admin');
        } else {
            navigate('/elections');
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
                <Paper elevation={3} sx={{ p: 4, width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <Box sx={{ m: 1, bgcolor: 'primary.main', p: 1, borderRadius: '50%' }}>
                        <Lock color="white" />
                    </Box>
                    <Typography component="h1" variant="h5" sx={{ mb: 3 }}>
                        Entrar no Sistema
                    </Typography>

                    <Alert severity="info" sx={{ mb: 2, width: '100%' }}>
                        Ambiente de Demonstração: Selecione seu papel.
                    </Alert>

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
