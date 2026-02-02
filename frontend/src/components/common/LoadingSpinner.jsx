import { Spinner } from 'react-bootstrap';

const LoadingSpinner = ({ text = 'Carregando...' }) => {
  return (
    <div className="loading-spinner">
      <div className="text-center">
        <Spinner animation="border" variant="primary" />
        <p className="mt-2 text-muted">{text}</p>
      </div>
    </div>
  );
};

export default LoadingSpinner;
