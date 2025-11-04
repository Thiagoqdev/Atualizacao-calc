import React, { useState } from 'react';
import { Form, Button, Row, Col } from 'react-bootstrap';

const indices = [
  { value: 'cjf', label: 'Manual do CJF' },
  { value: 'selic', label: 'SELIC' },
  { value: 'ipca', label: 'IPCA' },
  // Adicione outros índices conforme necessário
];

export default function FormCopiaValor({ onSubmit }) {
  const [form, setForm] = useState({
    valor: '',
    dataInicio: '',
    dataFim: '',
    indice: '',
    periodoCopiaInicio: '',
    periodoCopiaFim: '',
    aplicarManualCJF: false,
  });

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(form);
  };

  return (
    <Form onSubmit={handleSubmit} className="mb-4">
      <h5>Cópia de Valor Periódica</h5>
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
            <Form.Label>Índice Específico</Form.Label>
            <Form.Select
              name="indice"
              value={form.indice}
              onChange={handleChange}
              required={!form.aplicarManualCJF}
              disabled={form.aplicarManualCJF}
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
            <Form.Label>Período de Cópia - Início</Form.Label>
            <Form.Control
              type="date"
              name="periodoCopiaInicio"
              value={form.periodoCopiaInicio}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group>
            <Form.Label>Período de Cópia - Fim</Form.Label>
            <Form.Control
              type="date"
              name="periodoCopiaFim"
              value={form.periodoCopiaFim}
              onChange={handleChange}
              required
            />
          </Form.Group>
        </Col>
        <Col md={3} className="d-flex align-items-end">
          <Form.Group>
            <Form.Check
              type="checkbox"
              label="Aplicar Manual do CJF"
              name="aplicarManualCJF"
              checked={form.aplicarManualCJF}
              onChange={handleChange}
            />
          </Form.Group>
        </Col>
        <Col md={3} className="d-flex align-items-end">
          <Button type="submit" variant="success" className="w-100">
            Copiar Valor
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
