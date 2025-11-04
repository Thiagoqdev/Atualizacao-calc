import React from 'react';
import { Navbar, Container } from 'react-bootstrap';

export default function AppNavbar() {
  return (
    <Navbar bg="primary" variant="dark" expand="lg">
      <Container>
        <Navbar.Brand href="#">Sistema de Atualização Financeira</Navbar.Brand>
      </Container>
    </Navbar>
  );
}
