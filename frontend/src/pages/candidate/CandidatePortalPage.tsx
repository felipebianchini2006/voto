import React, { useState } from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  Button,
  Card,
  CardContent,
  CardActions,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
} from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { candidateService } from '../../services/candidateService';
import { Edit, ExitToApp, Add } from '@mui/icons-material';
import type { Election, ApplyToElectionRequest, UpdateCandidateProfileRequest, CandidateResponse } from '../../types';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

export const CandidatePortalPage: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [applyDialogOpen, setApplyDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedElection, setSelectedElection] = useState<Election | null>(null);
  const [selectedCandidate, setSelectedCandidate] = useState<CandidateResponse | null>(null);
  const [error, setError] = useState('');

  const [applyForm, setApplyForm] = useState<ApplyToElectionRequest>({
    fullName: '',
    description: '',
    party: '',
    photoUrl: '',
  });

  const [editForm, setEditForm] = useState<UpdateCandidateProfileRequest>({
    fullName: '',
    description: '',
    party: '',
    photoUrl: '',
  });

  const queryClient = useQueryClient();

  const { data: myCandidacies, isLoading: loadingMyCandidacies } = useQuery({
    queryKey: ['myCandidacies'],
    queryFn: candidateService.getMyCandidacies,
  });

  const { data: availableElections, isLoading: loadingAvailable } = useQuery({
    queryKey: ['availableElections'],
    queryFn: candidateService.getAvailableElections,
  });

  const applyMutation = useMutation({
    mutationFn: ({ electionId, data }: { electionId: string; data: ApplyToElectionRequest }) =>
      candidateService.applyToElection(electionId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myCandidacies'] });
      queryClient.invalidateQueries({ queryKey: ['availableElections'] });
      setApplyDialogOpen(false);
      setApplyForm({ fullName: '', description: '', party: '', photoUrl: '' });
      setError('');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Erro ao se candidatar');
    },
  });

  const withdrawMutation = useMutation({
    mutationFn: (electionId: string) => candidateService.withdrawFromElection(electionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myCandidacies'] });
      queryClient.invalidateQueries({ queryKey: ['availableElections'] });
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Erro ao retirar candidatura');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ candidateId, data }: { candidateId: string; data: UpdateCandidateProfileRequest }) =>
      candidateService.updateProfile(candidateId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myCandidacies'] });
      setEditDialogOpen(false);
      setError('');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Erro ao atualizar perfil');
    },
  });

  const handleApplyClick = (election: Election) => {
    setSelectedElection(election);
    setApplyDialogOpen(true);
    setError('');
  };

  const handleEditClick = (candidate: CandidateResponse) => {
    setSelectedCandidate(candidate);
    setEditForm({
      fullName: candidate.name,
      description: candidate.description,
      party: candidate.party,
      photoUrl: candidate.photoUrl,
    });
    setEditDialogOpen(true);
    setError('');
  };

  const handleApplySubmit = () => {
    if (!selectedElection || !applyForm.fullName.trim()) {
      setError('Nome completo é obrigatório');
      return;
    }

    applyMutation.mutate({
      electionId: selectedElection.id,
      data: applyForm,
    });
  };

  const handleUpdateSubmit = () => {
    if (!selectedCandidate) return;

    updateMutation.mutate({
      candidateId: selectedCandidate.id,
      data: editForm,
    });
  };

  const handleWithdraw = (electionId: string) => {
    if (window.confirm('Tem certeza que deseja retirar sua candidatura?')) {
      withdrawMutation.mutate(electionId);
    }
  };

  const getElectionStatusColor = (status: string) => {
    switch (status) {
      case 'DRAFT':
        return 'default';
      case 'RUNNING':
        return 'primary';
      case 'CLOSED':
        return 'success';
      case 'CANCELLED':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ py: 4 }}>
        <Typography variant="h4" gutterBottom>
          Portal do Candidato
        </Typography>

        <Paper sx={{ mt: 3 }}>
          <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
            <Tab label="Minhas Candidaturas" />
            <Tab label="Eleições Disponíveis" />
          </Tabs>

          <TabPanel value={tabValue} index={0}>
            {loadingMyCandidacies ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : myCandidacies && myCandidacies.length > 0 ? (
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                {myCandidacies.map((candidacy) => (
                  <Card key={candidacy.id}>
                      <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                          <Typography variant="h6">{candidacy.name}</Typography>
                          <Chip
                            label={`Nº ${candidacy.ballotNumber}`}
                            color="primary"
                            size="small"
                          />
                        </Box>
                        {candidacy.party && (
                          <Typography variant="body2" color="text.secondary" gutterBottom>
                            Partido: {candidacy.party}
                          </Typography>
                        )}
                        {candidacy.description && (
                          <Typography variant="body2" sx={{ mt: 1 }}>
                            {candidacy.description}
                          </Typography>
                        )}
                      </CardContent>
                      <CardActions>
                        <Button
                          size="small"
                          startIcon={<Edit />}
                          onClick={() => handleEditClick(candidacy)}
                        >
                          Editar Perfil
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          startIcon={<ExitToApp />}
                          onClick={() => handleWithdraw(candidacy.electionId)}
                        >
                          Retirar Candidatura
                        </Button>
                      </CardActions>
                    </Card>
                ))}
              </Box>
            ) : (
              <Alert severity="info">
                Você ainda não se candidatou a nenhuma eleição
              </Alert>
            )}
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            {loadingAvailable ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : availableElections && availableElections.length > 0 ? (
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                {availableElections.map((election) => (
                  <Card key={election.id}>
                      <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                          <Typography variant="h6">{election.name}</Typography>
                          <Chip
                            label={election.status}
                            color={getElectionStatusColor(election.status)}
                            size="small"
                          />
                        </Box>
                        <Typography variant="body2" color="text.secondary">
                          {election.description}
                        </Typography>
                        <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                          Início: {new Date(election.startTs).toLocaleString('pt-BR')}
                        </Typography>
                        <Typography variant="caption" display="block">
                          Fim: {new Date(election.endTs).toLocaleString('pt-BR')}
                        </Typography>
                      </CardContent>
                      <CardActions>
                        <Button
                          size="small"
                          variant="contained"
                          startIcon={<Add />}
                          onClick={() => handleApplyClick(election)}
                        >
                          Candidatar-se
                        </Button>
                      </CardActions>
                    </Card>
                ))}
              </Box>
            ) : (
              <Alert severity="info">
                Não há eleições disponíveis no momento
              </Alert>
            )}
          </TabPanel>
        </Paper>
      </Box>

      {/* Apply Dialog */}
      <Dialog open={applyDialogOpen} onClose={() => setApplyDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Candidatar-se a {selectedElection?.name}</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <TextField
            fullWidth
            required
            label="Nome Completo"
            value={applyForm.fullName}
            onChange={(e) => setApplyForm({ ...applyForm, fullName: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            label="Partido (Opcional)"
            value={applyForm.party}
            onChange={(e) => setApplyForm({ ...applyForm, party: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            label="URL da Foto (Opcional)"
            value={applyForm.photoUrl}
            onChange={(e) => setApplyForm({ ...applyForm, photoUrl: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            multiline
            rows={3}
            label="Descrição / Proposta (Opcional)"
            value={applyForm.description}
            onChange={(e) => setApplyForm({ ...applyForm, description: e.target.value })}
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApplyDialogOpen(false)}>Cancelar</Button>
          <Button
            onClick={handleApplySubmit}
            variant="contained"
            disabled={applyMutation.isPending}
          >
            {applyMutation.isPending ? <CircularProgress size={24} /> : 'Candidatar'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Editar Perfil</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <TextField
            fullWidth
            label="Nome Completo"
            value={editForm.fullName}
            onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            label="Partido"
            value={editForm.party}
            onChange={(e) => setEditForm({ ...editForm, party: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            label="URL da Foto"
            value={editForm.photoUrl}
            onChange={(e) => setEditForm({ ...editForm, photoUrl: e.target.value })}
            margin="normal"
          />

          <TextField
            fullWidth
            multiline
            rows={3}
            label="Descrição / Proposta"
            value={editForm.description}
            onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancelar</Button>
          <Button
            onClick={handleUpdateSubmit}
            variant="contained"
            disabled={updateMutation.isPending}
          >
            {updateMutation.isPending ? <CircularProgress size={24} /> : 'Salvar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};
