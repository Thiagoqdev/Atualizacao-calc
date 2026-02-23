import { Container, Row, Col } from 'react-bootstrap';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

const Layout = ({ children, variant = 'default' }) => {
  const isDashboard = variant === 'dashboard';

  return (
    <div className={`app-layout d-flex flex-column min-vh-100 ${isDashboard ? 'layout-dashboard' : ''}`}>
      <Navbar />
      <Container fluid className="app-layout-body flex-grow-1 p-0">
        <Row className="g-0 h-100 app-layout-row">
          <Col md={2} className="d-none d-md-flex app-layout-sidebar-col">
            <Sidebar />
          </Col>
          <Col md={10} className={`main-content ${isDashboard ? 'dashboard-main-content' : 'bg-light'}`}>
            {children}
          </Col>
        </Row>
      </Container>
    </div>
  );
};

export default Layout;
