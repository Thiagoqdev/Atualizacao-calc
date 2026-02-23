import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import PrivateRoute from './components/common/PrivateRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ProcessosPage from './pages/ProcessosPage';
import CalculosPage from './pages/CalculosPage';
import CalculoFormPage from './pages/CalculoFormPage';
import CalculoResultadoPage from './pages/CalculoResultadoPage';
import IndicesPage from './pages/IndicesPage';

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      {/* Rotas públicas */}
      <Route
        path="/login"
        element={
          isAuthenticated() ? <Navigate to="/dashboard" replace /> : <LoginPage />
        }
      />
      <Route
        path="/register"
        element={
          isAuthenticated() ? <Navigate to="/dashboard" replace /> : <RegisterPage />
        }
      />

      {/* Rotas protegidas */}
      <Route
        path="/dashboard"
        element={
          <PrivateRoute>
            <DashboardPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/dashboard/peticoes"
        element={
          <PrivateRoute>
            <DashboardPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/processos"
        element={
          <PrivateRoute>
            <ProcessosPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/calculos"
        element={
          <PrivateRoute>
            <CalculosPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/calculos/novo"
        element={
          <PrivateRoute>
            <CalculoFormPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/calculos/:id"
        element={
          <PrivateRoute>
            <CalculoResultadoPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/calculos/:id/editar"
        element={
          <PrivateRoute>
            <CalculoFormPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/indices"
        element={
          <PrivateRoute>
            <IndicesPage />
          </PrivateRoute>
        }
      />

      {/* Rota padrão */}
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
