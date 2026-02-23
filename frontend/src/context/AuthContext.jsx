import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import authApi from '../api/authApi';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [usuario, setUsuario] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // Carregar usuário do localStorage ao iniciar
  useEffect(() => {
    // Requer novo login a cada abertura/reload da aplicação.
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('usuario');
    setUsuario(null);
    setLoading(false);
  }, []);

  const login = useCallback(async (email, senha) => {
    const response = await authApi.login(email, senha);

    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('usuario', JSON.stringify(response.usuario));

    setUsuario(response.usuario);
    return response;
  }, []);

  const register = useCallback(async (nomeCompleto, email, senha) => {
    const response = await authApi.register(nomeCompleto, email, senha);
    return response;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('usuario');
    setUsuario(null);
    navigate('/login');
  }, [navigate]);

  const isAuthenticated = useCallback(() => {
    return !!usuario && !!localStorage.getItem('accessToken');
  }, [usuario]);

  const value = {
    usuario,
    loading,
    login,
    register,
    logout,
    isAuthenticated,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;
