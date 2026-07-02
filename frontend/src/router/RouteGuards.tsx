import { observer } from 'mobx-react-lite';
import { Navigate, useLocation } from 'react-router-dom';
import { authStore } from '../stores';

interface GuardProps {
  children: JSX.Element;
  adminOnly?: boolean;
}

export const ProtectedRoute = observer(({ children, adminOnly = false }: GuardProps) => {
  const location = useLocation();

  if (!authStore.isLoggedIn) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (adminOnly && authStore.role !== 'ADMIN') {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
});

export const LoginRoute = observer(({ children }: GuardProps) => {
  if (authStore.isLoggedIn) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
});
