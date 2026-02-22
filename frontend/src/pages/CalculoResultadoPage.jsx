import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Container, Row, Col, Card, Table, Button, Badge, Spinner, Alert } from 'react-bootstrap';
import {
  FaFilePdf,
  FaFileWord,
  FaFileExcel,
  FaArrowLeft,
  FaPlay,
  FaGavel,
} from 'react-icons/fa';
import Layout from '../components/common/Layout';
import calculoApi from '../api/calculoApi';
import { toast } from 'react-toastify';

const getBadgeIndiceClass = (nomeIndice) => {
  if (!nomeIndice) return 'badge-indice-default';
  const nome = nomeIndice.toUpperCase();
  if (nome.includes('IPCA-E') || nome.includes('IPCA_E')) return 'badge-indice-ipca-e';
  if (nome.includes('SELIC')) return 'badge-indice-selic';
  if (nome.includes('IPCA')) return 'badge-indice-ipca';
  if (nome.includes('INPC')) return 'badge-indice-inpc';
  if (nome.includes('IGPM') || nome.includes('IGP-M')) return 'badge-indice-igpm';
  if (nome.includes('TR')) return 'badge-indice-tr';
  return 'badge-indice-default';
};

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

      const mimeTypes = {
        pdf: 'application/pdf',
        docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      };
      const blob = new Blob([blobData], {
        type: mimeTypes[formato] || 'application/octet-stream'
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
    return `${Number(valor).toFixed(2)}%`;
  };

  const getStatusBadge = (status) => {
    const variants = {
      'RASCUNHO': 'secondary',
      'CALCULADO': 'success',
      'FINALIZADO': 'primary'
    };
    return <Badge bg={variants[status] || 'secondary'}>{status}</Badge>;
  };

  const getTipoCalculoLabel = (tipo) => {
    if (tipo === 'FAZENDA_PUBLICA') return 'Condenação da Fazenda Pública';
    return 'Cálculo Padrão';
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

  // Parse detalhamento com tratamento de erro
  let detalhamento = [];
  if (resultado?.detalhamento) {
    try {
      detalhamento = JSON.parse(resultado.detalhamento);
      if (!Array.isArray(detalhamento)) detalhamento = [];
    } catch (e) {
      console.warn('Erro ao parsear detalhamento:', e);
      detalhamento = [];
    }
  }

  const isFazendaPublica = calculo?.tipoCalculo === 'FAZENDA_PUBLICA';

  return (
    <Layout>
      <Container fluid className="py-4">
        {/* Cabeçalho */}
        <Row className="mb-4">
          <Col>
            <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
              <div>
                <h2 className="mb-1">
                  {calculo?.titulo}
                  {isFazendaPublica && (
                    <FaGavel className="ms-2 text-warning" size={20} title="Fazenda Pública" />
                  )}
                </h2>
                <p className="text-muted mb-0">
                  {getStatusBadge(calculo?.status)}
                  {isFazendaPublica && (
                    <Badge bg="warning" text="dark" className="ms-2">
                      {getTipoCalculoLabel(calculo?.tipoCalculo)}
                    </Badge>
                  )}
                  {calculo?.processo && (
                    <span className="ms-2">
                      Processo: {calculo.processo.numeroProcesso || 'Sem número'}
                    </span>
                  )}
                </p>
              </div>
              <div className="d-flex align-items-center gap-2 flex-wrap">
                <Button
                  variant="outline-secondary"
                  as={Link}
                  to="/calculos"
                >
                  <FaArrowLeft className="me-1" />
                  Voltar
                </Button>
                {calculo?.status === 'RASCUNHO' && (
                  <Button
                    variant="success"
                    onClick={executarCalculo}
                    disabled={loading}
                  >
                    <FaPlay className="me-1" />
                    Executar Cálculo
                  </Button>
                )}
                {resultado && (
                  <div className="btn-download-group">
                    <button
                      className="btn-download btn-download-pdf"
                      onClick={() => downloadRelatorio('pdf')}
                      disabled={downloading}
                    >
                      <FaFilePdf /> PDF
                    </button>
                    <button
                      className="btn-download btn-download-word"
                      onClick={() => downloadRelatorio('docx')}
                      disabled={downloading}
                    >
                      <FaFileWord /> Word
                    </button>
                    <button
                      className="btn-download btn-download-excel"
                      onClick={() => downloadRelatorio('xlsx')}
                      disabled={downloading}
                    >
                      <FaFileExcel /> Excel
                    </button>
                  </div>
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
                    {isFazendaPublica && (
                      <tr>
                        <td className="text-muted">Tipo de Cálculo:</td>
                        <td>
                          <Badge bg="warning" text="dark">
                            <FaGavel className="me-1" />
                            Fazenda Pública
                          </Badge>
                        </td>
                      </tr>
                    )}
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
                    {!isFazendaPublica && (
                      <>
                        <tr>
                          <td className="text-muted">Índice de Correção:</td>
                          <td>{calculo?.tabelaIndiceNome || '-'}</td>
                        </tr>
                        <tr>
                          <td className="text-muted">Tipo de Juros:</td>
                          <td>{calculo?.tipoJuros}</td>
                        </tr>
                        <tr>
                          <td className="text-muted">Taxa de Juros:</td>
                          <td>{formatarPercentual(calculo?.taxaJuros)} ao {calculo?.periodicidadeJuros?.toLowerCase()}</td>
                        </tr>
                      </>
                    )}
                    {isFazendaPublica && (
                      <tr>
                        <td className="text-muted">Índice/Juros:</td>
                        <td><em className="text-info">Automático conforme legislação</em></td>
                      </tr>
                    )}
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
                      <FaPlay className="me-1" />
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
                        <th>Índice</th>
                      </tr>
                    </thead>
                    <tbody>
                      {calculo.parcelas.map((parcela, index) => (
                        <tr key={parcela.id || index}>
                          <td>{index + 1}</td>
                          <td>{parcela.descricao || '-'}</td>
                          <td>{formatarMoeda(parcela.valorOriginal)}</td>
                          <td>{formatarData(parcela.dataVencimento)}</td>
                          <td>{parcela.tabelaIndiceNome || 'Padrão do cálculo'}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr className="table-secondary">
                        <td colSpan={2}><strong>Total</strong></td>
                        <td colSpan={3}>
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
          <Row className="mb-4">
            <Col>
              <Card>
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <h5 className="mb-0">Evolução Mensal</h5>
                  {isFazendaPublica && (
                    <Badge bg="warning" text="dark">
                      <FaGavel className="me-1" />
                      Índice automático conforme legislação
                    </Badge>
                  )}
                </Card.Header>
                <Card.Body>
                  <div style={{ maxHeight: '500px', overflowY: 'auto', overflowX: 'auto' }}>
                    <Table hover responsive size="sm" className="evolucao-table">
                      <thead className="sticky-top">
                        <tr>
                          <th>Competência</th>
                          <th>Índice Aplicado</th>
                          <th className="text-end">Variação (%)</th>
                          <th className="text-end">Valor Corrigido</th>
                          <th className="text-end">Juros</th>
                          <th className="text-end">Subtotal</th>
                        </tr>
                      </thead>
                      <tbody>
                        {detalhamento.map((item, index) => {
                          const prevIndice = index > 0 ? detalhamento[index - 1].nomeIndice : null;
                          const indiceMudou = prevIndice && item.nomeIndice && prevIndice !== item.nomeIndice;

                          return (
                            <tr key={index} className={indiceMudou ? 'indice-mudou' : ''}>
                              <td>{item.competencia}</td>
                              <td>
                                <span className={`badge-indice ${getBadgeIndiceClass(item.nomeIndice)}`}>
                                  {item.nomeIndice || '-'}
                                </span>
                              </td>
                              <td className="text-end">
                                {item.variacaoPercentual != null
                                  ? `${Number(item.variacaoPercentual).toFixed(4)}%`
                                  : '-'}
                              </td>
                              <td className="text-end">{formatarMoeda(item.valorCorrigidoParcial)}</td>
                              <td className="text-end">{formatarMoeda(item.jurosParcial)}</td>
                              <td className="text-end fw-bold">{formatarMoeda(item.subtotalParcial)}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                      <tfoot>
                        <tr className="table-secondary">
                          <td colSpan={3} className="text-end fw-bold">SUBTOTAL</td>
                          <td className="text-end fw-bold">{formatarMoeda(resultado?.valorCorrigido)}</td>
                          <td className="text-end fw-bold">{formatarMoeda(resultado?.valorJuros)}</td>
                          <td className="text-end fw-bold">
                            {formatarMoeda((resultado?.valorCorrigido || 0) + (resultado?.valorJuros || 0))}
                          </td>
                        </tr>
                        {(resultado?.valorMulta || 0) > 0 && (
                          <tr className="table-warning">
                            <td colSpan={5} className="text-end fw-bold">
                              Multa ({formatarPercentual(calculo?.multaPercentual)})
                            </td>
                            <td className="text-end fw-bold">{formatarMoeda(resultado?.valorMulta)}</td>
                          </tr>
                        )}
                        {(resultado?.valorHonorarios || 0) > 0 && (
                          <tr className="table-warning">
                            <td colSpan={5} className="text-end fw-bold">
                              Honorários ({formatarPercentual(calculo?.honorariosPercentual)})
                            </td>
                            <td className="text-end fw-bold">{formatarMoeda(resultado?.valorHonorarios)}</td>
                          </tr>
                        )}
                        {((resultado?.valorMulta || 0) > 0 || (resultado?.valorHonorarios || 0) > 0) && (
                          <tr className="total-geral">
                            <td colSpan={5} className="text-end fw-bold fs-6">TOTAL GERAL</td>
                            <td className="text-end fw-bold fs-6">{formatarMoeda(resultado?.valorTotal)}</td>
                          </tr>
                        )}
                      </tfoot>
                    </Table>
                  </div>

                  {/* Nota legislativa para Fazenda Pública */}
                  {isFazendaPublica && (
                    <div className="nota-legislativa mt-3">
                      <strong>Fundamentação Legal:</strong><br />
                      <strong>Correção Monetária:</strong>{' '}
                      INPC (01/1984 a 12/1991) | IPCA-E (01/1992 a 08/12/2021) |
                      SELIC taxa única (09/12/2021 a 09/2025) - EC 113/2021 |
                      IPCA + 2% a.a. limitado à SELIC (10/2025+) - EC 136/2025.<br />
                      <strong>Juros Moratórios:</strong>{' '}
                      1% a.m. (até 06/2009) | 0,5% a.m. poupança (07/2009 a 08/12/2021) |
                      SELIC (09/12/2021 a 09/2025) | 2% a.a. (10/2025+).
                    </div>
                  )}
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
