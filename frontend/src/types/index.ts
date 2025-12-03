export interface Candidate {
    id: string;
    name: string;
    ballotNumber: number;
    description?: string;
    photoUrl?: string;
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
