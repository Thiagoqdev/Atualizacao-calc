import { Navbar as BsNavbar, Container, Nav } from 'react-bootstrap';
import { Link } from 'react-router-dom';

const Navbar = () => {
  return (
    <BsNavbar bg="white" expand="lg" className="shadow-sm py-2">
      <Container fluid>
        <BsNavbar.Brand as={Link} to="/dashboard" className="fw-bold text-primary">
          Calculos Juridicos
        </BsNavbar.Brand>

        <BsNavbar.Toggle aria-controls="navbar-nav" />

        <BsNavbar.Collapse id="navbar-nav">
          <Nav className="ms-auto align-items-center">
            <Nav.Link as={Link} to="/dashboard">Dashboard</Nav.Link>
            <Nav.Link as={Link} to="/processos">Processos</Nav.Link>
            <Nav.Link as={Link} to="/calculos">Calculos</Nav.Link>
            <Nav.Link as={Link} to="/indices">Indices</Nav.Link>
          </Nav>
        </BsNavbar.Collapse>
      </Container>
    </BsNavbar>
  );
};

export default Navbar;
