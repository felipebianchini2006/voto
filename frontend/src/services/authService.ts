import type { AuthResponse, LoginRequest, CandidateRegistrationRequest, User } from '../types';
import { api } from './api';

export const authService = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/login', credentials);
    return response.data;
  },

  registerCandidate: async (data: CandidateRegistrationRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/register/candidate', data);
    return response.data;
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },
};
