import { Navbar as BsNavbar, Container, Nav } from 'react-bootstrap';
import { Link, NavLink } from 'react-router-dom';
import { FaBalanceScale } from 'react-icons/fa';

const Navbar = () => {
  return (
    <BsNavbar expand="lg" className="app-navbar">
      <Container fluid className="px-4">
        <BsNavbar.Brand as={Link} to="/dashboard" className="app-navbar-brand">
          <span className="app-navbar-logo">
            <FaBalanceScale />
          </span>
          <span className="app-navbar-title">
            Cálculos Jurídicos
            <span className="app-navbar-version">v2.0</span>
          </span>
        </BsNavbar.Brand>

        <BsNavbar.Toggle aria-controls="navbar-nav" className="app-navbar-toggle" />

        <BsNavbar.Collapse id="navbar-nav">
          <Nav className="ms-auto align-items-center gap-1">
            <Nav.Link as={NavLink} to="/dashboard" end className="app-nav-link">
              Dashboard
            </Nav.Link>
            <Nav.Link as={NavLink} to="/calculos" className="app-nav-link">
              Cálculos
            </Nav.Link>
            <Nav.Link as={NavLink} to="/indices" className="app-nav-link">
              Índices
            </Nav.Link>
          </Nav>
        </BsNavbar.Collapse>
      </Container>
    </BsNavbar>
  );
};

export default Navbar;
