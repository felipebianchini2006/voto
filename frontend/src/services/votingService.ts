import { api } from './api';

export interface TokenResponse {
  id: string;
  tokenValue: string;
  signature: string;
  expiresAt: string;
  publicKey?: string;
}

export interface VoteReceiptResponse {
  ballotId: string;
  ballotHash: string;
  castAt: string;
  message: string;
}

export interface BallotVerificationResponse {
  exists: boolean;
  tallied: boolean;
  castAt: string;
  message: string;
}

export const votingService = {
  requestToken: async (electionId: string, externalId: string): Promise<TokenResponse> => {
    try {
      const response = await api.post<TokenResponse>(`/voting/elections/${electionId}/token`, {
        externalId,
      });
      return response.data;
    } catch (error) {
      console.error('Error requesting token:', error);
      throw error;
    }
  },

  castVote: async (
    electionId: string,
    token: string,
    candidateId: string
  ): Promise<VoteReceiptResponse> => {
    try {
      const response = await api.post<VoteReceiptResponse>(
        `/voting/elections/${electionId}/vote`,
        {
          token,
          candidateId,
        }
      );
      return response.data;
    } catch (error) {
      console.error('Error casting vote:', error);
      throw error;
    }
  },

  verifyReceipt: async (
    electionId: string,
    ballotHash: string
  ): Promise<BallotVerificationResponse> => {
    try {
      const response = await api.get<BallotVerificationResponse>(
        `/voting/elections/${electionId}/verify/${ballotHash}`
      );
      return response.data;
    } catch (error) {
      console.error('Error verifying receipt:', error);
      throw error;
    }
  },
};
