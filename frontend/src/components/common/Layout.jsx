import { Container, Row, Col } from 'react-bootstrap';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

const Layout = ({ children }) => {
  return (
    <div className="d-flex flex-column min-vh-100">
      <Navbar />
      <Container fluid className="flex-grow-1 p-0">
        <Row className="g-0 h-100">
          <Col md={2} className="d-none d-md-block">
            <Sidebar />
          </Col>
          <Col md={10} className="main-content bg-light">
            {children}
          </Col>
        </Row>
      </Container>
    </div>
  );
};

export default Layout;
