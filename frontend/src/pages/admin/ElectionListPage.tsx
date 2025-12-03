import React from 'react';
import {
    Box,
    Typography,
    Button,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Chip,
    IconButton,
    Tooltip
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { Link as RouterLink } from 'react-router-dom';
import { Plus, Edit, Eye, Play, Square } from 'lucide-react';
import { electionService } from '../services/electionService';
import { Election } from '../types';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

const StatusChip = ({ status }: { status: string }) => {
    let color: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' = 'default';

    switch (status) {
        case 'DRAFT': color = 'warning'; break;
        case 'RUNNING': color = 'success'; break;
        case 'CLOSED': color = 'error'; break;
        case 'CANCELLED': color = 'default'; break;
    }

    return <Chip label={status} color={color} size="small" />;
};

export const ElectionListPage: React.FC = () => {
    const { data: elections, isLoading } = useQuery({
        queryKey: ['elections'],
        queryFn: electionService.getAll
    });

    if (isLoading) return <Typography>Carregando...</Typography>;

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4" component="h1">
                    Gerenciar Eleições
                </Typography>
                <Button
                    variant="contained"
                    startIcon={<Plus />}
                    component={RouterLink}
                    to="/admin/elections/new"
                >
                    Nova Eleição
                </Button>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Nome</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Início</TableCell>
                            <TableCell>Fim</TableCell>
                            <TableCell align="right">Ações</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {elections?.map((election: Election) => (
                            <TableRow key={election.id}>
                                <TableCell>{election.name}</TableCell>
                                <TableCell><StatusChip status={election.status} /></TableCell>
                                <TableCell>
                                    {format(new Date(election.startTs), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                                </TableCell>
                                <TableCell>
                                    {format(new Date(election.endTs), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                                </TableCell>
                                <TableCell align="right">
                                    <Tooltip title="Detalhes">
                                        <IconButton component={RouterLink} to={`/admin/elections/${election.id}`}>
                                            <Eye size={20} />
                                        </IconButton>
                                    </Tooltip>
                                    {election.status === 'DRAFT' && (
                                        <Tooltip title="Editar">
                                            <IconButton component={RouterLink} to={`/admin/elections/${election.id}/edit`}>
                                                <Edit size={20} />
                                            </IconButton>
                                        </Tooltip>
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                        {elections?.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={5} align="center">
                                    Nenhuma eleição encontrada
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};
