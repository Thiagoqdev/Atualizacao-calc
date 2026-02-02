import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Form, Button, Alert, Card } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaUser, FaEnvelope, FaLock, FaUserPlus } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';

const RegisterPage = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm();

  const senha = watch('senha');

  const onSubmit = async (data) => {
    setLoading(true);
    setError('');

    try {
      await registerUser(data.nomeCompleto, data.email, data.senha);
      toast.success('Cadastro realizado com sucesso! Faça login para continuar.');
      navigate('/login');
    } catch (err) {
      const message =
        err.response?.data?.message || 'Erro ao realizar cadastro. Tente novamente.';
      setError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <Card className="login-card">
        <div className="text-center mb-4">
          <h1 className="login-logo">CalcJur</h1>
          <p className="text-muted">Criar nova conta</p>
        </div>

        {error && <Alert variant="danger">{error}</Alert>}

        <Form onSubmit={handleSubmit(onSubmit)}>
          <Form.Group className="mb-3">
            <Form.Label>
              <FaUser className="me-2" />
              Nome Completo
            </Form.Label>
            <Form.Control
              type="text"
              placeholder="Seu nome completo"
              {...register('nomeCompleto', {
                required: 'Nome é obrigatório',
                minLength: {
                  value: 3,
                  message: 'Nome deve ter no mínimo 3 caracteres',
                },
              })}
              isInvalid={!!errors.nomeCompleto}
            />
            <Form.Control.Feedback type="invalid">
              {errors.nomeCompleto?.message}
            </Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label>
              <FaEnvelope className="me-2" />
              Email
            </Form.Label>
            <Form.Control
              type="email"
              placeholder="seu@email.com"
              {...register('email', {
                required: 'Email é obrigatório',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message: 'Email inválido',
                },
              })}
              isInvalid={!!errors.email}
            />
            <Form.Control.Feedback type="invalid">
              {errors.email?.message}
            </Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label>
              <FaLock className="me-2" />
              Senha
            </Form.Label>
            <Form.Control
              type="password"
              placeholder="Mínimo 6 caracteres"
              {...register('senha', {
                required: 'Senha é obrigatória',
                minLength: {
                  value: 6,
                  message: 'Senha deve ter no mínimo 6 caracteres',
                },
              })}
              isInvalid={!!errors.senha}
            />
            <Form.Control.Feedback type="invalid">
              {errors.senha?.message}
            </Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-4">
            <Form.Label>
              <FaLock className="me-2" />
              Confirmar Senha
            </Form.Label>
            <Form.Control
              type="password"
              placeholder="Repita a senha"
              {...register('confirmarSenha', {
                required: 'Confirmação de senha é obrigatória',
                validate: (value) =>
                  value === senha || 'As senhas não conferem',
              })}
              isInvalid={!!errors.confirmarSenha}
            />
            <Form.Control.Feedback type="invalid">
              {errors.confirmarSenha?.message}
            </Form.Control.Feedback>
          </Form.Group>

          <Button
            variant="primary"
            type="submit"
            className="w-100 py-2"
            disabled={loading}
          >
            {loading ? (
              'Cadastrando...'
            ) : (
              <>
                <FaUserPlus className="me-2" />
                Cadastrar
              </>
            )}
          </Button>
        </Form>

        <div className="text-center mt-4">
          <p className="text-muted mb-0">
            Já tem uma conta?{' '}
            <Link to="/login" className="text-primary">
              Faça login
            </Link>
          </p>
        </div>
      </Card>
    </div>
  );
};

export default RegisterPage;
