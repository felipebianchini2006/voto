import React from 'react';
import { AppBar, Toolbar, Typography, Container, Box, Button } from '@mui/material';
import { Outlet, Link as RouterLink } from 'react-router-dom';
import { Vote } from 'lucide-react';

export const MainLayout: React.FC = () => {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
            <AppBar position="static">
                <Toolbar>
                    <Vote style={{ marginRight: '8px' }} />
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                        Sistema de Votação
                    </Typography>
                    <Button color="inherit" component={RouterLink} to="/">
                        Home
                    </Button>
                    <Button color="inherit" component={RouterLink} to="/admin">
                        Admin
                    </Button>
                </Toolbar>
            </AppBar>
            <Container component="main" sx={{ mt: 4, mb: 4, flexGrow: 1 }}>
                <Outlet />
            </Container>
            <Box component="footer" sx={{ py: 3, px: 2, mt: 'auto', backgroundColor: (theme) => theme.palette.grey[200] }}>
                <Container maxWidth="sm">
                    <Typography variant="body2" color="text.secondary" align="center">
                        © {new Date().getFullYear()} Sistema de Votação Eletrônica Segura
                    </Typography>
                </Container>
            </Box>
        </Box>
    );
};
