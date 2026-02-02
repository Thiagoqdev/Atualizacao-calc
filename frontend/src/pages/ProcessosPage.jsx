import { useState, useEffect } from 'react';
import { Row, Col, Card, Button, Table, Badge, Form, Modal } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaPlus, FaEdit, FaTrash, FaSearch, FaCalculator } from 'react-icons/fa';
import Layout from '../components/common/Layout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import processoApi from '../api/processoApi';

const TIPOS_ACAO = ['TRABALHISTA', 'CIVEL', 'PREVIDENCIARIA', 'TRIBUTARIA'];

const ProcessosPage = () => {
  const [loading, setLoading] = useState(true);
  const [processos, setProcessos] = useState([]);
  const [pagination, setPagination] = useState({ page: 0, totalPages: 0 });
  const [filtros, setFiltros] = useState({ numeroProcesso: '', tipoAcao: '' });
  const [showModal, setShowModal] = useState(false);
  const [editando, setEditando] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm();

  useEffect(() => {
    carregarProcessos();
  }, [pagination.page, filtros]);

  const carregarProcessos = async () => {
    try {
      setLoading(true);
      const params = {
        page: pagination.page,
        size: 10,
        sort: 'dataCriacao,desc',
        ...filtros,
      };
      const response = await processoApi.listar(params);
      setProcessos(response.content || []);
      setPagination({
        page: response.number,
        totalPages: response.totalPages,
        totalElements: response.totalElements,
      });
    } catch (error) {
      toast.error('Erro ao carregar processos');
    } finally {
      setLoading(false);
    }
  };

  const handleNovoProcesso = () => {
    setEditando(null);
    reset({
      numeroProcesso: '',
      descricao: '',
      varaTribunal: '',
      tipoAcao: '',
    });
    setShowModal(true);
  };

  const handleEditarProcesso = (processo) => {
    setEditando(processo);
    reset(processo);
    setShowModal(true);
  };

  const handleExcluirProcesso = async (id) => {
    if (!window.confirm('Tem certeza que deseja excluir este processo?')) return;

    try {
      await processoApi.excluir(id);
      toast.success('Processo excluído com sucesso');
      carregarProcessos();
    } catch (error) {
      toast.error('Erro ao excluir processo');
    }
  };

  const onSubmit = async (data) => {
    setSalvando(true);
    try {
      if (editando) {
        await processoApi.atualizar(editando.id, data);
        toast.success('Processo atualizado com sucesso');
      } else {
        await processoApi.criar(data);
        toast.success('Processo criado com sucesso');
      }
      setShowModal(false);
      carregarProcessos();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar processo');
    } finally {
      setSalvando(false);
    }
  };

  const handleFiltrar = (e) => {
    e.preventDefault();
    setPagination({ ...pagination, page: 0 });
  };

  const formatarData = (data) => {
    if (!data) return '-';
    return new Date(data).toLocaleDateString('pt-BR');
  };

  const getBadgeTipoAcao = (tipo) => {
    const cores = {
      TRABALHISTA: 'primary',
      CIVEL: 'success',
      PREVIDENCIARIA: 'warning',
      TRIBUTARIA: 'danger',
    };
    return <Badge bg={cores[tipo] || 'secondary'}>{tipo}</Badge>;
  };

  if (loading && processos.length === 0) {
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
          <h2 className="mb-1">Processos</h2>
          <p className="text-muted mb-0">
            Gerencie seus processos jurídicos
          </p>
        </div>
        <Button variant="primary" onClick={handleNovoProcesso}>
          <FaPlus className="me-2" />
          Novo Processo
        </Button>
      </div>

      {/* Filtros */}
      <Card className="mb-4">
        <Card.Body>
          <Form onSubmit={handleFiltrar}>
            <Row className="align-items-end">
              <Col md={4}>
                <Form.Group>
                  <Form.Label>Número do Processo</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="Buscar por número..."
                    value={filtros.numeroProcesso}
                    onChange={(e) =>
                      setFiltros({ ...filtros, numeroProcesso: e.target.value })
                    }
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group>
                  <Form.Label>Tipo de Ação</Form.Label>
                  <Form.Select
                    value={filtros.tipoAcao}
                    onChange={(e) =>
                      setFiltros({ ...filtros, tipoAcao: e.target.value })
                    }
                  >
                    <option value="">Todos</option>
                    {TIPOS_ACAO.map((tipo) => (
                      <option key={tipo} value={tipo}>
                        {tipo}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={2}>
                <Button type="submit" variant="outline-primary" className="w-100">
                  <FaSearch className="me-2" />
                  Filtrar
                </Button>
              </Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      {/* Tabela de processos */}
      <Card>
        <Card.Body className="p-0">
          {processos.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <p className="mb-3">Nenhum processo encontrado</p>
              <Button variant="primary" onClick={handleNovoProcesso}>
                <FaPlus className="me-2" />
                Criar primeiro processo
              </Button>
            </div>
          ) : (
            <Table hover responsive className="mb-0">
              <thead>
                <tr>
                  <th>Número</th>
                  <th>Descrição</th>
                  <th>Vara/Tribunal</th>
                  <th>Tipo</th>
                  <th>Data Criação</th>
                  <th>Cálculos</th>
                  <th width="120">Ações</th>
                </tr>
              </thead>
              <tbody>
                {processos.map((processo) => (
                  <tr key={processo.id}>
                    <td>
                      <Link to={`/processos/${processo.id}`}>
                        {processo.numeroProcesso || `#${processo.id}`}
                      </Link>
                    </td>
                    <td>{processo.descricao || '-'}</td>
                    <td>{processo.varaTribunal || '-'}</td>
                    <td>{getBadgeTipoAcao(processo.tipoAcao)}</td>
                    <td>{formatarData(processo.dataCriacao)}</td>
                    <td>
                      <Badge bg="secondary">{processo.quantidadeCalculos || 0}</Badge>
                    </td>
                    <td>
                      <Button
                        variant="outline-primary"
                        size="sm"
                        className="me-1"
                        onClick={() => navigate(`/calculos/novo?processoId=${processo.id}`)}
                        title="Novo cálculo"
                      >
                        <FaCalculator />
                      </Button>
                      <Button
                        variant="outline-secondary"
                        size="sm"
                        className="me-1"
                        onClick={() => handleEditarProcesso(processo)}
                        title="Editar"
                      >
                        <FaEdit />
                      </Button>
                      <Button
                        variant="outline-danger"
                        size="sm"
                        onClick={() => handleExcluirProcesso(processo.id)}
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
                Mostrando {processos.length} de {pagination.totalElements} processos
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

      {/* Modal de criação/edição */}
      <Modal show={showModal} onHide={() => setShowModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>
            {editando ? 'Editar Processo' : 'Novo Processo'}
          </Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit(onSubmit)}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Número do Processo</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="0001234-56.2024.8.26.0100"
                    {...register('numeroProcesso')}
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Tipo de Ação *</Form.Label>
                  <Form.Select
                    {...register('tipoAcao', { required: 'Tipo de ação é obrigatório' })}
                    isInvalid={!!errors.tipoAcao}
                  >
                    <option value="">Selecione...</option>
                    {TIPOS_ACAO.map((tipo) => (
                      <option key={tipo} value={tipo}>
                        {tipo}
                      </option>
                    ))}
                  </Form.Select>
                  <Form.Control.Feedback type="invalid">
                    {errors.tipoAcao?.message}
                  </Form.Control.Feedback>
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-3">
              <Form.Label>Vara/Tribunal</Form.Label>
              <Form.Control
                type="text"
                placeholder="Ex: 1ª Vara do Trabalho de São Paulo"
                {...register('varaTribunal')}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Descrição</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                placeholder="Descrição do processo..."
                {...register('descricao')}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModal(false)}>
              Cancelar
            </Button>
            <Button variant="primary" type="submit" disabled={salvando}>
              {salvando ? 'Salvando...' : 'Salvar'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </Layout>
  );
};

export default ProcessosPage;
