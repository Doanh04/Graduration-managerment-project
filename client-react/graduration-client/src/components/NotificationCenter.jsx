import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Bell, CheckCheck, Inbox } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import httpClient from '../config/HttpClient.jsx';
import API_ENDPOINTS from '../config/endpoints.js';

const quietRequest = { skipGlobalLoading: true };

const destinationFor = (notification, section) => {
  const resource = notification?.relatedResourceType;
  if (resource === 'SUBMISSION') return section === 'student' ? '/student/progress' : section === 'lecturer' ? '/lecturer/submissions' : '/admin/submissions';
  if (resource === 'MILESTONE') return section === 'student' ? '/student/progress' : '/admin/progress';
  if (resource === 'DEFENSE_SCHEDULE') return section === 'student' ? '/student/schedule' : section === 'lecturer' ? '/lecturer/schedules' : '/admin/schedules';
  if (resource === 'TOPIC_REGISTRATION') return section === 'student' ? '/student/topics' : '/admin/topic-registrations';
  if (resource === 'TOPIC') return section === 'student' ? '/student/topics' : section === 'lecturer' ? '/lecturer/supervision' : '/admin/topics';
  if (resource === 'FINAL_SUBMISSION' || resource === 'FINAL_SUBMISSION_DEADLINE') return section === 'student' ? '/student/final-submission' : '/admin/final-submissions';
  if (resource === 'GRADUATION_ENROLLMENT' || resource === 'REVIEW_ASSIGNMENT') return section === 'student' ? '/student/results' : '/admin/graduation-enrollments';
  return null;
};

const formatTime = (value) => {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
};

export default function NotificationCenter({ section }) {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const wrapRef = useRef(null);
  const navigate = useNavigate();

  const loadUnreadCount = useCallback(() => {
    httpClient.get(API_ENDPOINTS.notifications.unreadCount, quietRequest)
      .then((response) => setUnreadCount(Number(response?.result ?? 0)))
      .catch(() => {});
  }, []);

  const loadNotifications = useCallback(() => {
    setLoading(true);
    return httpClient.get(`${API_ENDPOINTS.notifications.list}?page=0&size=20`, quietRequest)
      .then((response) => setNotifications(response?.result?.content ?? []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadUnreadCount();
    const timer = window.setInterval(loadUnreadCount, 30000);
    return () => window.clearInterval(timer);
  }, [loadUnreadCount]);

  useEffect(() => {
    if (open) loadNotifications();
  }, [open, loadNotifications]);

  useEffect(() => {
    const closeOutside = (event) => {
      if (wrapRef.current && !wrapRef.current.contains(event.target)) setOpen(false);
    };
    document.addEventListener('mousedown', closeOutside);
    return () => document.removeEventListener('mousedown', closeOutside);
  }, []);

  const openNotification = async (notification) => {
    if (!notification.read) {
      setNotifications((current) => current.map((item) => item.idNotification === notification.idNotification ? { ...item, read: true } : item));
      setUnreadCount((current) => Math.max(0, current - 1));
      try {
        await httpClient.patch(API_ENDPOINTS.notifications.read(notification.idNotification), null, quietRequest);
      } catch {
        loadUnreadCount();
      }
    }
    const destination = destinationFor(notification, section);
    setOpen(false);
    if (destination) navigate(destination);
  };

  const markAllRead = async () => {
    try {
      await httpClient.patch(API_ENDPOINTS.notifications.readAll, null, quietRequest);
      setNotifications((current) => current.map((item) => ({ ...item, read: true })));
      setUnreadCount(0);
    } catch {
      loadUnreadCount();
    }
  };

  return <div className="notification-wrap" ref={wrapRef}>
    <button className="icon-button notification-button" aria-label={`Thông báo${unreadCount ? `, ${unreadCount} chưa đọc` : ''}`} onClick={() => setOpen((value) => !value)} aria-expanded={open}>
      <Bell size={20} />
      {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
    </button>
    {open && <section className="notification-panel">
      <header>
        <div><strong>Thông báo</strong><span>{unreadCount} chưa đọc</span></div>
        {unreadCount > 0 && <button type="button" onClick={markAllRead}><CheckCheck size={15} /> Đọc tất cả</button>}
      </header>
      <div className="notification-list">
        {loading && <div className="notification-empty">Đang tải thông báo...</div>}
        {!loading && notifications.length === 0 && <div className="notification-empty"><Inbox size={27} /><span>Chưa có thông báo.</span></div>}
        {!loading && notifications.map((notification) => <button type="button" className={`notification-item ${notification.read ? '' : 'unread'}`} key={notification.idNotification} onClick={() => openNotification(notification)}>
          <i />
          <span><strong>{notification.title}</strong><p>{notification.content}</p><small>{formatTime(notification.createdAt)}</small></span>
        </button>)}
      </div>
    </section>}
  </div>;
}
