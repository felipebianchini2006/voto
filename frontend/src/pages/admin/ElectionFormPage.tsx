import React, { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  FormControlLabel,
  Switch,
  Alert,
  CircularProgress,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { electionService } from '../../services/electionService';
import type { CreateElectionRequest } from '../../types';
import Grid from '@mui/material/GridLegacy';

export const ElectionFormPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');

  const [formData, setFormData] = useState<CreateElectionRequest>({
    name: '',
    description: '',
    startTs: '',
    endTs: '',
    maxVotesPerVoter: 1,
    allowAbstention: true,
  });

  const createMutation = useMutation({
    mutationFn: electionService.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['elections'] });
      navigate('/admin');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Erro ao criar eleição');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.name.trim()) {
      setError('Nome da eleição é obrigatório');
      return;
    }

    if (!formData.startTs || !formData.endTs) {
      setError('Datas de início e fim são obrigatórias');
      return;
    }

    const startDate = new Date(formData.startTs);
    const endDate = new Date(formData.endTs);

    if (endDate <= startDate) {
      setError('Data de fim deve ser posterior à data de início');
      return;
    }

    const payload = {
      ...formData,
      startTs: startDate.toISOString(),
      endTs: endDate.toISOString(),
    };

    createMutation.mutate(payload);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  return (
    <Box maxWidth="md" margin="auto">
      <Typography variant="h4" gutterBottom>
        Nova Eleição
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ p: 4 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Nome da Eleição"
                name="name"
                value={formData.name}
                onChange={handleChange}
                required
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Descrição"
                name="description"
                multiline
                rows={3}
                value={formData.description}
                onChange={handleChange}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Início"
                name="startTs"
                InputLabelProps={{ shrink: true }}
                value={formData.startTs}
                onChange={handleChange}
                required
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="datetime-local"
                label="Fim"
                name="endTs"
                InputLabelProps={{ shrink: true }}
                value={formData.endTs}
                onChange={handleChange}
                required
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="number"
                label="Votos por Eleitor"
                name="maxVotesPerVoter"
                value={formData.maxVotesPerVoter}
                onChange={handleChange}
                inputProps={{ min: 1 }}
                required
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.allowAbstention}
                    onChange={handleChange}
                    name="allowAbstention"
                  />
                }
                label="Permitir Abstenção"
              />
            </Grid>

            <Grid item xs={12} sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
              <Button variant="outlined" onClick={() => navigate('/admin')}>
                Cancelar
              </Button>
              <Button
                type="submit"
                variant="contained"
                disabled={createMutation.isPending}
                startIcon={createMutation.isPending ? <CircularProgress size={20} /> : null}
              >
                {createMutation.isPending ? 'Criando...' : 'Criar Eleição'}
              </Button>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};
