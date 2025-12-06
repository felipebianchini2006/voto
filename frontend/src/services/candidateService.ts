import type {
  CandidateResponse,
  Election,
  ApplyToElectionRequest,
  UpdateCandidateProfileRequest
} from '../types';
import { api } from './api';

export const candidateService = {
  getMyCandidacies: async (): Promise<CandidateResponse[]> => {
    const response = await api.get<CandidateResponse[]>('/candidate/elections');
    return response.data;
  },

  getAvailableElections: async (): Promise<Election[]> => {
    const response = await api.get<Election[]>('/candidate/elections/available');
    return response.data;
  },

  applyToElection: async (electionId: string, data: ApplyToElectionRequest): Promise<CandidateResponse> => {
    const response = await api.post<CandidateResponse>(`/candidate/elections/${electionId}/apply`, data);
    return response.data;
  },

  withdrawFromElection: async (electionId: string): Promise<void> => {
    await api.delete(`/candidate/elections/${electionId}/withdraw`);
  },

  updateProfile: async (candidateId: string, data: UpdateCandidateProfileRequest): Promise<CandidateResponse> => {
    const response = await api.put<CandidateResponse>(`/candidate/candidates/${candidateId}/profile`, data);
    return response.data;
  },
};
