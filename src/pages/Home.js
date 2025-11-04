import React, { useState } from 'react';
import FormAtualizacaoValor from '../components/FormAtualizacaoValor';
import FormCopiaValor from '../components/FormCopiaValor';
import TabelaResultados from '../components/TabelaResultados';
import FeedbackMensagem from '../components/FeedbackMensagem';

export default function Home() {
  const [resultados, setResultados] = useState([]);
  const [mensagem, setMensagem] = useState({ tipo: '', texto: '' });

  // Simulação de submissão (substitua por chamada à API no futuro)
  const handleAtualizacao = (dados) => {
    // Validação extra pode ser feita aqui
    setResultados([
      ...resultados,
      {
        periodo: `${dados.dataInicio} a ${dados.dataFim}`,
        valorOriginal: dados.valor,
        indice: dados.indice,
        juros: dados.juros,
        valorFinal: dados.valor, // Substitua pelo valor calculado
      },
    ]);
    setMensagem({ tipo: 'success', texto: 'Valor atualizado com sucesso!' });
  };

  const handleCopia = (dados) => {
    setResultados([
      ...resultados,
      {
        periodo: `${dados.periodoCopiaInicio} a ${dados.periodoCopiaFim}`,
        valorOriginal: dados.valor,
        indice: dados.aplicarManualCJF ? 'Manual do CJF' : dados.indice,
        juros: '', // Adapte conforme necessário
        valorFinal: dados.valor, // Substitua pelo valor calculado
      },
    ]);
    setMensagem({ tipo: 'success', texto: 'Valor copiado com sucesso!' });
  };

  return (
    <div className="container mt-4">
      <FeedbackMensagem
        tipo={mensagem.tipo}
        mensagem={mensagem.texto}
        onClose={() => setMensagem({ tipo: '', texto: '' })}
      />
      <FormAtualizacaoValor onSubmit={handleAtualizacao} />
      <FormCopiaValor onSubmit={handleCopia} />
      <TabelaResultados resultados={resultados} />
    </div>
  );
}
