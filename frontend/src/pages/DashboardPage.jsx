import { useState, useEffect } from 'react';
import { Row, Col, Card, Button, Table, Badge } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { FaGavel, FaCalculator, FaPlus, FaChartLine } from 'react-icons/fa';
import { toast } from 'react-toastify';
import Layout from '../components/common/Layout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import processoApi from '../api/processoApi';
import calculoApi from '../api/calculoApi';

const DashboardPage = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalProcessos: 0,
    totalCalculos: 0,
  });
  const [processosRecentes, setProcessosRecentes] = useState([]);
  const [calculosRecentes, setCalculosRecentes] = useState([]);

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      setLoading(true);

      const [processosRes, calculosRes] = await Promise.all([
        processoApi.listar({ size: 5, sort: 'dataCriacao,desc' }),
        calculoApi.listar({ size: 5, sort: 'dataCriacao,desc' }),
      ]);

      setProcessosRecentes(processosRes.content || []);
      setCalculosRecentes(calculosRes.content || []);
      setStats({
        totalProcessos: processosRes.totalElements || 0,
        totalCalculos: calculosRes.totalElements || 0,
      });
    } catch (error) {
      console.error('Erro ao carregar dados:', error);
      toast.error('Erro ao carregar dados do dashboard');
    } finally {
      setLoading(false);
    }
  };

  const formatarData = (data) => {
    if (!data) return '-';
    return new Date(data).toLocaleDateString('pt-BR');
  };

  const formatarMoeda = (valor) => {
    if (!valor) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(valor);
  };

  const getBadgeTipoAcao = (tipo) => {
    const cores = {
      TRABALHISTA: 'primary',
      CIVEL: 'success',
      PREVIDENCIARIA: 'warning',
      TRIBUTARIA: 'danger',
    };
    return (
      <Badge bg={cores[tipo] || 'secondary'} className="badge-tipo-acao">
        {tipo}
      </Badge>
    );
  };

  const getBadgeStatus = (status) => {
    const cores = {
      RASCUNHO: 'secondary',
      CALCULADO: 'info',
      FINALIZADO: 'success',
    };
    return <Badge bg={cores[status] || 'secondary'}>{status}</Badge>;
  };

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="mb-4">
        <h2 className="mb-1">Dashboard</h2>
        <p className="text-muted">Visão geral do sistema</p>
      </div>

      {/* Cards de estatísticas */}
      <Row className="mb-4">
        <Col md={4}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <p className="text-muted mb-1">Total de Processos</p>
                  <h3 className="stat-number mb-0">{stats.totalProcessos}</h3>
                </div>
                <FaGavel size={40} className="text-primary opacity-25" />
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="stat-card success h-100">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <p className="text-muted mb-1">Total de Cálculos</p>
                  <h3 className="stat-number mb-0">{stats.totalCalculos}</h3>
                </div>
                <FaCalculator size={40} className="text-success opacity-25" />
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="stat-card warning h-100">
            <Card.Body>
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <p className="text-muted mb-1">Índices Disponíveis</p>
                  <h3 className="stat-number mb-0">5</h3>
                </div>
                <FaChartLine size={40} className="text-warning opacity-25" />
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Ações rápidas */}
      <Row className="mb-4">
        <Col>
          <Card>
            <Card.Body>
              <h5 className="mb-3">Ações Rápidas</h5>
              <div className="d-flex gap-2 flex-wrap">
                <Button as={Link} to="/processos/novo" variant="primary">
                  <FaPlus className="me-2" />
                  Novo Processo
                </Button>
                <Button as={Link} to="/calculos/novo" variant="outline-primary">
                  <FaCalculator className="me-2" />
                  Novo Cálculo
                </Button>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Tabelas de processos e cálculos recentes */}
      <Row>
        <Col lg={6}>
          <Card className="mb-4">
            <Card.Header className="d-flex justify-content-between align-items-center">
              <span>Processos Recentes</span>
              <Button
                as={Link}
                to="/processos"
                variant="link"
                size="sm"
                className="p-0"
              >
                Ver todos
              </Button>
            </Card.Header>
            <Card.Body className="p-0">
              {processosRecentes.length === 0 ? (
                <div className="text-center py-4 text-muted">
                  Nenhum processo cadastrado
                </div>
              ) : (
                <Table hover className="mb-0">
                  <thead>
                    <tr>
                      <th>Número</th>
                      <th>Tipo</th>
                      <th>Data</th>
                    </tr>
                  </thead>
                  <tbody>
                    {processosRecentes.map((processo) => (
                      <tr key={processo.id}>
                        <td>
                          <Link to={`/processos/${processo.id}`}>
                            {processo.numeroProcesso || `#${processo.id}`}
                          </Link>
                        </td>
                        <td>{getBadgeTipoAcao(processo.tipoAcao)}</td>
                        <td>{formatarData(processo.dataCriacao)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col lg={6}>
          <Card className="mb-4">
            <Card.Header className="d-flex justify-content-between align-items-center">
              <span>Cálculos Recentes</span>
              <Button
                as={Link}
                to="/calculos"
                variant="link"
                size="sm"
                className="p-0"
              >
                Ver todos
              </Button>
            </Card.Header>
            <Card.Body className="p-0">
              {calculosRecentes.length === 0 ? (
                <div className="text-center py-4 text-muted">
                  Nenhum cálculo realizado
                </div>
              ) : (
                <Table hover className="mb-0">
                  <thead>
                    <tr>
                      <th>Título</th>
                      <th>Valor</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {calculosRecentes.map((calculo) => (
                      <tr key={calculo.id}>
                        <td>
                          <Link to={`/calculos/${calculo.id}`}>
                            {calculo.titulo || `Cálculo #${calculo.id}`}
                          </Link>
                        </td>
                        <td>{formatarMoeda(calculo.valorPrincipal)}</td>
                        <td>{getBadgeStatus(calculo.status)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Layout>
  );
};

export default DashboardPage;
