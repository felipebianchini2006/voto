import { api } from './api';
import { Election, CreateElectionRequest, CreateCandidateRequest, Candidate } from '../types';

export const electionService = {
    getAll: async () => {
        const response = await api.get<any>('/admin/elections');
        return response.data.content || response.data;
    },

    getById: async (id: string) => {
        const response = await api.get<Election>(`/admin/elections/${id}`);
        return response.data;
    },

    create: async (data: CreateElectionRequest) => {
        const response = await api.post<Election>('/admin/elections', data);
        return response.data;
    },

    update: async (id: string, data: Partial<CreateElectionRequest>) => {
        const response = await api.put<Election>(`/admin/elections/${id}`, data);
        return response.data;
    },

    start: async (id: string) => {
        const response = await api.post(`/admin/elections/${id}/start`);
        return response.data;
    },

    close: async (id: string) => {
        const response = await api.post(`/admin/elections/${id}/close`);
        return response.data;
    },

    addCandidate: async (electionId: string, data: CreateCandidateRequest) => {
        const response = await api.post<Candidate>(`/admin/elections/${electionId}/candidates`, data);
        return response.data;
    },

    getCandidates: async (electionId: string) => {
        const response = await api.get<Candidate[]>(`/admin/elections/${electionId}/candidates`);
        return response.data;
    }
};
