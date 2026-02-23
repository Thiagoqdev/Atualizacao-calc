import { useEffect, useRef, useState } from 'react';
import { Row, Col, Card, Button, Table, Badge, Form } from 'react-bootstrap';
import { Link, useLocation } from 'react-router-dom';
import {
  FaGavel,
  FaCalculator,
  FaPlus,
  FaChartLine,
  FaFileSignature,
  FaCopy,
  FaDownload,
  FaBolt,
  FaArrowRight,
  FaRegFileAlt,
} from 'react-icons/fa';
import { toast } from 'react-toastify';
import Layout from '../components/common/Layout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import processoApi from '../api/processoApi';
import calculoApi from '../api/calculoApi';

const PETICAO_MODELOS = [
  {
    value: 'juntada_memoria',
    label: 'Juntada de memória de cálculo',
    buildText: ({ parteNome, numeroProcesso, pedido, observacoes }) => `EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DE DIREITO

Processo n. ${numeroProcesso || '[informar numero do processo]'}

${parteNome || '[informar nome da parte]'}, por seu advogado, vem, respeitosamente, requerer a JUNTADA DA MEMORIA DE CALCULO atualizada aos autos.

${pedido || 'Requer-se a juntada da planilha e memoriais de calculo para fins de prosseguimento do feito, com a devida apreciacao por este Juizo.'}

${observacoes ? `Observacoes adicionais:\n${observacoes}\n` : ''}Nestes termos,
Pede deferimento.
`,
  },
  {
    value: 'cumprimento_sentenca',
    label: 'Requerimento de cumprimento de sentença',
    buildText: ({ parteNome, numeroProcesso, pedido, observacoes }) => `EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DE DIREITO

Processo n. ${numeroProcesso || '[informar numero do processo]'}

${parteNome || '[informar nome da parte]'}, vem requerer o CUMPRIMENTO DE SENTENCA, com fundamento no titulo judicial formado nos autos.

${pedido || 'Requer o inicio da fase de cumprimento, com intimacao da parte executada para pagamento do valor atualizado apurado nos memoriais anexos.'}

${observacoes ? `Observacoes adicionais:\n${observacoes}\n` : ''}Nestes termos,
Pede deferimento.
`,
  },
  {
    value: 'rpv_precatorio',
    label: 'Pedido de expedição de RPV/Precatório',
    buildText: ({ parteNome, numeroProcesso, pedido, observacoes }) => `EXCELENTISSIMO(A) SENHOR(A) DOUTOR(A) JUIZ(A) DE DIREITO

Processo n. ${numeroProcesso || '[informar numero do processo]'}

${parteNome || '[informar nome da parte]'}, tendo em vista a apuracao do valor atualizado, vem requerer a expedicao de RPV/PRECATORIO, conforme o montante homologado.

${pedido || 'Requer-se a expedicao do requisitorio cabivel, observados os dados constantes nos autos e nos memoriais de calculo apresentados.'}

${observacoes ? `Observacoes adicionais:\n${observacoes}\n` : ''}Nestes termos,
Pede deferimento.
`,
  },
];

const PETICAO_DEFAULT_FORM = {
  modelo: PETICAO_MODELOS[0].value,
  parteNome: '',
  numeroProcesso: '',
  pedido: '',
  observacoes: '',
};

const DashboardPage = () => {
  const location = useLocation();
  const peticoesPanelRef = useRef(null);
  const isPeticoesRoute = location.pathname === '/dashboard/peticoes';

  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalProcessos: 0,
    totalCalculos: 0,
  });
  const [processosRecentes, setProcessosRecentes] = useState([]);
  const [calculosRecentes, setCalculosRecentes] = useState([]);
  const [peticaoForm, setPeticaoForm] = useState(PETICAO_DEFAULT_FORM);
  const [peticaoTexto, setPeticaoTexto] = useState('');

  useEffect(() => {
    carregarDados();
  }, []);

  useEffect(() => {
    if (!peticaoTexto) {
      setPeticaoTexto(gerarTextoPeticao(PETICAO_DEFAULT_FORM));
    }
  }, [peticaoTexto]);

  useEffect(() => {
    if (!isPeticoesRoute || !peticoesPanelRef.current) return;
    peticoesPanelRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [isPeticoesRoute]);

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
        {tipo || '-'}
      </Badge>
    );
  };

  const getBadgeStatus = (status) => {
    const config = {
      RASCUNHO: { bg: 'secondary', label: 'Rascunho' },
      CALCULADO: { bg: 'info', label: 'Calculado' },
      FINALIZADO: { bg: 'success', label: 'Finalizado' },
    };
    const { bg, label } = config[status] || { bg: 'secondary', label: status || '-' };
    return <Badge bg={bg}>{label}</Badge>;
  };

  const gerarTextoPeticao = (dados) => {
    const modelo = PETICAO_MODELOS.find((item) => item.value === dados.modelo) || PETICAO_MODELOS[0];
    return modelo.buildText(dados);
  };

  const atualizarCampoPeticao = (campo, valor) => {
    setPeticaoForm((prev) => ({
      ...prev,
      [campo]: valor,
    }));
  };

  const handleGerarPreviewPeticao = () => {
    setPeticaoTexto(gerarTextoPeticao(peticaoForm));
    toast.success('Preview da petição atualizado');
  };

  const copiarTextoFallback = (texto) => {
    const textarea = document.createElement('textarea');
    textarea.value = texto;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'absolute';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  };

  const handleCopiarPeticao = async () => {
    if (!peticaoTexto.trim()) {
      toast.warning('Gere ou preencha o texto da petição antes de copiar');
      return;
    }

    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(peticaoTexto);
      } else {
        copiarTextoFallback(peticaoTexto);
      }
      toast.success('Texto da petição copiado');
    } catch (error) {
      try {
        copiarTextoFallback(peticaoTexto);
        toast.success('Texto da petição copiado');
      } catch {
        toast.error('Não foi possível copiar o texto');
      }
    }
  };

  const handleBaixarPeticaoTxt = () => {
    if (!peticaoTexto.trim()) {
      toast.warning('Gere ou preencha o texto da petição antes de baixar');
      return;
    }

    const modeloAtual = PETICAO_MODELOS.find((item) => item.value === peticaoForm.modelo);
    const nomeBase = (modeloAtual?.label || 'peticao')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '');

    const blob = new Blob([peticaoTexto], { type: 'text/plain;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${nomeBase || 'peticao'}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    toast.success('Petição em .txt baixada');
  };

  if (loading) {
    return (
      <Layout variant="dashboard">
        <LoadingSpinner />
      </Layout>
    );
  }

  return (
    <Layout variant="dashboard">
      <div className="dashboard-page">
        <section className="dashboard-hero mb-4">
          <div className="dashboard-hero-content">
            <div>
              <div className="dashboard-eyebrow">Painel de Controle</div>
              <h1 className="dashboard-title">Dashboard Jurídico</h1>
              <p className="dashboard-subtitle mb-0">
                Visão consolidada de processos, cálculos e modelos para petições em um só lugar.
              </p>
            </div>

            <div className="dashboard-hero-actions">
              <Button as={Link} to="/calculos/novo" className="dashboard-hero-btn-primary">
                <FaCalculator className="me-2" />
                Novo Cálculo
              </Button>
              <Button as={Link} to="/dashboard/peticoes" className="dashboard-hero-btn-secondary">
                <FaFileSignature className="me-2" />
                Criar Petições
              </Button>
            </div>
          </div>
        </section>

        <Row className="g-3 mb-4">
          <Col md={4}>
            <Card className="dashboard-stat-card stat-processos h-100">
              <Card.Body>
                <div className="dashboard-stat-top">
                  <span className="dashboard-stat-label">Processos cadastrados</span>
                  <span className="dashboard-stat-icon">
                    <FaGavel />
                  </span>
                </div>
                <div className="dashboard-stat-value">{stats.totalProcessos}</div>
                <div className="dashboard-stat-note">Base ativa do escritório</div>
              </Card.Body>
            </Card>
          </Col>

          <Col md={4}>
            <Card className="dashboard-stat-card stat-calculos h-100">
              <Card.Body>
                <div className="dashboard-stat-top">
                  <span className="dashboard-stat-label">Cálculos registrados</span>
                  <span className="dashboard-stat-icon">
                    <FaCalculator />
                  </span>
                </div>
                <div className="dashboard-stat-value">{stats.totalCalculos}</div>
                <div className="dashboard-stat-note">Histórico de apurações</div>
              </Card.Body>
            </Card>
          </Col>

          <Col md={4}>
            <Card className="dashboard-stat-card stat-indices h-100">
              <Card.Body>
                <div className="dashboard-stat-top">
                  <span className="dashboard-stat-label">Índices disponíveis</span>
                  <span className="dashboard-stat-icon">
                    <FaChartLine />
                  </span>
                </div>
                <div className="dashboard-stat-value">5</div>
                <div className="dashboard-stat-note">Tabelas para atualização monetária</div>
              </Card.Body>
            </Card>
          </Col>
        </Row>

        <Row className="g-4 mb-4">
          <Col lg={8}>
            <Card className="dashboard-panel h-100">
              <Card.Body>
                <div className="dashboard-panel-header mb-3">
                  <div>
                    <h5 className="mb-1">Ações rápidas</h5>
                    <p className="dashboard-panel-subtitle mb-0">
                      Atalhos para as tarefas mais frequentes no dia a dia.
                    </p>
                  </div>
                </div>

                <div className="dashboard-actions-grid">
                  <Link className="dashboard-action-card" to="/processos/novo">
                    <div className="dashboard-action-icon icon-processo">
                      <FaPlus />
                    </div>
                    <div>
                      <strong>Novo Processo</strong>
                      <span>Cadastrar processo e iniciar organização.</span>
                    </div>
                    <FaArrowRight className="dashboard-action-arrow" />
                  </Link>

                  <Link className="dashboard-action-card" to="/calculos/novo">
                    <div className="dashboard-action-icon icon-calculo">
                      <FaCalculator />
                    </div>
                    <div>
                      <strong>Novo Cálculo</strong>
                      <span>Gerar atualização monetária e memorial.</span>
                    </div>
                    <FaArrowRight className="dashboard-action-arrow" />
                  </Link>

                  <Link className="dashboard-action-card" to="/dashboard/peticoes">
                    <div className="dashboard-action-icon icon-peticao">
                      <FaFileSignature />
                    </div>
                    <div>
                      <strong>Criar Petições</strong>
                      <span>Montar texto base com preview e exportação.</span>
                    </div>
                    <FaArrowRight className="dashboard-action-arrow" />
                  </Link>

                  <Link className="dashboard-action-card" to="/indices">
                    <div className="dashboard-action-icon icon-indice">
                      <FaChartLine />
                    </div>
                    <div>
                      <strong>Gerenciar Índices</strong>
                      <span>Consultar, importar e sincronizar tabelas.</span>
                    </div>
                    <FaArrowRight className="dashboard-action-arrow" />
                  </Link>
                </div>
              </Card.Body>
            </Card>
          </Col>

          <Col lg={4}>
            <Card className={`dashboard-panel dashboard-highlight-panel h-100 ${isPeticoesRoute ? 'is-focused' : ''}`}>
              <Card.Body>
                <div className="dashboard-highlight-badge">
                  <FaBolt className="me-2" />
                  Novo fluxo
                </div>
                <h5 className="mt-3 mb-2">Criar Petições no Dashboard</h5>
                <p className="text-muted mb-3">
                  Preencha campos essenciais, gere texto base, copie para o editor jurídico e baixe um arquivo
                  `.txt` em segundos.
                </p>
                <div className="d-grid gap-2">
                  <Button as={Link} to="/dashboard/peticoes" variant="dark" className="dashboard-dark-btn">
                    <FaFileSignature className="me-2" />
                    Abrir painel de petições
                  </Button>
                  <Button
                    as={Link}
                    to="/calculos"
                    variant="outline-secondary"
                    className="dashboard-outline-btn"
                  >
                    <FaRegFileAlt className="me-2" />
                    Ver cálculos recentes
                  </Button>
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>

        <Card
          ref={peticoesPanelRef}
          className={`dashboard-panel dashboard-peticoes-panel mb-4 ${isPeticoesRoute ? 'panel-focused' : ''}`}
        >
          <Card.Header className="dashboard-peticoes-header">
            <div>
              <div className="dashboard-peticoes-title-row">
                <h5 className="mb-0">Criar Petições</h5>
                {isPeticoesRoute && <Badge bg="warning" text="dark">Em foco</Badge>}
              </div>
              <small className="text-muted">
                Ferramenta rápida para gerar textos-base de petições a partir de modelos.
              </small>
            </div>
          </Card.Header>
          <Card.Body>
            <Row className="g-4">
              <Col lg={5}>
                <div className="dashboard-peticoes-form">
                  <Form.Group className="mb-3">
                    <Form.Label>Modelo de petição</Form.Label>
                    <Form.Select
                      value={peticaoForm.modelo}
                      onChange={(e) => atualizarCampoPeticao('modelo', e.target.value)}
                    >
                      {PETICAO_MODELOS.map((modelo) => (
                        <option key={modelo.value} value={modelo.value}>
                          {modelo.label}
                        </option>
                      ))}
                    </Form.Select>
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label>Nome da parte</Form.Label>
                    <Form.Control
                      type="text"
                      placeholder="Ex.: João da Silva"
                      value={peticaoForm.parteNome}
                      onChange={(e) => atualizarCampoPeticao('parteNome', e.target.value)}
                    />
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label>Número do processo</Form.Label>
                    <Form.Control
                      type="text"
                      placeholder="0000000-00.0000.0.00.0000"
                      value={peticaoForm.numeroProcesso}
                      onChange={(e) => atualizarCampoPeticao('numeroProcesso', e.target.value)}
                    />
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label>Pedido principal</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={4}
                      placeholder="Descreva o pedido objetivo..."
                      value={peticaoForm.pedido}
                      onChange={(e) => atualizarCampoPeticao('pedido', e.target.value)}
                    />
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label>Observações adicionais</Form.Label>
                    <Form.Control
                      as="textarea"
                      rows={3}
                      placeholder="Informações complementares (opcional)"
                      value={peticaoForm.observacoes}
                      onChange={(e) => atualizarCampoPeticao('observacoes', e.target.value)}
                    />
                  </Form.Group>

                  <div className="dashboard-peticoes-actions">
                    <Button variant="primary" onClick={handleGerarPreviewPeticao}>
                      <FaFileSignature className="me-2" />
                      Atualizar Preview
                    </Button>
                    <Button variant="outline-primary" onClick={handleCopiarPeticao}>
                      <FaCopy className="me-2" />
                      Copiar texto
                    </Button>
                    <Button variant="outline-secondary" onClick={handleBaixarPeticaoTxt}>
                      <FaDownload className="me-2" />
                      Baixar .txt
                    </Button>
                  </div>
                </div>
              </Col>

              <Col lg={7}>
                <div className="dashboard-peticoes-preview-wrap">
                  <div className="dashboard-preview-header">
                    <span>Preview editável</span>
                    <small>Você pode ajustar livremente o texto antes de copiar/baixar.</small>
                  </div>
                  <Form.Control
                    as="textarea"
                    rows={18}
                    className="dashboard-peticoes-preview"
                    value={peticaoTexto}
                    onChange={(e) => setPeticaoTexto(e.target.value)}
                  />
                </div>
              </Col>
            </Row>
          </Card.Body>
        </Card>

        <Row className="g-4">
          <Col xl={6}>
            <Card className="dashboard-panel dashboard-table-card h-100">
              <Card.Header className="d-flex justify-content-between align-items-center">
                <div>
                  <span className="fw-semibold">Processos recentes</span>
                  <div className="dashboard-table-subtitle">Últimos cadastros</div>
                </div>
                <Button as={Link} to="/processos" variant="link" size="sm" className="p-0">
                  Ver todos
                </Button>
              </Card.Header>
              <Card.Body className="p-0">
                {processosRecentes.length === 0 ? (
                  <div className="dashboard-empty-state">
                    <FaGavel className="dashboard-empty-icon" />
                    <p className="mb-2">Nenhum processo cadastrado</p>
                    <Button as={Link} to="/processos/novo" variant="primary" size="sm">
                      <FaPlus className="me-2" />
                      Criar primeiro processo
                    </Button>
                  </div>
                ) : (
                  <div className="table-responsive">
                    <Table hover className="mb-0 dashboard-table">
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
                  </div>
                )}
              </Card.Body>
            </Card>
          </Col>

          <Col xl={6}>
            <Card className="dashboard-panel dashboard-table-card h-100">
              <Card.Header className="d-flex justify-content-between align-items-center">
                <div>
                  <span className="fw-semibold">Cálculos recentes</span>
                  <div className="dashboard-table-subtitle">Acompanhamento operacional</div>
                </div>
                <Button as={Link} to="/calculos" variant="link" size="sm" className="p-0">
                  Ver todos
                </Button>
              </Card.Header>
              <Card.Body className="p-0">
                {calculosRecentes.length === 0 ? (
                  <div className="dashboard-empty-state">
                    <FaCalculator className="dashboard-empty-icon" />
                    <p className="mb-2">Nenhum cálculo realizado</p>
                    <Button as={Link} to="/calculos/novo" variant="primary" size="sm">
                      <FaPlus className="me-2" />
                      Criar primeiro cálculo
                    </Button>
                  </div>
                ) : (
                  <div className="table-responsive">
                    <Table hover className="mb-0 dashboard-table">
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
                  </div>
                )}
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </div>
    </Layout>
  );
};

export default DashboardPage;
