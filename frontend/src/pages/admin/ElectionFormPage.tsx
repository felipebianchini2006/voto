import React, { useState } from 'react';
import {
    Box,
    Typography,
    TextField,
    Button,
    Paper,
    FormControlLabel,
    Switch,
    Grid
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { electionService } from '../../services/electionService';
import type { CreateElectionRequest } from '../../types';

export const ElectionFormPage: React.FC = () => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const [formData, setFormData] = useState<CreateElectionRequest>({
        name: '',
        description: '',
        startTs: '',
        endTs: '',
        maxVotesPerVoter: 1,
        allowAbstention: true
    });

    const createMutation = useMutation({
        mutationFn: electionService.create,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['elections'] });
            navigate('/admin');
        }
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        // Format dates to ISO string if needed, but input type="datetime-local" usually needs handling
        // For simplicity assuming backend accepts ISO format
        const payload = {
            ...formData,
            startTs: new Date(formData.startTs).toISOString(),
            endTs: new Date(formData.endTs).toISOString()
        };
        createMutation.mutate(payload);
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    return (
        <Box maxWidth="md" margin="auto">
            <Typography variant="h4" gutterBottom>
                Nova Eleição
            </Typography>

            <Paper sx={{ p: 4 }}>
                <form onSubmit={handleSubmit}>
                    <Grid container spacing={3}>
                        <Grid size={12}>
                            <TextField
                                fullWidth
                                label="Nome da Eleição"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                required
                            />
                        </Grid>

                        <Grid size={12}>
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

                        <Grid size={{ xs: 12, md: 6 }}>
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

                        <Grid size={{ xs: 12, md: 6 }}>
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

                        <Grid size={{ xs: 12, md: 6 }}>
                            <TextField
                                fullWidth
                                type="number"
                                label="Votos por Eleitor"
                                name="maxVotesPerVoter"
                                value={formData.maxVotesPerVoter}
                                onChange={handleChange}
                                required
                            />
                        </Grid>

                        <Grid size={{ xs: 12, md: 6 }}>
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

                        <Grid size={12} sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                            <Button variant="outlined" onClick={() => navigate('/admin')}>
                                Cancelar
                            </Button>
                            <Button
                                type="submit"
                                variant="contained"
                                disabled={createMutation.isPending}
                            >
                                Criar Eleição
                            </Button>
                        </Grid>
                    </Grid>
                </form>
            </Paper>
        </Box>
    );
};
