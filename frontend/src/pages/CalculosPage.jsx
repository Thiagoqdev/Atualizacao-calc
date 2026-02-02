import { useState, useEffect } from 'react';
import { Row, Col, Card, Button, Table, Badge, Form } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import {
  FaPlus,
  FaEye,
  FaTrash,
  FaSearch,
  FaPlay,
  FaFilePdf,
  FaFileExcel,
} from 'react-icons/fa';
import Layout from '../components/common/Layout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import calculoApi from '../api/calculoApi';

const CalculosPage = () => {
  const [loading, setLoading] = useState(true);
  const [calculos, setCalculos] = useState([]);
  const [pagination, setPagination] = useState({ page: 0, totalPages: 0 });
  const [filtroStatus, setFiltroStatus] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    carregarCalculos();
  }, [pagination.page, filtroStatus]);

  const carregarCalculos = async () => {
    try {
      setLoading(true);
      const params = {
        page: pagination.page,
        size: 10,
        sort: 'dataCriacao,desc',
      };
      if (filtroStatus) params.status = filtroStatus;

      const response = await calculoApi.listar(params);
      setCalculos(response.content || []);
      setPagination({
        page: response.number,
        totalPages: response.totalPages,
        totalElements: response.totalElements,
      });
    } catch (error) {
      toast.error('Erro ao carregar cálculos');
    } finally {
      setLoading(false);
    }
  };

  const handleExecutar = async (id) => {
    try {
      await calculoApi.executar(id);
      toast.success('Cálculo executado com sucesso');
      carregarCalculos();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao executar cálculo');
    }
  };

  const handleExcluir = async (id) => {
    if (!window.confirm('Tem certeza que deseja excluir este cálculo?')) return;

    try {
      await calculoApi.excluir(id);
      toast.success('Cálculo excluído com sucesso');
      carregarCalculos();
    } catch (error) {
      toast.error('Erro ao excluir cálculo');
    }
  };

  const handleDownload = async (id, formato) => {
    try {
      const blob = await calculoApi.downloadRelatorio(id, formato);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `calculo_${id}.${formato}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      a.remove();
    } catch (error) {
      toast.error('Erro ao baixar relatório');
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

  const getBadgeStatus = (status) => {
    const config = {
      RASCUNHO: { bg: 'secondary', label: 'Rascunho' },
      CALCULADO: { bg: 'info', label: 'Calculado' },
      FINALIZADO: { bg: 'success', label: 'Finalizado' },
    };
    const { bg, label } = config[status] || { bg: 'secondary', label: status };
    return <Badge bg={bg}>{label}</Badge>;
  };

  if (loading && calculos.length === 0) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">Cálculos</h2>
          <p className="text-muted mb-0">Gerencie seus cálculos de atualização</p>
        </div>
        <Button variant="primary" as={Link} to="/calculos/novo">
          <FaPlus className="me-2" />
          Novo Cálculo
        </Button>
      </div>

      {/* Filtros */}
      <Card className="mb-4">
        <Card.Body>
          <Row className="align-items-end">
            <Col md={3}>
              <Form.Group>
                <Form.Label>Status</Form.Label>
                <Form.Select
                  value={filtroStatus}
                  onChange={(e) => {
                    setFiltroStatus(e.target.value);
                    setPagination({ ...pagination, page: 0 });
                  }}
                >
                  <option value="">Todos</option>
                  <option value="RASCUNHO">Rascunho</option>
                  <option value="CALCULADO">Calculado</option>
                  <option value="FINALIZADO">Finalizado</option>
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {/* Tabela */}
      <Card>
        <Card.Body className="p-0">
          {calculos.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <p className="mb-3">Nenhum cálculo encontrado</p>
              <Button variant="primary" as={Link} to="/calculos/novo">
                <FaPlus className="me-2" />
                Criar primeiro cálculo
              </Button>
            </div>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Título</th>
                  <th>Valor Principal</th>
                  <th>Período</th>
                  <th>Índice</th>
                  <th>Status</th>
                  <th>Data Criação</th>
                  <th width="180">Ações</th>
                </tr>
              </thead>
              <tbody>
                {calculos.map((calculo) => (
                  <tr key={calculo.id}>
                    <td>
                      <Link to={`/calculos/${calculo.id}`}>
                        {calculo.titulo || `Cálculo #${calculo.id}`}
                      </Link>
                    </td>
                    <td>{formatarMoeda(calculo.valorPrincipal)}</td>
                    <td>
                      {formatarData(calculo.dataInicial)} a{' '}
                      {formatarData(calculo.dataFinal)}
                    </td>
                    <td>{calculo.tabelaIndiceNome || '-'}</td>
                    <td>{getBadgeStatus(calculo.status)}</td>
                    <td>{formatarData(calculo.dataCriacao)}</td>
                    <td>
                      <Button
                        variant="outline-primary"
                        size="sm"
                        className="me-1"
                        onClick={() => navigate(`/calculos/${calculo.id}`)}
                        title="Visualizar"
                      >
                        <FaEye />
                      </Button>
                      {calculo.status === 'RASCUNHO' && (
                        <Button
                          variant="outline-success"
                          size="sm"
                          className="me-1"
                          onClick={() => handleExecutar(calculo.id)}
                          title="Executar cálculo"
                        >
                          <FaPlay />
                        </Button>
                      )}
                      {calculo.status !== 'RASCUNHO' && (
                        <>
                          <Button
                            variant="outline-danger"
                            size="sm"
                            className="me-1"
                            onClick={() => handleDownload(calculo.id, 'pdf')}
                            title="Baixar PDF"
                          >
                            <FaFilePdf />
                          </Button>
                          <Button
                            variant="outline-success"
                            size="sm"
                            className="me-1"
                            onClick={() => handleDownload(calculo.id, 'xlsx')}
                            title="Baixar Excel"
                          >
                            <FaFileExcel />
                          </Button>
                        </>
                      )}
                      <Button
                        variant="outline-danger"
                        size="sm"
                        onClick={() => handleExcluir(calculo.id)}
                        title="Excluir"
                      >
                        <FaTrash />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>

        {/* Paginação */}
        {pagination.totalPages > 1 && (
          <Card.Footer>
            <div className="d-flex justify-content-between align-items-center">
              <span className="text-muted">
                Mostrando {calculos.length} de {pagination.totalElements} cálculos
              </span>
              <div>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  className="me-2"
                  disabled={pagination.page === 0}
                  onClick={() =>
                    setPagination({ ...pagination, page: pagination.page - 1 })
                  }
                >
                  Anterior
                </Button>
                <Button
                  variant="outline-secondary"
                  size="sm"
                  disabled={pagination.page >= pagination.totalPages - 1}
                  onClick={() =>
                    setPagination({ ...pagination, page: pagination.page + 1 })
                  }
                >
                  Próxima
                </Button>
              </div>
            </div>
          </Card.Footer>
        )}
      </Card>
    </Layout>
  );
};

export default CalculosPage;
