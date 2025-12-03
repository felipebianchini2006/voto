import { api } from './api';

export const votingService = {
    requestToken: async (electionId: string, externalId: string) => {
        const response = await api.post(`/voting/elections/${electionId}/token`, { externalId });
        return response.data;
    },

    castVote: async (electionId: string, token: string, candidateId: string) => {
        const response = await api.post(`/voting/elections/${electionId}/vote`, {
            token,
            candidateId
        });
        return response.data;
    },

    verifyReceipt: async (electionId: string, ballotHash: string) => {
        const response = await api.get(`/voting/elections/${electionId}/verify/${ballotHash}`);
        return response.data;
    }
};
