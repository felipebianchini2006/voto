import React from 'react';
import { Typography, Paper, Box, Button, Grid } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { ShieldCheck, Vote, BarChart3 } from 'lucide-react';

export const HomePage: React.FC = () => {
    return (
        <Box>
            <Box sx={{ textAlign: 'center', mb: 8 }}>
                <Typography variant="h2" component="h1" gutterBottom>
                    Votação Eletrônica Segura
                </Typography>
                <Typography variant="h5" color="text.secondary" paragraph>
                    Sistema de votação anônima, auditável e verificável.
                </Typography>
                <Box sx={{ mt: 4 }}>
                    <Button variant="contained" size="large" component={RouterLink} to="/elections" sx={{ mr: 2 }}>
                        Ver Eleições
                    </Button>
                    <Button variant="outlined" size="large" component={RouterLink} to="/verify">
                        Verificar Voto
                    </Button>
                </Box>
            </Box>

            <Grid container spacing={4}>
                <Grid item xs={12} md={4}>
                    <Paper sx={{ p: 3, height: '100%', textAlign: 'center' }}>
                        <ShieldCheck size={48} color="#1976d2" style={{ marginBottom: '16px' }} />
                        <Typography variant="h6" gutterBottom>
                            Segurança Total
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                            Criptografia de ponta a ponta e anonimato garantido por Blind Tokens.
                        </Typography>
                    </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                    <Paper sx={{ p: 3, height: '100%', textAlign: 'center' }}>
                        <Vote size={48} color="#1976d2" style={{ marginBottom: '16px' }} />
                        <Typography variant="h6" gutterBottom>
                            Voto Verificável
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                            Receba um comprovante digital e verifique se seu voto foi contabilizado.
                        </Typography>
                    </Paper>
                </Grid>
                <Grid item xs={12} md={4}>
                    <Paper sx={{ p: 3, height: '100%', textAlign: 'center' }}>
                        <BarChart3 size={48} color="#1976d2" style={{ marginBottom: '16px' }} />
                        <Typography variant="h6" gutterBottom>
                            Auditoria Transparente
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                            Resultados auditáveis publicamente com integridade garantida por Hash Chain.
                        </Typography>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};
