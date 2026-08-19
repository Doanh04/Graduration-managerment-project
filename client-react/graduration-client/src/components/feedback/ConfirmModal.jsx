import React from 'react';
import { AlertTriangle, Edit3, KeyRound, Trash2, X } from 'lucide-react';

export default function ConfirmModal({ open, type = 'warning', title, message, confirmLabel = 'Xác nhận', loading = false, onConfirm, onCancel }) {
  if (!open) return null;
  const Icon = type === 'delete' ? Trash2 : type === 'reset' ? KeyRound : type === 'edit' ? Edit3 : AlertTriangle;
  return <div className="modal-backdrop confirm-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !loading && onCancel()}><section className={`confirm-modal ${type}`} role="alertdialog" aria-modal="true" aria-labelledby="confirm-title"><button className="confirm-close" disabled={loading} onClick={onCancel}><X size={19} /></button><div className="confirm-icon"><Icon size={25} /></div><span>XÁC NHẬN THAO TÁC</span><h3 id="confirm-title">{title}</h3><p>{message}</p><footer><button className="secondary-button" disabled={loading} onClick={onCancel}>Hủy</button><button className="confirm-submit" disabled={loading} onClick={onConfirm}>{loading ? 'Đang xử lý...' : confirmLabel}</button></footer></section></div>;
}
