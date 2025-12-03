import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { theme } from './theme';
import { AuthProvider, useAuth } from './store/AuthContext';
import { MainLayout } from './layouts/MainLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { ElectionListPage } from './pages/admin/ElectionListPage';
import { ElectionFormPage } from './pages/admin/ElectionFormPage';
import { VoterDashboard } from './pages/voter/VoterDashboard';
import { VotingBoothPage } from './pages/voter/VotingBoothPage';
import { AuditLogPage } from './pages/audit/AuditLogPage';

const queryClient = new QueryClient();

const ProtectedRoute: React.FC<{ children: React.ReactNode, roles?: string[] }> = ({ children, roles }) => {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  if (roles && user && !roles.some(role => user.roles.includes(role))) {
    return <Navigate to="/" />;
  }

  return <>{children}</>;
};

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />

        {/* Voter Routes */}
        <Route path="elections" element={
          <ProtectedRoute roles={['ROLE_VOTER', 'ROLE_ADMIN']}>
            <VoterDashboard />
          </ProtectedRoute>
        } />
        <Route path="vote/:electionId" element={
          <ProtectedRoute roles={['ROLE_VOTER', 'ROLE_ADMIN']}>
            <VotingBoothPage />
          </ProtectedRoute>
        } />

        {/* Admin Routes */}
        <Route path="admin" element={
          <ProtectedRoute roles={['ROLE_ADMIN']}>
            <ElectionListPage />
          </ProtectedRoute>
        } />
        <Route path="admin/elections/new" element={
          <ProtectedRoute roles={['ROLE_ADMIN']}>
            <ElectionFormPage />
          </ProtectedRoute>
        } />

        {/* Audit Routes */}
        <Route path="audit" element={
          <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_AUDITOR']}>
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
