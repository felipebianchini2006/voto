import React from 'react';
import {
    Box,
    Typography,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Chip,
    Button,
    Alert,
    Collapse
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { auditService, AuditLogEntry } from '../../services/auditService';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { CheckCircle, AlertTriangle, RefreshCw } from 'lucide-react';

export const AuditLogPage: React.FC = () => {
    const { data: logs, isLoading, refetch } = useQuery({
        queryKey: ['audit-logs'],
        queryFn: () => auditService.getLogs(0, 50)
    });

    const { data: verification, refetch: verifyChain, isFetching: isVerifying } = useQuery({
        queryKey: ['audit-verify'],
        queryFn: auditService.verifyChain,
        enabled: false
    });

    if (isLoading) return <Typography>Carregando logs...</Typography>;

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4" component="h1">
                    Auditoria do Sistema
                </Typography>
                <Box>
                    <Button
                        startIcon={<RefreshCw />}
                        onClick={() => refetch()}
                        sx={{ mr: 2 }}
                    >
                        Atualizar
                    </Button>
                    <Button
                        variant="contained"
                        color={verification?.valid ? "success" : "primary"}
                        onClick={() => verifyChain()}
                        disabled={isVerifying}
                        startIcon={verification?.valid ? <CheckCircle /> : <ShieldCheck />}
                    >
                        {isVerifying ? 'Verificando...' : 'Verificar Integridade da Cadeia'}
                    </Button>
                </Box>
            </Box>

            {verification && (
                <Alert
                    severity={verification.valid ? "success" : "error"}
                    sx={{ mb: 3 }}
                    icon={verification.valid ? <CheckCircle /> : <AlertTriangle />}
                >
                    {verification.valid
                        ? `Integridade da cadeia verificada com sucesso em ${format(new Date(verification.timestamp), 'dd/MM/yyyy HH:mm:ss')}`
                        : "FALHA NA VERIFICAÇÃO DE INTEGRIDADE! A cadeia de logs pode ter sido alterada."}
                </Alert>
            )}

            <TableContainer component={Paper}>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Timestamp</TableCell>
                            <TableCell>Evento</TableCell>
                            <TableCell>Hash da Entrada</TableCell>
                            <TableCell>Hash Anterior</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {logs?.content?.map((log: AuditLogEntry) => (
                            <TableRow key={log.id}>
                                <TableCell>
                                    {format(new Date(log.ts), 'dd/MM/yyyy HH:mm:ss', { locale: ptBR })}
                                </TableCell>
                                <TableCell>
                                    <Chip label={log.eventType} size="small" variant="outlined" />
                                </TableCell>
                                <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                                    {log.entryHash.substring(0, 16)}...
                                </TableCell>
                                <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                                    {log.prevHash ? `${log.prevHash.substring(0, 16)}...` : 'GENESIS'}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

import { ShieldCheck } from 'lucide-react';
