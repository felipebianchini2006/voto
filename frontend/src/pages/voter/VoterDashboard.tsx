import React from 'react';
import {
    Box,
    Typography,
    Grid,
    Card,
    CardContent,
    CardActions,
    Button,
    Chip
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { electionService } from '../services/electionService';
import { Election } from '../types';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { useNavigate } from 'react-router-dom';

export const VoterDashboard: React.FC = () => {
    const navigate = useNavigate();
    const { data: elections, isLoading } = useQuery({
        queryKey: ['elections-active'],
        queryFn: electionService.getAll // Ideally filter for active/upcoming
    });

    if (isLoading) return <Typography>Carregando eleições...</Typography>;

    return (
        <Box>
            <Typography variant="h4" gutterBottom>
                Eleições Disponíveis
            </Typography>

            <Grid container spacing={3}>
                {elections?.map((election: Election) => (
                    <Grid item xs={12} md={6} lg={4} key={election.id}>
                        <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                            <CardContent sx={{ flexGrow: 1 }}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                                    <Chip
                                        label={election.status}
                                        color={election.status === 'RUNNING' ? 'success' : 'default'}
                                        size="small"
                                    />
                                </Box>
                                <Typography variant="h5" component="div" gutterBottom>
                                    {election.name}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" paragraph>
                                    {election.description}
                                </Typography>
                                <Typography variant="caption" display="block">
                                    Encerra em: {format(new Date(election.endTs), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                                </Typography>
                            </CardContent>
                            <CardActions>
                                {election.status === 'RUNNING' ? (
                                    <Button
                                        size="small"
                                        variant="contained"
                                        fullWidth
                                        onClick={() => navigate(`/vote/${election.id}`)}
                                    >
                                        Votar Agora
                                    </Button>
                                ) : (
                                    <Button size="small" disabled fullWidth>
                                        Indisponível
                                    </Button>
                                )}
                            </CardActions>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
};
