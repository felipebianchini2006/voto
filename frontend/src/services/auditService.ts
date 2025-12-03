import { api } from './api';

export interface AuditLogEntry {
    id: number;
    eventType: string;
    eventData: string;
    ts: string;
    entryHash: string;
    prevHash: string;
    signature: string;
}

export const auditService = {
    getLogs: async (page = 0, size = 20) => {
        const response = await api.get<any>(`/audit/log?page=${page}&size=${size}&sort=ts,desc`);
        return response.data;
    },

    verifyChain: async () => {
        const response = await api.get<{ valid: boolean; timestamp: string }>('/audit/verify-chain');
        return response.data;
    },

    getCommitment: async () => {
        const response = await api.get<{ rootHash: string }>('/audit/commitment');
        return response.data;
    }
};
