import React, { useState } from 'react';
import { Form, Button, Row, Col } from 'react-bootstrap';

const indices = [
  { value: 'cjf', label: 'Manual do CJF' },
  { value: 'selic', label: 'SELIC' },
  { value: 'ipca', label: 'IPCA' },
  // Adicione outros índices conforme necessário
];

export default function FormAtualizacaoValor({ onSubmit }) {
  const [form, setForm] = useState({
    valor: '',
    dataInicio: '',
    dataFim: '',
    indice: '',
    juros: '',
  });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(form);
  };

  return (
    <Form onSubmit={handleSubmit} className="mb-4">
      <h5>Atualização de Valor Único</h5>
      <Row>
        <Col md={3}>
          <Form.Group>
            <Form.Label>Valor Inicial</Form.Label>
            <Form.Control
              type="number"
              name="valor"
              value={form.valor}
              onChange={handleChange}
              required
              min="0"
              step="0.01"
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group>
            <Form.Label>Data de Início</Form.Label>
            <Form.Control
              type="date"
              name="dataInicio"
              value={form.dataInicio}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group>
            <Form.Label>Data de Fim</Form.Label>
            <Form.Control
              type="date"
              name="dataFim"
              value={form.dataFim}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group>
            <Form.Label>Índice Inicial</Form.Label>
            <Form.Select
              name="indice"
              value={form.indice}
              onChange={handleChange}
              required
            >
              <option value="">Selecione</option>
              {indices.map((i) => (
                <option key={i.value} value={i.value}>{i.label}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>
      <Row className="mt-2">
        <Col md={3}>
          <Form.Group>
            <Form.Label>Taxa de Juros (%)</Form.Label>
            <Form.Control
              type="number"
              name="juros"
              value={form.juros}
              onChange={handleChange}
              min="0"
              step="0.01"
            />
          </Form.Group>
        </Col>
        <Col md={3} className="d-flex align-items-end">
          <Button type="submit" variant="primary" className="w-100">
            Atualizar Valor
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
