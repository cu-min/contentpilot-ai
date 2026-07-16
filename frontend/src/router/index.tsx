import { createBrowserRouter, Navigate } from 'react-router-dom';
import BasicLayout from '../layouts/BasicLayout';
import Login from '../pages/Login';
import Dashboard from '../pages/Dashboard';
import Article from '../pages/Article';
import ArticleEditor from '../pages/ArticleEditor';
import ArticleAiGenerate from '../pages/Article/AiGenerate';
import Product from '../pages/Product';
import Platform from '../pages/Platform';
import Publish from '../pages/Publish';
import User from '../pages/User';
import { LoginRoute, ProtectedRoute } from './RouteGuards';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <LoginRoute>
        <Login />
      </LoginRoute>
    ),
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <BasicLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'articles', element: <Article /> },
      { path: 'articles/new', element: <ArticleEditor /> },
      { path: 'articles/:id', element: <ArticleEditor /> },
      { path: 'ai-generate', element: <ArticleAiGenerate /> },
      { path: 'product', element: <Product /> },
      { path: 'platform', element: <Platform /> },
      { path: 'publish', element: <Publish /> },
      {
        path: 'user',
        element: (
          <ProtectedRoute adminOnly>
            <User />
          </ProtectedRoute>
        ),
      },
    ],
  },
]);
