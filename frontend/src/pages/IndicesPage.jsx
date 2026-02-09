import { useState, useEffect } from 'react';
import { Row, Col, Card, Button, Table, Form, Modal, Badge } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { FaUpload, FaSync, FaChartLine } from 'react-icons/fa';
import Layout from '../components/common/Layout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import indiceApi from '../api/indiceApi';

const IndicesPage = () => {
  const [loading, setLoading] = useState(true);
  const [tabelas, setTabelas] = useState([]);
  const [tabelaSelecionada, setTabelaSelecionada] = useState(null);
  const [valores, setValores] = useState([]);
  const [loadingValores, setLoadingValores] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [arquivo, setArquivo] = useState(null);
  const [importando, setImportando] = useState(false);
  const [sincronizando, setSincronizando] = useState(false);

  useEffect(() => {
    carregarTabelas();
  }, []);

  const carregarTabelas = async () => {
    try {
      setLoading(true);
      const response = await indiceApi.listarTabelas();
      setTabelas(response);
      if (response.length > 0) {
        setTabelaSelecionada(response[0]);
        carregarValores(response[0].id);
      }
    } catch (error) {
      toast.error('Erro ao carregar índices');
    } finally {
      setLoading(false);
    }
  };

  const carregarValores = async (tabelaId) => {
    try {
      setLoadingValores(true);
      const response = await indiceApi.listarValores(tabelaId);
      setValores(response);
    } catch (error) {
      toast.error('Erro ao carregar valores do índice');
    } finally {
      setLoadingValores(false);
    }
  };

  const handleSelecionarTabela = (tabela) => {
    setTabelaSelecionada(tabela);
    carregarValores(tabela.id);
  };

  const handleImportar = async () => {
    if (!arquivo || !tabelaSelecionada) {
      toast.warning('Selecione um arquivo CSV');
      return;
    }

    setImportando(true);
    try {
      const result = await indiceApi.importarCSV(tabelaSelecionada.id, arquivo);
      toast.success(
        `Importação concluída: ${result.registrosImportados} novos, ${result.registrosAtualizados} atualizados`
      );
      if (result.erros && result.erros.length > 0) {
        console.warn('Erros na importação:', result.erros);
      }
      setShowImportModal(false);
      setArquivo(null);
      carregarValores(tabelaSelecionada.id);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao importar arquivo');
    } finally {
      setImportando(false);
    }
  };

  const handleSincronizar = async () => {
    if (!tabelaSelecionada) return;

    setSincronizando(true);
    try {
      const result = await indiceApi.sincronizar(tabelaSelecionada.id);
      toast.success(
        `Sincronizado com BCB: ${result.registrosImportados} novos, ${result.registrosAtualizados} atualizados`
      );
      if (result.erros && result.erros.length > 0) {
        result.erros.forEach((erro) => toast.warning(erro));
      }
      carregarValores(tabelaSelecionada.id);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao sincronizar com o Banco Central');
    } finally {
      setSincronizando(false);
    }
  };

  const handleSincronizarTodos = async () => {
    setSincronizando(true);
    try {
      const resultados = await indiceApi.sincronizarTodos();
      let totalNovos = 0;
      let totalAtualizados = 0;
      Object.entries(resultados).forEach(([nome, result]) => {
        totalNovos += result.registrosImportados;
        totalAtualizados += result.registrosAtualizados;
      });
      toast.success(
        `Todos sincronizados: ${totalNovos} novos, ${totalAtualizados} atualizados`
      );
      if (tabelaSelecionada) {
        carregarValores(tabelaSelecionada.id);
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao sincronizar índices');
    } finally {
      setSincronizando(false);
    }
  };

  const formatarData = (data) => {
    if (!data) return '-';
    return new Date(data).toLocaleDateString('pt-BR', {
      month: '2-digit',
      year: 'numeric',
    });
  };

  const formatarValor = (valor) => {
    if (!valor) return '-';
    return parseFloat(valor).toLocaleString('pt-BR', {
      minimumFractionDigits: 4,
      maximumFractionDigits: 8,
    });
  };

  const getBadgeFonte = (fonte) => {
    const cores = {
      API_IBGE: 'success',
      API_BCB: 'primary',
      API_FGV: 'info',
      CSV_IMPORT: 'warning',
      MANUAL: 'secondary',
    };
    return <Badge bg={cores[fonte] || 'secondary'}>{fonte || 'MANUAL'}</Badge>;
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
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-1">Índices Monetários</h2>
          <p className="text-muted mb-0">
            Gerencie os índices de correção monetária
          </p>
        </div>
        <div className="d-flex gap-2">
          <Button
            variant="success"
            onClick={handleSincronizarTodos}
            disabled={sincronizando}
          >
            <FaSync className={`me-2 ${sincronizando ? 'fa-spin' : ''}`} />
            {sincronizando ? 'Sincronizando...' : 'Sincronizar Todos (BCB)'}
          </Button>
          {tabelaSelecionada && (
            <>
              <Button
                variant="outline-success"
                onClick={handleSincronizar}
                disabled={sincronizando}
              >
                <FaSync className="me-2" />
                Sincronizar {tabelaSelecionada.nome}
              </Button>
              <Button variant="primary" onClick={() => setShowImportModal(true)}>
                <FaUpload className="me-2" />
                Importar CSV
              </Button>
            </>
          )}
        </div>
      </div>

      <Row>
        {/* Lista de tabelas */}
        <Col md={4}>
          <Card>
            <Card.Header>
              <strong>Tabelas de Índices</strong>
            </Card.Header>
            <Card.Body className="p-0">
              <div className="list-group list-group-flush">
                {tabelas.map((tabela) => (
                  <button
                    key={tabela.id}
                    className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center ${
                      tabelaSelecionada?.id === tabela.id ? 'active' : ''
                    }`}
                    onClick={() => handleSelecionarTabela(tabela)}
                  >
                    <div>
                      <FaChartLine className="me-2" />
                      <strong>{tabela.nome}</strong>
                      <br />
                      <small className="opacity-75">{tabela.descricao}</small>
                    </div>
                    {tabela.fonteApi && (
                      <Badge
                        bg={tabelaSelecionada?.id === tabela.id ? 'light' : 'secondary'}
                        text={tabelaSelecionada?.id === tabela.id ? 'dark' : 'light'}
                      >
                        {tabela.fonteApi}
                      </Badge>
                    )}
                  </button>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>

        {/* Valores do índice selecionado */}
        <Col md={8}>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <strong>
                Valores - {tabelaSelecionada?.nome || 'Selecione um índice'}
              </strong>
              <span className="text-muted">
                {valores.length} registros
              </span>
            </Card.Header>
            <Card.Body className="p-0">
              {loadingValores ? (
                <div className="text-center py-5">
                  <LoadingSpinner text="Carregando valores..." />
                </div>
              ) : valores.length === 0 ? (
                <div className="text-center py-5 text-muted">
                  <p className="mb-3">Nenhum valor cadastrado</p>
                  <div className="d-flex justify-content-center gap-2">
                    <Button
                      variant="success"
                      onClick={handleSincronizar}
                      disabled={sincronizando}
                    >
                      <FaSync className="me-2" />
                      {sincronizando ? 'Sincronizando...' : 'Buscar do Banco Central'}
                    </Button>
                    <Button
                      variant="outline-primary"
                      onClick={() => setShowImportModal(true)}
                    >
                      <FaUpload className="me-2" />
                      Importar CSV
                    </Button>
                  </div>
                </div>
              ) : (
                <div style={{ maxHeight: '500px', overflowY: 'auto' }}>
                  <Table hover className="mb-0">
                    <thead className="sticky-top bg-white">
                      <tr>
                        <th>Competência</th>
                        <th className="text-end">Valor/Índice</th>
                        <th>Fonte</th>
                      </tr>
                    </thead>
                    <tbody>
                      {valores.map((valor) => (
                        <tr key={valor.id}>
                          <td>{formatarData(valor.competencia)}</td>
                          <td className="text-end font-monospace">
                            {formatarValor(valor.valor)}
                          </td>
                          <td>{getBadgeFonte(valor.fonte)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Modal de importação */}
      <Modal show={showImportModal} onHide={() => setShowImportModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Importar Valores - {tabelaSelecionada?.nome}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label>Arquivo CSV</Form.Label>
            <Form.Control
              type="file"
              accept=".csv"
              onChange={(e) => setArquivo(e.target.files[0])}
            />
            <Form.Text className="text-muted">
              O arquivo deve conter as colunas: competencia (YYYY-MM ou MM/YYYY) e valor
            </Form.Text>
          </Form.Group>

          <div className="alert alert-info small">
            <strong>Exemplo de formato CSV:</strong>
            <br />
            competencia,valor
            <br />
            2024-01,4532.52
            <br />
            2024-02,4546.78
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowImportModal(false)}>
            Cancelar
          </Button>
          <Button
            variant="primary"
            onClick={handleImportar}
            disabled={!arquivo || importando}
          >
            {importando ? 'Importando...' : 'Importar'}
          </Button>
        </Modal.Footer>
      </Modal>
    </Layout>
  );
};

export default IndicesPage;
