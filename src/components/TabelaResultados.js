import React from 'react';
import { Table } from 'react-bootstrap';

export default function TabelaResultados({ resultados }) {
  if (!resultados || resultados.length === 0) return null;

  return (
    <Table striped bordered hover responsive>
      <thead>
        <tr>
          <th>Período</th>
          <th>Valor Original</th>
          <th>Índice Aplicado</th>
          <th>Taxa de Juros (%)</th>
          <th>Valor Final</th>
        </tr>
      </thead>
      <tbody>
        {resultados.map((r, idx) => (
          <tr key={idx}>
            <td>{r.periodo}</td>
            <td>R$ {Number(r.valorOriginal).toFixed(2)}</td>
            <td>{r.indice}</td>
            <td>{r.juros ? `${r.juros}%` : '-'}</td>
            <td>R$ {Number(r.valorFinal).toFixed(2)}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
