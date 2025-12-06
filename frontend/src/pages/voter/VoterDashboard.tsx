import React from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  CircularProgress,
  Alert,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { electionService } from '../../services/electionService';
import type { Election } from '../../types';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { useNavigate } from 'react-router-dom';
import Grid from '@mui/material/GridLegacy';

export const VoterDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { data: elections, isLoading, error } = useQuery<Election[] | { content?: Election[] }>(
    {
      queryKey: ['elections-active'],
      queryFn: electionService.getAll,
    }
  );

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        Erro ao carregar eleições. Tente novamente.
      </Alert>
    );
  }

  const electionsList: Election[] = Array.isArray(elections)
    ? elections
    : (elections as { content?: Election[] } | undefined)?.content ?? [];
  const activeElections = electionsList.filter(
    (e: Election) => e.status === 'RUNNING' || e.status === 'DRAFT'
  );

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Eleições Disponíveis
      </Typography>

      {activeElections.length === 0 ? (
        <Alert severity="info">
          Nenhuma eleição disponível no momento.
        </Alert>
      ) : (
        <Grid container spacing={3}>
          {activeElections.map((election: Election) => (
            <Grid item xs={12} md={6} lg={4} key={election.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                    <Chip
                      label={election.status}
                      color={election.status === 'RUNNING' ? 'success' : 'warning'}
                      size="small"
                    />
                  </Box>
                  <Typography variant="h6" component="div" gutterBottom>
                    {election.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" paragraph>
                    {election.description || 'Sem descrição'}
                  </Typography>
                  <Typography variant="caption" display="block">
                    Início: {format(new Date(election.startTs), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                  </Typography>
                  <Typography variant="caption" display="block">
                    Fim: {format(new Date(election.endTs), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
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
      )}
    </Box>
  );
};
