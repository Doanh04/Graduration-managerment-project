import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import { ToastProvider } from './context/ToastContext.jsx';
import AppRouter from './routes/AppRouter';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <AppRouter />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
