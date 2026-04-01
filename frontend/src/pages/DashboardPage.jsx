import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import {
  FaPlus,
  FaArrowRight,
  FaBalanceScale,
  FaLandmark,
  FaChartLine,
  FaSync,
  FaCalendarAlt,
  FaFileInvoiceDollar,
} from 'react-icons/fa';
import Layout from '../components/common/Layout';
import LoadingSpinner from '../components/common/LoadingSpinner';
import calculoApi from '../api/calculoApi';
import indiceApi from '../api/indiceApi';

const DashboardPage = () => {
  const [loading, setLoading] = useState(true);
  const [totalCalculos, setTotalCalculos] = useState(0);
  const [calculosRecentes, setCalculosRecentes] = useState([]);
  const [indicesStatus, setIndicesStatus] = useState([]);

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [calculosRes, tabelasRes] = await Promise.all([
        calculoApi.listar({ size: 6, sort: 'dataCriacao,desc' }),
        indiceApi.listarTabelas(),
      ]);
      setCalculosRecentes(calculosRes.content || []);
      setTotalCalculos(calculosRes.totalElements || 0);
      setIndicesStatus(tabelasRes || []);
    } catch (error) {
      console.error('Erro ao carregar dados:', error);
      toast.error('Erro ao carregar dados do dashboard');
    } finally {
      setLoading(false);
    }
  };

  const formatarMoeda = (valor) => {
    if (!valor) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
  };

  const formatarData = (data) => {
    if (!data) return '-';
    return new Date(data).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  const getStatusConfig = (status) => {
    const map = {
      RASCUNHO: { label: 'Rascunho', cls: 'dash-status-draft' },
      CALCULADO: { label: 'Calculado', cls: 'dash-status-calc' },
      FINALIZADO: { label: 'Finalizado', cls: 'dash-status-done' },
    };
    return map[status] || { label: status || '-', cls: 'dash-status-draft' };
  };

  const getTipoLabel = (tipo) => {
    if (tipo === 'FAZENDA_PUBLICA') return 'Fazenda Pública';
    return 'Padrão';
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
      <div className="dash">
        {/* ── Header ── */}
        <header className="dash-header">
          <div className="dash-header-left">
            <span className="dash-kicker">Painel de cálculos</span>
            <h1 className="dash-heading">
              Seus cálculos<br />jurídicos
            </h1>
          </div>
          <div className="dash-header-right">
            <div className="dash-metric">
              <span className="dash-metric-value">{totalCalculos}</span>
              <span className="dash-metric-label">cálculos realizados</span>
            </div>
            <div className="dash-metric">
              <span className="dash-metric-value">{indicesStatus.length}</span>
              <span className="dash-metric-label">índices disponíveis</span>
            </div>
          </div>
        </header>

        {/* ── Novo Cálculo - Entry Points ── */}
        <section className="dash-entry">
          <div className="dash-entry-label">
            <FaPlus />
            <span>Iniciar novo cálculo</span>
          </div>
          <div className="dash-entry-grid">
            <Link to="/calculos/novo?tipo=PADRAO" className="dash-entry-card dash-entry-padrao">
              <div className="dash-entry-icon">
                <FaBalanceScale />
              </div>
              <div className="dash-entry-body">
                <strong>Cálculo Padrão</strong>
                <p>Escolha o índice de correção, taxa de juros e encargos manualmente.</p>
              </div>
              <FaArrowRight className="dash-entry-arrow" />
            </Link>
            <Link to="/calculos/novo?tipo=FAZENDA_PUBLICA" className="dash-entry-card dash-entry-fazenda">
              <div className="dash-entry-icon">
                <FaLandmark />
              </div>
              <div className="dash-entry-body">
                <strong>Fazenda Pública</strong>
                <p>Índices e juros aplicados automaticamente conforme a legislação federal vigente.</p>
              </div>
              <FaArrowRight className="dash-entry-arrow" />
            </Link>
          </div>
        </section>

        {/* ── Cálculos Recentes ── */}
        <section className="dash-recentes">
          <div className="dash-section-head">
            <h2 className="dash-section-title">Cálculos recentes</h2>
            <Link to="/calculos" className="dash-section-link">
              Ver todos <FaArrowRight />
            </Link>
          </div>

          {calculosRecentes.length === 0 ? (
            <div className="dash-empty">
              <FaFileInvoiceDollar className="dash-empty-icon" />
              <p>Nenhum cálculo realizado ainda.</p>
              <Link to="/calculos/novo" className="dash-empty-btn">
                <FaPlus /> Criar primeiro cálculo
              </Link>
            </div>
          ) : (
            <div className="dash-calc-grid">
              {calculosRecentes.map((c) => {
                const st = getStatusConfig(c.status);
                return (
                  <Link key={c.id} to={`/calculos/${c.id}`} className="dash-calc-card">
                    <div className="dash-calc-top">
                      <span className={`dash-status ${st.cls}`}>{st.label}</span>
                      <span className="dash-calc-tipo">{getTipoLabel(c.tipoCalculo)}</span>
                    </div>
                    <h3 className="dash-calc-title">{c.titulo || `Cálculo #${c.id}`}</h3>
                    <div className="dash-calc-valor">{formatarMoeda(c.valorPrincipal)}</div>
                    <div className="dash-calc-meta">
                      <span><FaCalendarAlt /> {formatarData(c.dataCriacao)}</span>
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </section>

        {/* ── Indices Status ── */}
        <section className="dash-indices">
          <div className="dash-section-head">
            <h2 className="dash-section-title">Índices monetários</h2>
            <Link to="/indices" className="dash-section-link">
              Gerenciar <FaArrowRight />
            </Link>
          </div>
          <div className="dash-indices-strip">
            {indicesStatus.map((t) => (
              <div key={t.id} className="dash-indice-chip">
                <FaChartLine className="dash-indice-chip-icon" />
                <span className="dash-indice-chip-name">{t.nome.replace('_', '-')}</span>
                <span className="dash-indice-chip-src">{t.fonteApi}</span>
              </div>
            ))}
            <Link to="/indices" className="dash-indice-sync-btn" title="Sincronizar índices">
              <FaSync /> Sincronizar
            </Link>
          </div>
        </section>
      </div>
    </Layout>
  );
};

export default DashboardPage;
