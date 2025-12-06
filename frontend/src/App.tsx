import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { theme } from './theme';
import { AuthProvider, useAuth } from './store/AuthContext';
import { MainLayout } from './layouts/MainLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ElectionListPage } from './pages/admin/ElectionListPage';
import { ElectionFormPage } from './pages/admin/ElectionFormPage';
import { VoterDashboard } from './pages/voter/VoterDashboard';
import { VotingBoothPage } from './pages/voter/VotingBoothPage';
import { AuditLogPage } from './pages/audit/AuditLogPage';
import { CandidatePortalPage } from './pages/candidate/CandidatePortalPage';

const queryClient = new QueryClient();

const ProtectedRoute: React.FC<{ children: React.ReactNode, allowedRoles?: ('ADMIN' | 'CANDIDATE')[] }> = ({ children, allowedRoles }) => {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" />;
  }

  return <>{children}</>;
};

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />

        {/* Voter Routes - Public for now */}
        <Route path="elections" element={<VoterDashboard />} />
        <Route path="vote/:electionId" element={<VotingBoothPage />} />

        {/* Admin Routes */}
        <Route path="admin" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <ElectionListPage />
          </ProtectedRoute>
        } />
        <Route path="admin/elections/new" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <ElectionFormPage />
          </ProtectedRoute>
        } />

        {/* Candidate Routes */}
        <Route path="candidate" element={
          <ProtectedRoute allowedRoles={['CANDIDATE']}>
            <CandidatePortalPage />
          </ProtectedRoute>
        } />

        {/* Audit Routes */}
        <Route path="audit" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AuditLogPage />
          </ProtectedRoute>
        } />
      </Route>
    </Routes>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </ThemeProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
