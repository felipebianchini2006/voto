import { api } from './api';
import type {
  Election,
  CreateElectionRequest,
  CreateCandidateRequest,
  Candidate,
} from '../types';

export const electionService = {
  getAll: async (): Promise<Election[]> => {
    try {
      const response = await api.get<Election[]>('/public/elections');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Error fetching elections:', error);
      throw error;
    }
  },

  getById: async (id: string): Promise<Election> => {
    try {
      // Try public endpoint first, fall back to admin if needed
      const response = await api.get<Election>(`/public/elections/${id}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching election:', error);
      throw error;
    }
  },

  create: async (data: CreateElectionRequest): Promise<Election> => {
    try {
      const response = await api.post<Election>('/admin/elections', data);
      return response.data;
    } catch (error) {
      console.error('Error creating election:', error);
      throw error;
    }
  },

  update: async (id: string, data: Partial<CreateElectionRequest>): Promise<Election> => {
    try {
      const response = await api.put<Election>(`/admin/elections/${id}`, data);
      return response.data;
    } catch (error) {
      console.error('Error updating election:', error);
      throw error;
    }
  },

  start: async (id: string): Promise<void> => {
    try {
      await api.post(`/admin/elections/${id}/start`);
    } catch (error) {
      console.error('Error starting election:', error);
      throw error;
    }
  },

  close: async (id: string): Promise<void> => {
    try {
      await api.post(`/admin/elections/${id}/close`);
    } catch (error) {
      console.error('Error closing election:', error);
      throw error;
    }
  },

  addCandidate: async (electionId: string, data: CreateCandidateRequest): Promise<Candidate> => {
    try {
      const response = await api.post<Candidate>(
        `/admin/elections/${electionId}/candidates`,
        data
      );
      return response.data;
    } catch (error) {
      console.error('Error adding candidate:', error);
      throw error;
    }
  },

  getCandidates: async (electionId: string): Promise<Candidate[]> => {
    try {
      const response = await api.get<Candidate[]>(
        `/admin/elections/${electionId}/candidates`
      );
      return response.data || [];
    } catch (error) {
      console.error('Error fetching candidates:', error);
      throw error;
    }
  },
};
