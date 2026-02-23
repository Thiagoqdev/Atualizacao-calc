import { Nav } from 'react-bootstrap';
import { NavLink } from 'react-router-dom';
import {
  FaHome,
  FaGavel,
  FaCalculator,
  FaChartLine,
  FaFileAlt,
  FaFileSignature,
} from 'react-icons/fa';

const Sidebar = () => {
  const menuItems = [
    { path: '/dashboard', icon: FaHome, label: 'Dashboard', end: true },
    { path: '/dashboard/peticoes', icon: FaFileSignature, label: 'Criar Petições' },
    { path: '/processos', icon: FaGavel, label: 'Processos' },
    { path: '/calculos', icon: FaCalculator, label: 'Cálculos' },
    { path: '/indices', icon: FaChartLine, label: 'Índices' },
  ];

  return (
    <div className="sidebar d-flex flex-column p-3">
      <div className="text-center mb-4 pt-3">
        <h5 className="text-white fw-bold mb-0">
          <FaFileAlt className="me-2" />
          CalcJur
        </h5>
        <small className="text-white-50">Sistema de Cálculos</small>
      </div>

      <Nav className="flex-column">
        {menuItems.map((item) => (
          <Nav.Link
            key={item.path}
            as={NavLink}
            to={item.path}
            end={item.end}
            className={({ isActive }) =>
              `nav-link d-flex align-items-center ${isActive ? 'active' : ''}`
            }
          >
            <item.icon className="me-2" />
            {item.label}
          </Nav.Link>
        ))}
      </Nav>

      <div className="mt-auto text-center text-white-50 small py-3">
        <small>v1.0.0</small>
      </div>
    </div>
  );
};

export default Sidebar;
