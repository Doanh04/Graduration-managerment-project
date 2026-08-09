import React from 'react';
import LoginLayout from '../layout/LoginLayout.jsx';

export const Routers = [
  {
    path: '/login',
    element: <LoginLayout />,
  },
  {
    path: '/',
    element: <LoginLayout />,
  },
  {
    path: '*',
    element: <LoginLayout />,
  },
];

export default Routers;
