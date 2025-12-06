import { api } from './api';

export interface AuditLogEntry {
  id: number;
  eventType: string;
  eventData: string;
  ts: string;
  entryHash: string;
  prevHash: string | null;
  signature: string;
}

export interface AuditLogResponse {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface ChainVerificationResponse {
  valid: boolean;
  timestamp: string;
  totalEntries?: number;
  invalidEntries?: number;
}

export const auditService = {
  getLogs: async (page = 0, size = 20): Promise<AuditLogResponse> => {
    try {
      const response = await api.get<AuditLogResponse>(
        `/audit/log?page=${page}&size=${size}&sort=ts,desc`
      );
      return response.data;
    } catch (error) {
      console.error('Error fetching audit logs:', error);
      throw error;
    }
  },

  verifyChain: async (): Promise<ChainVerificationResponse> => {
    try {
      const response = await api.get<ChainVerificationResponse>('/audit/verify-chain');
      return response.data;
    } catch (error) {
      console.error('Error verifying chain:', error);
      throw error;
    }
  },

  getCommitment: async (): Promise<{ rootHash: string }> => {
    try {
      const response = await api.get<{ rootHash: string }>('/audit/commitment');
      return response.data;
    } catch (error) {
      console.error('Error fetching commitment:', error);
      throw error;
    }
  },
};
