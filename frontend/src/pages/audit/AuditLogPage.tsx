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
  CircularProgress,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { auditService } from '../../services/auditService';
import type { AuditLogEntry } from '../../services/auditService';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { CheckCircle, Refresh, Security } from '@mui/icons-material';

export const AuditLogPage: React.FC = () => {
  const {
    data: logs,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['audit-logs'],
    queryFn: () => auditService.getLogs(0, 50),
  });

  const {
    data: verification,
    refetch: verifyChain,
    isFetching: isVerifying,
  } = useQuery({
    queryKey: ['audit-verify'],
    queryFn: auditService.verifyChain,
    enabled: false,
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  const logsList: AuditLogEntry[] = logs?.content ?? [];

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          mb: 4,
        }}
      >
        <Typography variant="h4" component="h1">
          Auditoria do Sistema
        </Typography>
        <Box>
          <Button startIcon={<Refresh />} onClick={() => refetch()} sx={{ mr: 2 }}>
            Atualizar
          </Button>
          <Button
            variant="contained"
            color={verification?.valid ? 'success' : 'primary'}
            onClick={() => verifyChain()}
            disabled={isVerifying}
            startIcon={verification?.valid ? <CheckCircle /> : <Security />}
          >
            {isVerifying ? 'Verificando...' : 'Verificar Integridade da Cadeia'}
          </Button>
        </Box>
      </Box>

      {verification && (
        <Alert
          severity={verification.valid ? 'success' : 'error'}
          sx={{ mb: 3 }}
          icon={verification.valid ? <CheckCircle /> : undefined}
        >
          {verification.valid
            ? `Integridade da cadeia verificada com sucesso em ${format(
                new Date(verification.timestamp),
                'dd/MM/yyyy HH:mm:ss',
                { locale: ptBR }
              )}`
            : 'FALHA NA VERIFICAÇÃO DE INTEGRIDADE! A cadeia de logs pode ter sido alterada.'}
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
            {logsList.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} align="center">
                  Nenhum log encontrado
                </TableCell>
              </TableRow>
            ) : (
              logsList.map((log: AuditLogEntry) => (
                <TableRow key={log.id}>
                  <TableCell>
                    {format(new Date(log.ts), 'dd/MM/yyyy HH:mm:ss', { locale: ptBR })}
                  </TableCell>
                  <TableCell>
                    <Chip label={log.eventType} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                    {log.entryHash?.substring(0, 16)}...
                  </TableCell>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                    {log.prevHash ? `${log.prevHash.substring(0, 16)}...` : 'GENESIS'}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};
