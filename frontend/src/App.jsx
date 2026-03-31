import { Routes, Route, Navigate } from 'react-router-dom';
import DashboardPage from './pages/DashboardPage';
import ProcessosPage from './pages/ProcessosPage';
import CalculosPage from './pages/CalculosPage';
import CalculoFormPage from './pages/CalculoFormPage';
import CalculoResultadoPage from './pages/CalculoResultadoPage';
import IndicesPage from './pages/IndicesPage';

function App() {
  return (
    <Routes>
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/dashboard/peticoes" element={<DashboardPage />} />
      <Route path="/processos" element={<ProcessosPage />} />
      <Route path="/calculos" element={<CalculosPage />} />
      <Route path="/calculos/novo" element={<CalculoFormPage />} />
      <Route path="/calculos/:id" element={<CalculoResultadoPage />} />
      <Route path="/calculos/:id/editar" element={<CalculoFormPage />} />
      <Route path="/indices" element={<IndicesPage />} />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
