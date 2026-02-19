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
      parcelas: [],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'parcelas',
  });

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
      toast.success('Cálculo realizado com sucesso!');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao calcular');
    } finally {
      setCalculando(false);
    }
  };

  const handleSalvar = async (data) => {
    if (!processoId) {
      toast.warning('Selecione um processo para salvar o cálculo');
      return;
    }

    setSalvando(true);
    try {
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

  const prepararPayload = (data) => {
    const payload = {
      titulo: data.titulo || `Cálculo ${new Date().toLocaleDateString('pt-BR')}`,
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

    if (data.tabelaIndiceId) {
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

  return (
    <Layout>
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
        <Row>
          {/* Formulário */}
          <Col lg={6}>
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

            {/* Botões de ação */}
            <div className="d-flex gap-2 mb-4">
              <Button
                variant="primary"
                onClick={handleSubmit(handleCalcular)}
                disabled={calculando}
              >
                <FaCalculator className="me-2" />
                {calculando ? 'Calculando...' : 'Calcular'}
              </Button>
              {processoId && resultado && (
                <Button
                  variant="success"
                  onClick={handleSubmit(handleSalvar)}
                  disabled={salvando}
                >
                  <FaSave className="me-2" />
                  {salvando ? 'Salvando...' : 'Salvar Cálculo'}
                </Button>
              )}
            </div>
          </Col>

          {/* Resultado */}
          <Col lg={6}>
            {resultado && (
              <Card className="sticky-top" style={{ top: '20px' }}>
                <Card.Header className="bg-success text-white">
                  <strong>Resultado do Cálculo</strong>
                </Card.Header>
                <Card.Body>
                  <div className="resultado-calculo mb-4">
                    <p className="mb-1 opacity-75">Valor Total Atualizado</p>
                    <p className="valor-total mb-0">
                      {formatarMoeda(resultado.valorTotal)}
                    </p>
                  </div>

                  <Row className="mb-4">
                    <Col xs={6}>
                      <div className="resultado-item">
                        <small className="text-muted">Valor Original</small>
                        <p className="mb-0 fw-bold">
                          {formatarMoeda(resultado.valorOriginal)}
                        </p>
                      </div>
                    </Col>
                    <Col xs={6}>
                      <div className="resultado-item">
                        <small className="text-muted">Valor Corrigido</small>
                        <p className="mb-0 fw-bold">
                          {formatarMoeda(resultado.valorCorrigido)}
                        </p>
                      </div>
                    </Col>
                  </Row>

                  <Row className="mb-4">
                    <Col xs={6}>
                      <div className="resultado-item">
                        <small className="text-muted">Juros</small>
                        <p className="mb-0 fw-bold">
                          {formatarMoeda(resultado.valorJuros)}
                        </p>
                      </div>
                    </Col>
                    <Col xs={6}>
                      <div className="resultado-item">
                        <small className="text-muted">Multa</small>
                        <p className="mb-0 fw-bold">
                          {formatarMoeda(resultado.valorMulta)}
                        </p>
                      </div>
                    </Col>
                  </Row>

                  <Row>
                    <Col xs={6}>
                      <div className="resultado-item">
                        <small className="text-muted">Honorários</small>
                        <p className="mb-0 fw-bold">
                          {formatarMoeda(resultado.valorHonorarios)}
                        </p>
                      </div>
                    </Col>
                    <Col xs={6}>
                      <div className="resultado-item">
                        <small className="text-muted">Fator de Correção</small>
                        <p className="mb-0 fw-bold">
                          {resultado.fatorCorrecao?.toFixed(6) || '-'}
                        </p>
                      </div>
                    </Col>
                  </Row>

                  {/* Detalhamento por parcela */}
                  {resultado.parcelas && resultado.parcelas.length > 0 && (
                    <>
                      <hr />
                      <h6>Detalhamento por Parcela</h6>
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
                              <td><small>{parcela.indiceNome || 'Padrão'}</small></td>
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
                    </>
                  )}
                </Card.Body>
              </Card>
            )}
          </Col>
        </Row>
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
    </Layout>
  );
};

export default CalculoFormPage;
