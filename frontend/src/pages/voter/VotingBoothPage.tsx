import React, { useState } from 'react';
import {
    Box,
    Typography,
    Stepper,
    Step,
    StepLabel,
    Button,
    Paper,
    TextField,
    Grid,
    Card,
    CardActionArea,
    CardContent,
    Alert
} from '@mui/material';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { electionService } from '../../services/electionService';
import { votingService } from '../../services/votingService';
import type { Candidate } from '../../types';

const steps = ['Identificação', 'Seleção', 'Confirmação', 'Comprovante'];

export const VotingBoothPage: React.FC = () => {
    const { electionId } = useParams<{ electionId: string }>();
    const navigate = useNavigate();
    const [activeStep, setActiveStep] = useState(0);
    const [externalId, setExternalId] = useState('');
    const [token, setToken] = useState<string | null>(null);
    const [selectedCandidate, setSelectedCandidate] = useState<string | null>(null);
    const [receipt, setReceipt] = useState<string | null>(null);

    const { data: candidates } = useQuery({
        queryKey: ['candidates', electionId],
        queryFn: () => electionService.getCandidates(electionId!)
    });

    const tokenMutation = useMutation({
        mutationFn: () => votingService.requestToken(electionId!, externalId),
        onSuccess: (data) => {
            setToken(data.tokenValue);
            setActiveStep(1);
        }
    });

    const voteMutation = useMutation({
        mutationFn: () => votingService.castVote(electionId!, token!, selectedCandidate!),
        onSuccess: (data) => {
            setReceipt(data.ballotHash);
            setActiveStep(3);
        }
    });

    const handleNext = () => {
        if (activeStep === 0) {
            tokenMutation.mutate();
        } else if (activeStep === 1) {
            setActiveStep(2);
        } else if (activeStep === 2) {
            voteMutation.mutate();
        }
    };

    const handleBack = () => {
        setActiveStep((prev) => prev - 1);
    };

    return (
        <Box maxWidth="lg" margin="auto">
            <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
                {steps.map((label) => (
                    <Step key={label}>
                        <StepLabel>{label}</StepLabel>
                    </Step>
                ))}
            </Stepper>

            <Paper sx={{ p: 4, minHeight: '400px' }}>
                {activeStep === 0 && (
                    <Box sx={{ maxWidth: 400, margin: 'auto', textAlign: 'center' }}>
                        <Typography variant="h6" gutterBottom>
                            Identificação do Eleitor
                        </Typography>
                        <Typography paragraph color="text.secondary">
                            Informe seu ID externo (CPF/Matrícula) para receber o token de votação.
                        </Typography>
                        <TextField
                            fullWidth
                            label="ID Externo"
                            value={externalId}
                            onChange={(e) => setExternalId(e.target.value)}
                            sx={{ mb: 2 }}
                        />
                        <Button
                            variant="contained"
                            onClick={handleNext}
                            disabled={!externalId || tokenMutation.isPending}
                        >
                            Solicitar Token
                        </Button>
                    </Box>
                )}

                {activeStep === 1 && (
                    <Box>
                        <Typography variant="h6" gutterBottom align="center">
                            Escolha seu Candidato
                        </Typography>
                        <Grid container spacing={2}>
                            {candidates?.map((candidate: Candidate) => (
                                <Grid size={{ xs: 12, sm: 6, md: 4 }} key={candidate.id}>
                                    <Card
                                        sx={{
                                            border: selectedCandidate === candidate.id ? '2px solid #1976d2' : 'none',
                                            height: '100%'
                                        }}
                                    >
                                        <CardActionArea
                                            onClick={() => setSelectedCandidate(candidate.id)}
                                            sx={{ height: '100%', p: 2 }}
                                        >
                                            <CardContent sx={{ textAlign: 'center' }}>
                                                <Typography variant="h3" color="primary" gutterBottom>
                                                    {candidate.ballotNumber}
                                                </Typography>
                                                <Typography variant="h6">
                                                    {candidate.name}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    {candidate.description}
                                                </Typography>
                                            </CardContent>
                                        </CardActionArea>
                                    </Card>
                                </Grid>
                            ))}
                        </Grid>
                        <Box sx={{ mt: 4, display: 'flex', justifyContent: 'flex-end' }}>
                            <Button onClick={handleBack} sx={{ mr: 1 }}>Voltar</Button>
                            <Button
                                variant="contained"
                                onClick={handleNext}
                                disabled={!selectedCandidate}
                            >
                                Continuar
                            </Button>
                        </Box>
                    </Box>
                )}

                {activeStep === 2 && (
                    <Box sx={{ maxWidth: 500, margin: 'auto', textAlign: 'center' }}>
                        <Typography variant="h6" gutterBottom>
                            Confirme seu Voto
                        </Typography>
                        <Alert severity="warning" sx={{ mb: 3 }}>
                            Esta ação não poderá ser desfeita. Seu voto será criptografado e enviado.
                        </Alert>

                        <Typography variant="body1" gutterBottom>
                            Você está votando em:
                        </Typography>

                        {selectedCandidate && (
                            <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: '#f5f5f5' }}>
                                <Typography variant="h5" color="primary">
                                    {candidates?.find(c => c.id === selectedCandidate)?.name}
                                </Typography>
                                <Typography variant="h4">
                                    {candidates?.find(c => c.id === selectedCandidate)?.ballotNumber}
                                </Typography>
                            </Paper>
                        )}

                        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2 }}>
                            <Button onClick={handleBack}>Corrigir</Button>
                            <Button
                                variant="contained"
                                color="success"
                                size="large"
                                onClick={handleNext}
                                disabled={voteMutation.isPending}
                            >
                                CONFIRMAR VOTO
                            </Button>
                        </Box>
                    </Box>
                )}

                {activeStep === 3 && (
                    <Box sx={{ textAlign: 'center' }}>
                        <Typography variant="h5" color="success.main" gutterBottom>
                            Voto Registrado com Sucesso!
                        </Typography>
                        <Typography paragraph>
                            Seu voto foi criptografado e armazenado na urna eletrônica.
                        </Typography>

                        <Paper sx={{ p: 3, bgcolor: '#e3f2fd', mb: 3, wordBreak: 'break-all' }}>
                            <Typography variant="subtitle2" gutterBottom>
                                Seu Comprovante (Hash do Voto):
                            </Typography>
                            <Typography variant="body2" sx={{ fontFamily: 'monospace', fontWeight: 'bold' }}>
                                {receipt}
                            </Typography>
                        </Paper>

                        <Typography variant="caption" display="block" paragraph>
                            Guarde este hash para verificar se seu voto foi contabilizado na auditoria.
                        </Typography>

                        <Button variant="contained" onClick={() => navigate('/')}>
                            Voltar ao Início
                        </Button>
                    </Box>
                )}
            </Paper>
        </Box>
    );
};
