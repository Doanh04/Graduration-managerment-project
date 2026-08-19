import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';
import '../style/Toast.scss';

const ToastContext = createContext(null);
let toastSequence = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const dismiss = useCallback((id) => setToasts((items) => items.filter((item) => item.id !== id)), []);
  const dismissAll = useCallback(() => setToasts([]), []);
  const show = useCallback((message, type = 'info', options = {}) => {
    const id = ++toastSequence;
    const toast = { id, message, type, title: options.title, duration: options.duration ?? 5000 };
    setToasts((items) => [...items.slice(-3), toast]);
    if (toast.duration > 0) window.setTimeout(() => dismiss(id), toast.duration);
    return id;
  }, [dismiss]);
  const api = useMemo(() => ({ show, success: (message, options) => show(message, 'success', options), error: (message, options) => show(message, 'error', options), info: (message, options) => show(message, 'info', options), dismiss, dismissAll }), [show, dismiss, dismissAll]);
  return <ToastContext.Provider value={api}>{children}<div className="toast-viewport" aria-live="polite" aria-atomic="false">{toasts.map((toast) => <Toast key={toast.id} toast={toast} dismiss={dismiss} />)}</div></ToastContext.Provider>;
}

function Toast({ toast, dismiss }) { const Icon = toast.type === 'success' ? CheckCircle2 : toast.type === 'error' ? AlertCircle : Info; return <div className={`app-toast ${toast.type}`} role={toast.type === 'error' ? 'alert' : 'status'}><div className="toast-icon"><Icon size={20} /></div><div className="toast-copy"><strong>{toast.title || (toast.type === 'success' ? 'Thành công' : toast.type === 'error' ? 'Không thể thực hiện' : 'Thông báo')}</strong>{String(toast.message).split('\n').map((line, index) => <span key={`${line}-${index}`}>{line}</span>)}</div><button onClick={() => dismiss(toast.id)} aria-label="Đóng thông báo"><X size={17} /></button><i style={{ animationDuration: `${toast.duration}ms` }} /></div>; }

export function useToast() { const context = useContext(ToastContext); if (!context) throw new Error('useToast phải được dùng bên trong ToastProvider'); return context; }
