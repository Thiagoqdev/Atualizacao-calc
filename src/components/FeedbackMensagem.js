import React from 'react';
import { Alert } from 'react-bootstrap';

export default function FeedbackMensagem({ tipo, mensagem, onClose }) {
  if (!mensagem) return null;
  return (
    <Alert variant={tipo} onClose={onClose} dismissible>
      {mensagem}
    </Alert>
  );
}
