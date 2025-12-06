// Auth Types
export interface User {
    id: string;
    username: string;
    email: string;
    role: 'ADMIN' | 'CANDIDATE';
    enabled: boolean;
    lastLoginAt?: string;
    createdAt: string;
}

export interface AuthResponse {
    token: string;
    type: string;
    user: User;
}

export interface LoginRequest {
    username: string;
    password: string;
}

export interface CandidateRegistrationRequest {
    username: string;
    password: string;
    fullName: string;
    email: string;
    description?: string;
    party?: string;
}

// Election Types
export interface Candidate {
    id: string;
    name: string;
    ballotNumber: number;
    description?: string;
    photoUrl?: string;
    party?: string;
}

export interface Election {
    id: string;
    name: string;
    description: string;
    startTs: string;
    endTs: string;
    status: 'DRAFT' | 'RUNNING' | 'CLOSED' | 'CANCELLED';
    maxVotesPerVoter: number;
    allowAbstention: boolean;
    requireJustification: boolean;
    candidates?: Candidate[];
}

export interface CreateElectionRequest {
    name: string;
    description: string;
    startTs: string;
    endTs: string;
    maxVotesPerVoter: number;
    allowAbstention: boolean;
}

export interface CreateCandidateRequest {
    name: string;
    ballotNumber: number;
    description: string;
}

// Candidate Portal Types
export interface ApplyToElectionRequest {
    fullName: string;
    description?: string;
    party?: string;
    photoUrl?: string;
}

export interface UpdateCandidateProfileRequest {
    fullName?: string;
    photoUrl?: string;
    description?: string;
    party?: string;
}

export interface CandidateResponse extends Candidate {
    userId?: string;
    electionId: string;
}
