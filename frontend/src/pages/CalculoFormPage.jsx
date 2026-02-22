import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Row,
  Col,
  Card,
  Button,
  Form,
  Table,
  Alert,
  Modal,
  OverlayTrigger,
  Tooltip,
} from 'react-bootstrap';
import { useForm, useFieldArray } from 'react-hook-form';
import { toast } from 'react-toastify';
import {
  FaCalculator,
  FaSave,
  FaPlus,
  FaTrash,
  FaArrowLeft,
  FaLayerGroup,
  FaFilePdf,
  FaFileWord,
  FaFileExcel,
  FaGavel,
  FaInfoCircle,
} from 'react-icons/fa';
import Layout from '../components/common/Layout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import calculoApi from '../api/calculoApi';
import processoApi from '../api/processoApi';
import indiceApi from '../api/indiceApi';

const TIPOS_JUROS = [
  { value: 'SIMPLES', label: 'Juros Simples' },
  { value: 'COMPOSTO', label: 'Juros Compostos' },
];

const PERIODICIDADES = [
  { value: 'DIARIO', label: 'Diário' },
  { value: 'MENSAL', label: 'Mensal' },
  { value: 'ANUAL', label: 'Anual' },
];

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

const CalculoFormPage = () => {
  const [searchParams] = useSearchParams();
  const processoId = searchParams.get('processoId');
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [calculando, setCalculando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [indices, setIndices] = useState([]);
  const [processo, setProcesso] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [downloadingPreview, setDownloadingPreview] = useState(false);
  const [view, setView] = useState('form'); // 'form' | 'resultado'
  const [showModalVarias, setShowModalVarias] = useState(false);
  const [modalVarias, setModalVarias] = useState({
    quantidade: 1,
    valorParcela: '',
    descricaoBase: 'Parcela',
    tipoVencimento: 'subsequentes',
    dataInicialParcelas: '',
    tabelaIndiceId: '',
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm({
    defaultValues: {
      titulo: '',
      tipoCalculo: 'PADRAO',
      valorPrincipal: '',
      dataInicial: '',
      dataFinal: new Date().toISOString().split('T')[0],
      tabelaIndiceId: '',
      tipoJuros: 'SIMPLES',
      taxaJuros: '1',
      periodicidadeJuros: 'MENSAL',
      multaPercentual: '0',
      honorariosPercentual: '0',
      jurosSobreCorrigido: true,
      mostrarMemorial: false,
      parcelas: [],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'parcelas',
  });

  const tipoCalculo = watch('tipoCalculo');
  const isFazendaPublica = tipoCalculo === 'FAZENDA_PUBLICA';

  useEffect(() => {
    carregarDados();
  }, [processoId]);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [indicesRes] = await Promise.all([indiceApi.listarTabelas()]);
      setIndices(indicesRes);

      if (processoId) {
        const processoRes = await processoApi.buscarPorId(processoId);
        setProcesso(processoRes);
      }
    } catch (error) {
      toast.error('Erro ao carregar dados');
    } finally {
      setLoading(false);
    }
  };

  const handleCalcular = async (data) => {
    setCalculando(true);
    setResultado(null);

    try {
      const payload = prepararPayload(data);
      const res = await calculoApi.preview(payload);
      setResultado(res);
      setView('resultado');
      toast.success('Cálculo realizado com sucesso!');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao calcular');
    } finally {
      setCalculando(false);
    }
  };

  const handleSalvar = async () => {
    if (!processoId) {
      toast.warning('Selecione um processo para salvar o cálculo');
      return;
    }

    setSalvando(true);
    try {
      const data = watch();
      const payload = prepararPayload(data);
      const calculo = await calculoApi.criar(processoId, payload);
      toast.success('Cálculo salvo com sucesso!');
      navigate(`/calculos/${calculo.id}`);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar cálculo');
    } finally {
      setSalvando(false);
    }
  };

  const handleVoltar = () => {
    setView('form');
  };

  const handleDownloadPreview = async (formato) => {
    const data = watch();
    setDownloadingPreview(true);
    try {
      const payload = prepararPayload(data);
      const blobData = await calculoApi.previewRelatorio(payload, formato);
      const mimeTypes = {
        pdf: 'application/pdf',
        docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      };
      const blob = new Blob([blobData], {
        type: mimeTypes[formato] || 'application/octet-stream',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `memorial_calculo.${formato}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      toast.success(`Relatório ${formato.toUpperCase()} baixado com sucesso!`);
    } catch (error) {
      toast.error('Erro ao gerar relatório');
    } finally {
      setDownloadingPreview(false);
    }
  };

  const prepararPayload = (data) => {
    const payload = {
      titulo: data.titulo || `Cálculo ${new Date().toLocaleDateString('pt-BR')}`,
      tipoCalculo: data.tipoCalculo || 'PADRAO',
      valorPrincipal: parseFloat(data.valorPrincipal),
      dataInicial: data.dataInicial,
      dataFinal: data.dataFinal,
      tipoJuros: data.tipoJuros,
      taxaJuros: parseFloat(data.taxaJuros) || 0,
      periodicidadeJuros: data.periodicidadeJuros,
      multaPercentual: parseFloat(data.multaPercentual) || 0,
      honorariosPercentual: parseFloat(data.honorariosPercentual) || 0,
      jurosSobreCorrigido: data.jurosSobreCorrigido,
    };

    if (data.tabelaIndiceId && data.tipoCalculo !== 'FAZENDA_PUBLICA') {
      payload.tabelaIndiceId = parseInt(data.tabelaIndiceId);
    }

    if (data.parcelas && data.parcelas.length > 0) {
      payload.parcelas = data.parcelas.map((p) => ({
        descricao: p.descricao,
        valorOriginal: parseFloat(p.valorOriginal),
        dataVencimento: p.dataVencimento,
        tabelaIndiceId: p.tabelaIndiceId ? parseInt(p.tabelaIndiceId) : null,
      }));
    }

    return payload;
  };

  const formatarMoeda = (valor) => {
    if (!valor && valor !== 0) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(valor);
  };

  const adicionarParcela = () => {
    append({ descricao: '', valorOriginal: '', dataVencimento: '', tabelaIndiceId: '' });
  };

  const adicionarVariasParcelas = () => {
    const { quantidade, valorParcela, descricaoBase, tipoVencimento, dataInicialParcelas, tabelaIndiceId } = modalVarias;

    if (!quantidade || quantidade < 1 || !valorParcela) {
      toast.warning('Preencha a quantidade e o valor das parcelas');
      return;
    }

    const dataInicialCalculo = watch('dataInicial');
    let dataBase;
    if (tipoVencimento === 'subsequentes') {
      if (!dataInicialCalculo) {
        toast.warning('Defina a data inicial do cálculo primeiro');
        return;
      }
      dataBase = new Date(dataInicialCalculo + 'T00:00:00');
    } else {
      if (!dataInicialParcelas) {
        toast.warning('Informe a data inicial das parcelas');
        return;
      }
      dataBase = new Date(dataInicialParcelas + 'T00:00:00');
    }

    const novasParcelas = [];
    for (let i = 0; i < parseInt(quantidade); i++) {
      const dataVencimento = new Date(dataBase);
      dataVencimento.setMonth(dataVencimento.getMonth() + i);
      const dataFormatada = dataVencimento.toISOString().split('T')[0];

      novasParcelas.push({
        descricao: `${descricaoBase} ${i + 1}`,
        valorOriginal: valorParcela,
        dataVencimento: dataFormatada,
        tabelaIndiceId: tabelaIndiceId || '',
      });
    }

    append(novasParcelas);
    setShowModalVarias(false);
    setModalVarias({
      quantidade: 1,
      valorParcela: '',
      descricaoBase: 'Parcela',
      tipoVencimento: 'subsequentes',
      dataInicialParcelas: '',
      tabelaIndiceId: '',
    });
    toast.success(`${quantidade} parcela(s) adicionada(s)!`);
  };

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  // ===================================================================
  //  RENDER: VIEW RESULTADO (SPA — substitui o formulário)
  // ===================================================================
  if (view === 'resultado' && resultado) {
    return (
      <Layout>
        <div className="view-transition-enter">
          {/* Cabeçalho do resultado */}
          <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
            <div>
              <Button variant="outline-secondary" onClick={handleVoltar}>
                <FaArrowLeft className="me-2" />
                Voltar ao Formulário
              </Button>
            </div>
            <div className="btn-download-group">
              <button
                className="btn-download btn-download-pdf"
                onClick={() => handleDownloadPreview('pdf')}
                disabled={downloadingPreview}
                type="button"
              >
                <FaFilePdf /> PDF
              </button>
              <button
                className="btn-download btn-download-word"
                onClick={() => handleDownloadPreview('docx')}
                disabled={downloadingPreview}
                type="button"
              >
                <FaFileWord /> Word
              </button>
              <button
                className="btn-download btn-download-excel"
                onClick={() => handleDownloadPreview('xlsx')}
                disabled={downloadingPreview}
                type="button"
              >
                <FaFileExcel /> Excel
              </button>
            </div>
          </div>

          {/* Card de resumo */}
          <Card className="mb-4 border-success">
            <Card.Header className="bg-success text-white">
              <strong>Resultado do Cálculo</strong>
              {isFazendaPublica && (
                <span className="ms-2 badge bg-warning text-dark">Fazenda Pública</span>
              )}
            </Card.Header>
            <Card.Body>
              <div className="resultado-calculo mb-4">
                <p className="mb-1 opacity-75">Valor Total Atualizado</p>
                <p className="valor-total mb-0">
                  {formatarMoeda(resultado.valorTotal)}
                </p>
              </div>

              <Row className="mb-3">
                <Col sm={6} md={3}>
                  <div className="resultado-item">
                    <small className="text-muted">Valor Original</small>
                    <p className="mb-0 fw-bold">
                      {formatarMoeda(resultado.valorOriginal)}
                    </p>
                  </div>
                </Col>
                <Col sm={6} md={3}>
                  <div className="resultado-item">
                    <small className="text-muted">Valor Corrigido</small>
                    <p className="mb-0 fw-bold">
                      {formatarMoeda(resultado.valorCorrigido)}
                    </p>
                  </div>
                </Col>
                <Col sm={6} md={3}>
                  <div className="resultado-item">
                    <small className="text-muted">Juros</small>
                    <p className="mb-0 fw-bold">
                      {formatarMoeda(resultado.valorJuros)}
                    </p>
                  </div>
                </Col>
                <Col sm={6} md={3}>
                  <div className="resultado-item">
                    <small className="text-muted">Multa + Honorários</small>
                    <p className="mb-0 fw-bold">
                      {formatarMoeda((resultado.valorMulta || 0) + (resultado.valorHonorarios || 0))}
                    </p>
                  </div>
                </Col>
              </Row>

              <Row>
                <Col sm={6} md={3}>
                  <div className="resultado-item">
                    <small className="text-muted">Multa</small>
                    <p className="mb-0">{formatarMoeda(resultado.valorMulta)}</p>
                  </div>
                </Col>
                <Col sm={6} md={3}>
                  <div className="resultado-item">
                    <small className="text-muted">Honorários</small>
                    <p className="mb-0">{formatarMoeda(resultado.valorHonorarios)}</p>
                  </div>
                </Col>
                <Col sm={6} md={3}>
                  <div className="resultado-item">
                    <small className="text-muted">Fator de Correção</small>
                    <p className="mb-0">{resultado.fatorCorrecao != null ? Number(resultado.fatorCorrecao).toFixed(6) : '-'}</p>
                  </div>
                </Col>
                {resultado.variacaoTotalPeriodo != null && (
                  <Col sm={6} md={3}>
                    <div className="resultado-item">
                      <small className="text-muted">Variação Total</small>
                      <p className="mb-0">{Number(resultado.variacaoTotalPeriodo).toFixed(4)}%</p>
                    </div>
                  </Col>
                )}
              </Row>
            </Card.Body>
          </Card>

          {/* Detalhamento por parcela */}
          {resultado.parcelas && resultado.parcelas.length > 0 && (
            <Card className="mb-4">
              <Card.Header>
                <strong>Detalhamento por Parcela</strong>
              </Card.Header>
              <Card.Body>
                <div style={{ overflowX: 'auto' }}>
                  <Table size="sm" className="detalhamento-table">
                    <thead>
                      <tr>
                        <th>Descrição</th>
                        <th>Índice</th>
                        <th className="text-end">Corrigido</th>
                        <th className="text-end">Juros</th>
                        <th className="text-end">Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {resultado.parcelas.map((parcela, idx) => (
                        <tr key={idx}>
                          <td>{parcela.descricao || `Parcela ${idx + 1}`}</td>
                          <td>
                            <span className={`badge-indice ${getBadgeIndiceClass(parcela.nomeIndice || parcela.indiceNome)}`}>
                              {parcela.nomeIndice || parcela.indiceNome || 'Padrão'}
                            </span>
                          </td>
                          <td className="text-end">
                            {formatarMoeda(parcela.valorCorrigido)}
                          </td>
                          <td className="text-end">
                            {formatarMoeda(parcela.valorJuros)}
                          </td>
                          <td className="text-end fw-bold">
                            {formatarMoeda(parcela.subtotal)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              </Card.Body>
            </Card>
          )}

          {/* Memorial de Cálculo - Evolução Mensal */}
          {resultado.detalhamento && resultado.detalhamento.length > 0 && (
            <Card className="mb-4">
              <Card.Header>
                <strong>Memorial de Cálculo - Evolução Mensal</strong>
              </Card.Header>
              <Card.Body>
                {resultado.variacaoTotalPeriodo != null && (
                  <Alert variant="info" className="py-2">
                    <strong>Variação total no período:</strong>{' '}
                    {Number(resultado.variacaoTotalPeriodo).toFixed(4)}%
                  </Alert>
                )}
                <div style={{ overflowX: 'auto' }}>
                  <Table size="sm" className="evolucao-table">
                    <thead>
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
                      {resultado.detalhamento.map((item, idx) => {
                        const prevIndice = idx > 0 ? resultado.detalhamento[idx - 1].nomeIndice : null;
                        const indiceMudou = prevIndice && item.nomeIndice && prevIndice !== item.nomeIndice;

                        return (
                          <tr key={idx} className={indiceMudou ? 'indice-mudou' : ''}>
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
                        <td className="text-end fw-bold">
                          {formatarMoeda(resultado.valorCorrigido)}
                        </td>
                        <td className="text-end fw-bold">
                          {formatarMoeda(resultado.valorJuros)}
                        </td>
                        <td className="text-end fw-bold">
                          {formatarMoeda((resultado.valorCorrigido || 0) + (resultado.valorJuros || 0))}
                        </td>
                      </tr>
                      {(resultado.valorMulta || 0) > 0 && (
                        <tr className="table-warning">
                          <td colSpan={5} className="text-end fw-bold">
                            Multa ({watch('multaPercentual') || 0}%)
                          </td>
                          <td className="text-end fw-bold">
                            {formatarMoeda(resultado.valorMulta)}
                          </td>
                        </tr>
                      )}
                      {(resultado.valorHonorarios || 0) > 0 && (
                        <tr className="table-warning">
                          <td colSpan={5} className="text-end fw-bold">
                            Honorários ({watch('honorariosPercentual') || 0}%)
                          </td>
                          <td className="text-end fw-bold">
                            {formatarMoeda(resultado.valorHonorarios)}
                          </td>
                        </tr>
                      )}
                      {((resultado.valorMulta || 0) > 0 || (resultado.valorHonorarios || 0) > 0) && (
                        <tr className="total-geral">
                          <td colSpan={5} className="text-end fw-bold fs-6">TOTAL GERAL</td>
                          <td className="text-end fw-bold fs-6">
                            {formatarMoeda(resultado.valorTotal)}
                          </td>
                        </tr>
                      )}
                    </tfoot>
                  </Table>
                </div>
              </Card.Body>
            </Card>
          )}

          {/* Botão salvar */}
          <div className="d-flex gap-2 mb-4">
            {processoId && (
              <Button
                variant="success"
                size="lg"
                onClick={handleSalvar}
                disabled={salvando}
              >
                <FaSave className="me-2" />
                {salvando ? 'Salvando...' : 'Salvar Cálculo'}
              </Button>
            )}
            <Button variant="outline-secondary" size="lg" onClick={handleVoltar}>
              <FaArrowLeft className="me-2" />
              Novo Cálculo
            </Button>
          </div>
        </div>
      </Layout>
    );
  }

  // ===================================================================
  //  RENDER: VIEW FORMULÁRIO
  // ===================================================================
  return (
    <Layout>
      <div className={view === 'form' ? 'view-transition-enter' : ''}>
        <div className="d-flex justify-content-between align-items-center mb-4">
          <div>
            <Button
              variant="link"
              className="p-0 mb-2"
              onClick={() => navigate(-1)}
            >
              <FaArrowLeft className="me-2" />
              Voltar
            </Button>
            <h2 className="mb-1">Novo Cálculo</h2>
            {processo && (
              <p className="text-muted mb-0">
                Processo: {processo.numeroProcesso || `#${processo.id}`}
              </p>
            )}
          </div>
        </div>

        <Form>
          {/* Tipo de Cálculo */}
          <Card className="mb-4">
            <Card.Header>
              <strong>Tipo de Cálculo</strong>
            </Card.Header>
            <Card.Body>
              <Row>
                <Col md={6}>
                  <div
                    className={`tipo-calculo-card ${!isFazendaPublica ? 'active' : ''}`}
                    onClick={() => setValue('tipoCalculo', 'PADRAO')}
                  >
                    <div className="d-flex align-items-center">
                      <FaCalculator className="me-2 text-primary" />
                      <div>
                        <div className="tipo-titulo">Cálculo Padrão</div>
                        <div className="tipo-descricao">Índice único selecionado pelo usuário</div>
                      </div>
                    </div>
                  </div>
                </Col>
                <Col md={6}>
                  <div
                    className={`tipo-calculo-card ${isFazendaPublica ? 'active' : ''}`}
                    onClick={() => setValue('tipoCalculo', 'FAZENDA_PUBLICA')}
                  >
                    <div className="d-flex align-items-center">
                      <FaGavel className="me-2 text-warning" />
                      <div>
                        <div className="tipo-titulo">Fazenda Pública</div>
                        <div className="tipo-descricao">Índice automático conforme legislação federal</div>
                      </div>
                    </div>
                  </div>
                </Col>
              </Row>
              <input type="hidden" {...register('tipoCalculo')} />

              {/* Info Fazenda Pública */}
              {isFazendaPublica && (
                <div className="nota-legislativa mt-3">
                  <strong>Legislação aplicada automaticamente:</strong>{' '}
                  INPC (1984-1991) | IPCA-E (1992-08/12/2021) | SELIC taxa única (09/12/2021-09/2025) - EC 113/2021 |
                  IPCA + 2% a.a. limitado à SELIC (10/2025+) - EC 136/2025.
                  Juros moratórios: 1% a.m. (até 06/2009) | 0,5% a.m. (07/2009-08/12/2021) | SELIC/IPCA+2% conforme EC vigente.
                </div>
              )}
            </Card.Body>
          </Card>

          <Card className="mb-4">
            <Card.Header>
              <strong>Dados do Cálculo</strong>
            </Card.Header>
            <Card.Body>
              <Form.Group className="mb-3">
                <Form.Label>Título</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="Ex: Diferenças salariais 2020-2024"
                  {...register('titulo')}
                />
              </Form.Group>

              <Row>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Valor Principal *</Form.Label>
                    <Form.Control
                      type="number"
                      step="0.01"
                      placeholder="0,00"
                      {...register('valorPrincipal', {
                        required: 'Valor é obrigatório',
                        min: { value: 0.01, message: 'Valor deve ser maior que zero' },
                      })}
                      isInvalid={!!errors.valorPrincipal}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.valorPrincipal?.message}
                    </Form.Control.Feedback>
                  </Form.Group>
                </Col>
                {!isFazendaPublica && (
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Índice de Correção</Form.Label>
                      <Form.Select {...register('tabelaIndiceId')}>
                        <option value="">Sem correção monetária</option>
                        {indices.map((indice) => (
                          <option key={indice.id} value={indice.id}>
                            {indice.nome} - {indice.descricao}
                          </option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  </Col>
                )}
                {isFazendaPublica && (
                  <Col md={6}>
                    <Form.Group className="mb-3">
                      <Form.Label>Índice de Correção</Form.Label>
                      <Form.Control
                        type="text"
                        value="Automático conforme legislação"
                        disabled
                        className="bg-light"
                      />
                      <Form.Text className="text-muted">
                        INPC, IPCA-E, SELIC ou IPCA conforme o período
                      </Form.Text>
                    </Form.Group>
                  </Col>
                )}
              </Row>

              <Row>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Data Inicial *</Form.Label>
                    <Form.Control
                      type="date"
                      {...register('dataInicial', {
                        required: 'Data inicial é obrigatória',
                      })}
                      isInvalid={!!errors.dataInicial}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.dataInicial?.message}
                    </Form.Control.Feedback>
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Data Final *</Form.Label>
                    <Form.Control
                      type="date"
                      {...register('dataFinal', {
                        required: 'Data final é obrigatória',
                      })}
                      isInvalid={!!errors.dataFinal}
                    />
                    <Form.Control.Feedback type="invalid">
                      {errors.dataFinal?.message}
                    </Form.Control.Feedback>
                  </Form.Group>
                </Col>
              </Row>
            </Card.Body>
          </Card>

          {/* Juros - ocultar quando Fazenda Pública */}
          {!isFazendaPublica && (
            <Card className="mb-4">
              <Card.Header>
                <strong>Juros</strong>
              </Card.Header>
              <Card.Body>
                <Row>
                  <Col md={4}>
                    <Form.Group className="mb-3">
                      <Form.Label>Tipo de Juros</Form.Label>
                      <Form.Select {...register('tipoJuros')}>
                        {TIPOS_JUROS.map((tipo) => (
                          <option key={tipo.value} value={tipo.value}>
                            {tipo.label}
                          </option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group className="mb-3">
                      <Form.Label>Taxa (%)</Form.Label>
                      <Form.Control
                        type="number"
                        step="0.01"
                        {...register('taxaJuros')}
                      />
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group className="mb-3">
                      <Form.Label>Periodicidade</Form.Label>
                      <Form.Select {...register('periodicidadeJuros')}>
                        {PERIODICIDADES.map((p) => (
                          <option key={p.value} value={p.value}>
                            {p.label}
                          </option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  </Col>
                </Row>

                <Form.Check
                  type="checkbox"
                  label="Calcular juros sobre valor corrigido"
                  {...register('jurosSobreCorrigido')}
                />
              </Card.Body>
            </Card>
          )}

          {isFazendaPublica && (
            <Alert variant="info" className="mb-4">
              <FaInfoCircle className="me-2" />
              <strong>Juros automáticos:</strong> Os juros moratórios são calculados automaticamente
              conforme a legislação vigente no período.
            </Alert>
          )}

          <Card className="mb-4">
            <Card.Header>
              <strong>Encargos</strong>
            </Card.Header>
            <Card.Body>
              <Row>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Multa (%)</Form.Label>
                    <Form.Control
                      type="number"
                      step="0.01"
                      {...register('multaPercentual')}
                    />
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Honorários (%)</Form.Label>
                    <Form.Control
                      type="number"
                      step="0.01"
                      {...register('honorariosPercentual')}
                    />
                  </Form.Group>
                </Col>
              </Row>
            </Card.Body>
          </Card>

          {/* Parcelas */}
          <Card className="mb-4">
            <Card.Header className="d-flex justify-content-between align-items-center">
              <strong>Parcelas (opcional)</strong>
              <div className="d-flex gap-2">
                <Button variant="outline-primary" size="sm" onClick={adicionarParcela}>
                  <FaPlus className="me-1" /> Adicionar Parcela
                </Button>
                <Button variant="outline-secondary" size="sm" onClick={() => setShowModalVarias(true)}>
                  <FaLayerGroup className="me-1" /> Adicionar Várias
                </Button>
              </div>
            </Card.Header>
            <Card.Body>
              {fields.length === 0 ? (
                <Alert variant="info" className="mb-0">
                  Nenhuma parcela adicionada. O cálculo será feito sobre o valor
                  principal.
                </Alert>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                <Table size="sm">
                  <thead>
                    <tr>
                      <th>Descrição</th>
                      <th>Valor</th>
                      <th>Vencimento</th>
                      <th>Índice</th>
                      <th width="50"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {fields.map((field, index) => (
                      <tr key={field.id}>
                        <td>
                          <Form.Control
                            size="sm"
                            type="text"
                            placeholder="Descrição"
                            {...register(`parcelas.${index}.descricao`)}
                          />
                        </td>
                        <td>
                          <Form.Control
                            size="sm"
                            type="number"
                            step="0.01"
                            placeholder="Valor"
                            {...register(`parcelas.${index}.valorOriginal`, {
                              required: true,
                            })}
                          />
                        </td>
                        <td>
                          <Form.Control
                            size="sm"
                            type="date"
                            {...register(`parcelas.${index}.dataVencimento`, {
                              required: true,
                            })}
                          />
                        </td>
                        <td>
                          <Form.Select
                            size="sm"
                            {...register(`parcelas.${index}.tabelaIndiceId`)}
                          >
                            <option value="">Padrão do cálculo</option>
                            {indices.map((indice) => (
                              <option key={indice.id} value={indice.id}>
                                {indice.nome}
                              </option>
                            ))}
                          </Form.Select>
                        </td>
                        <td>
                          <Button
                            variant="outline-danger"
                            size="sm"
                            onClick={() => remove(index)}
                          >
                            <FaTrash />
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
                </div>
              )}
            </Card.Body>
          </Card>

          {/* Memorial de Cálculo */}
          <Card className="mb-4">
            <Card.Body>
              <Form.Check
                type="checkbox"
                label="Apresentar Memoriais de Cálculo"
                {...register('mostrarMemorial')}
              />
              <Form.Text className="text-muted">
                Exibe a evolução mensal detalhada com variação percentual do período.
              </Form.Text>
            </Card.Body>
          </Card>

          {/* Botões de ação */}
          <div className="d-flex gap-2 mb-4">
            <Button
              variant="primary"
              size="lg"
              onClick={handleSubmit(handleCalcular)}
              disabled={calculando}
            >
              <FaCalculator className="me-2" />
              {calculando ? 'Calculando...' : 'Calcular'}
            </Button>
          </div>
        </Form>

        {/* Modal Adicionar Várias Parcelas */}
        <Modal show={showModalVarias} onHide={() => setShowModalVarias(false)} centered>
          <Modal.Header closeButton>
            <Modal.Title>Adicionar Várias Parcelas</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Quantidade de parcelas</Form.Label>
              <Form.Control
                type="number"
                min="1"
                max="120"
                value={modalVarias.quantidade}
                onChange={(e) => setModalVarias({ ...modalVarias, quantidade: e.target.value })}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Valor de cada parcela (R$)</Form.Label>
              <Form.Control
                type="number"
                step="0.01"
                placeholder="0,00"
                value={modalVarias.valorParcela}
                onChange={(e) => setModalVarias({ ...modalVarias, valorParcela: e.target.value })}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Descrição base</Form.Label>
              <Form.Control
                type="text"
                placeholder="Ex: Parcela"
                value={modalVarias.descricaoBase}
                onChange={(e) => setModalVarias({ ...modalVarias, descricaoBase: e.target.value })}
              />
              <Form.Text className="text-muted">
                Será numerada automaticamente: &quot;Parcela 1&quot;, &quot;Parcela 2&quot;, etc.
              </Form.Text>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Vencimentos</Form.Label>
              <Form.Check
                type="radio"
                label="Subsequentes à data inicial do cálculo"
                name="tipoVencimento"
                checked={modalVarias.tipoVencimento === 'subsequentes'}
                onChange={() => setModalVarias({ ...modalVarias, tipoVencimento: 'subsequentes' })}
              />
              <Form.Check
                type="radio"
                label="A partir de uma data específica"
                name="tipoVencimento"
                checked={modalVarias.tipoVencimento === 'dataEspecifica'}
                onChange={() => setModalVarias({ ...modalVarias, tipoVencimento: 'dataEspecifica' })}
              />
              {modalVarias.tipoVencimento === 'dataEspecifica' && (
                <Form.Control
                  type="date"
                  className="mt-2"
                  value={modalVarias.dataInicialParcelas}
                  onChange={(e) => setModalVarias({ ...modalVarias, dataInicialParcelas: e.target.value })}
                />
              )}
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Índice de correção</Form.Label>
              <Form.Select
                value={modalVarias.tabelaIndiceId}
                onChange={(e) => setModalVarias({ ...modalVarias, tabelaIndiceId: e.target.value })}
              >
                <option value="">Usar índice do cálculo</option>
                {indices.map((indice) => (
                  <option key={indice.id} value={indice.id}>
                    {indice.nome} - {indice.descricao}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowModalVarias(false)}>
              Cancelar
            </Button>
            <Button variant="primary" onClick={adicionarVariasParcelas}>
              <FaPlus className="me-1" /> Adicionar {modalVarias.quantidade || 0} Parcela(s)
            </Button>
          </Modal.Footer>
        </Modal>
      </div>
    </Layout>
  );
};

export default CalculoFormPage;
