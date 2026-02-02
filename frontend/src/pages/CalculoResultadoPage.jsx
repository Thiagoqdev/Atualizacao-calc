import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Container, Row, Col, Card, Table, Button, Badge, Spinner, Alert, Tabs, Tab } from 'react-bootstrap';
import Layout from '../components/common/Layout';
import calculoApi from '../api/calculoApi';
import { toast } from 'react-toastify';

function CalculoResultadoPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [calculo, setCalculo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    carregarCalculo();
  }, [id]);

  const carregarCalculo = async () => {
    try {
      setLoading(true);
      const response = await calculoApi.buscarPorId(id);
      setCalculo(response.data);
    } catch (err) {
      setError('Erro ao carregar cálculo');
      toast.error('Erro ao carregar cálculo');
    } finally {
      setLoading(false);
    }
  };

  const executarCalculo = async () => {
    try {
      setLoading(true);
      const response = await calculoApi.executar(id);
      setCalculo(response.data);
      toast.success('Cálculo executado com sucesso!');
    } catch (err) {
      toast.error('Erro ao executar cálculo');
    } finally {
      setLoading(false);
    }
  };

  const downloadRelatorio = async (formato) => {
    try {
      setDownloading(true);
      const blobData = await calculoApi.downloadRelatorio(id, formato);

      const blob = new Blob([blobData], {
        type: formato === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      });

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `calculo_${id}.${formato}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success(`Relatório ${formato.toUpperCase()} baixado com sucesso!`);
    } catch (err) {
      toast.error('Erro ao baixar relatório');
    } finally {
      setDownloading(false);
    }
  };

  const formatarMoeda = (valor) => {
    if (valor == null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  };

  const formatarData = (data) => {
    if (!data) return '-';
    return new Date(data).toLocaleDateString('pt-BR');
  };

  const formatarPercentual = (valor) => {
    if (valor == null) return '0%';
    return `${valor.toFixed(2)}%`;
  };

  const getStatusBadge = (status) => {
    const variants = {
      'RASCUNHO': 'secondary',
      'CALCULADO': 'success',
      'FINALIZADO': 'primary'
    };
    return <Badge bg={variants[status] || 'secondary'}>{status}</Badge>;
  };

  if (loading) {
    return (
      <Layout>
        <Container className="d-flex justify-content-center align-items-center" style={{ minHeight: '400px' }}>
          <Spinner animation="border" variant="primary" />
        </Container>
      </Layout>
    );
  }

  if (error) {
    return (
      <Layout>
        <Container>
          <Alert variant="danger">{error}</Alert>
          <Button variant="secondary" onClick={() => navigate('/calculos')}>
            Voltar para Cálculos
          </Button>
        </Container>
      </Layout>
    );
  }

  const resultado = calculo?.resultado;
  const detalhamento = resultado?.detalhamento ? JSON.parse(resultado.detalhamento) : [];

  return (
    <Layout>
      <Container fluid className="py-4">
        {/* Cabeçalho */}
        <Row className="mb-4">
          <Col>
            <div className="d-flex justify-content-between align-items-center">
              <div>
                <h2 className="mb-1">{calculo?.titulo}</h2>
                <p className="text-muted mb-0">
                  {getStatusBadge(calculo?.status)}
                  {calculo?.processo && (
                    <span className="ms-2">
                      Processo: {calculo.processo.numeroProcesso || 'Sem número'}
                    </span>
                  )}
                </p>
              </div>
              <div>
                <Button
                  variant="outline-secondary"
                  className="me-2"
                  as={Link}
                  to="/calculos"
                >
                  <i className="bi bi-arrow-left me-1"></i>
                  Voltar
                </Button>
                {calculo?.status === 'RASCUNHO' && (
                  <Button
                    variant="success"
                    className="me-2"
                    onClick={executarCalculo}
                    disabled={loading}
                  >
                    <i className="bi bi-play-fill me-1"></i>
                    Executar Cálculo
                  </Button>
                )}
                {resultado && (
                  <>
                    <Button
                      variant="danger"
                      className="me-2"
                      onClick={() => downloadRelatorio('pdf')}
                      disabled={downloading}
                    >
                      <i className="bi bi-file-pdf me-1"></i>
                      PDF
                    </Button>
                    <Button
                      variant="success"
                      onClick={() => downloadRelatorio('xlsx')}
                      disabled={downloading}
                    >
                      <i className="bi bi-file-excel me-1"></i>
                      Excel
                    </Button>
                  </>
                )}
              </div>
            </div>
          </Col>
        </Row>

        {/* Parâmetros do Cálculo */}
        <Row className="mb-4">
          <Col md={6}>
            <Card>
              <Card.Header>
                <h5 className="mb-0">Parâmetros do Cálculo</h5>
              </Card.Header>
              <Card.Body>
                <Table borderless size="sm">
                  <tbody>
                    <tr>
                      <td className="text-muted">Valor Principal:</td>
                      <td className="fw-bold">{formatarMoeda(calculo?.valorPrincipal)}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Data Inicial:</td>
                      <td>{formatarData(calculo?.dataInicial)}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Data Final:</td>
                      <td>{formatarData(calculo?.dataFinal)}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Índice de Correção:</td>
                      <td>{calculo?.tabelaIndice?.nome || '-'}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Tipo de Juros:</td>
                      <td>{calculo?.tipoJuros}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Taxa de Juros:</td>
                      <td>{formatarPercentual(calculo?.taxaJuros)} ao {calculo?.periodicidadeJuros?.toLowerCase()}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Multa:</td>
                      <td>{formatarPercentual(calculo?.multaPercentual)}</td>
                    </tr>
                    <tr>
                      <td className="text-muted">Honorários:</td>
                      <td>{formatarPercentual(calculo?.honorariosPercentual)}</td>
                    </tr>
                  </tbody>
                </Table>
              </Card.Body>
            </Card>
          </Col>

          {/* Resultado */}
          <Col md={6}>
            <Card className={resultado ? 'border-success' : ''}>
              <Card.Header className={resultado ? 'bg-success text-white' : ''}>
                <h5 className="mb-0">Resultado do Cálculo</h5>
              </Card.Header>
              <Card.Body>
                {resultado ? (
                  <Table borderless size="sm">
                    <tbody>
                      <tr>
                        <td className="text-muted">Valor Corrigido:</td>
                        <td className="fw-bold">{formatarMoeda(resultado.valorCorrigido)}</td>
                      </tr>
                      <tr>
                        <td className="text-muted">Juros:</td>
                        <td className="fw-bold">{formatarMoeda(resultado.valorJuros)}</td>
                      </tr>
                      <tr>
                        <td className="text-muted">Multa:</td>
                        <td>{formatarMoeda(resultado.valorMulta)}</td>
                      </tr>
                      <tr>
                        <td className="text-muted">Honorários:</td>
                        <td>{formatarMoeda(resultado.valorHonorarios)}</td>
                      </tr>
                      <tr className="border-top">
                        <td className="text-muted fs-5">TOTAL:</td>
                        <td className="fw-bold fs-4 text-success">
                          {formatarMoeda(resultado.valorTotal)}
                        </td>
                      </tr>
                    </tbody>
                  </Table>
                ) : (
                  <div className="text-center py-4">
                    <p className="text-muted mb-3">Cálculo ainda não foi executado</p>
                    <Button variant="success" onClick={executarCalculo}>
                      <i className="bi bi-play-fill me-1"></i>
                      Executar Agora
                    </Button>
                  </div>
                )}
              </Card.Body>
            </Card>
          </Col>
        </Row>

        {/* Parcelas */}
        {calculo?.parcelas && calculo.parcelas.length > 0 && (
          <Row className="mb-4">
            <Col>
              <Card>
                <Card.Header>
                  <h5 className="mb-0">Parcelas ({calculo.parcelas.length})</h5>
                </Card.Header>
                <Card.Body>
                  <Table striped hover responsive>
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Descrição</th>
                        <th>Valor Original</th>
                        <th>Data Vencimento</th>
                      </tr>
                    </thead>
                    <tbody>
                      {calculo.parcelas.map((parcela, index) => (
                        <tr key={parcela.id || index}>
                          <td>{index + 1}</td>
                          <td>{parcela.descricao || '-'}</td>
                          <td>{formatarMoeda(parcela.valorOriginal)}</td>
                          <td>{formatarData(parcela.dataVencimento)}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr className="table-secondary">
                        <td colSpan={2}><strong>Total</strong></td>
                        <td colSpan={2}>
                          <strong>
                            {formatarMoeda(
                              calculo.parcelas.reduce((sum, p) => sum + (p.valorOriginal || 0), 0)
                            )}
                          </strong>
                        </td>
                      </tr>
                    </tfoot>
                  </Table>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        )}

        {/* Detalhamento Mensal */}
        {detalhamento.length > 0 && (
          <Row>
            <Col>
              <Card>
                <Card.Header>
                  <h5 className="mb-0">Evolução Mensal</h5>
                </Card.Header>
                <Card.Body>
                  <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                    <Table striped hover responsive size="sm">
                      <thead className="table-light sticky-top">
                        <tr>
                          <th>Competência</th>
                          <th>Índice</th>
                          <th>Fator Acumulado</th>
                          <th>Valor Corrigido</th>
                          <th>Juros Acumulados</th>
                          <th>Total Parcial</th>
                        </tr>
                      </thead>
                      <tbody>
                        {detalhamento.map((item, index) => (
                          <tr key={index}>
                            <td>{item.competencia}</td>
                            <td>{item.indice?.toFixed(4) || '-'}</td>
                            <td>{item.fatorAcumulado?.toFixed(6) || '-'}</td>
                            <td>{formatarMoeda(item.valorCorrigido)}</td>
                            <td>{formatarMoeda(item.jurosAcumulados)}</td>
                            <td className="fw-bold">{formatarMoeda(item.totalParcial)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        )}
      </Container>
    </Layout>
  );
}

export default CalculoResultadoPage;
