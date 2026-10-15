import React, { useEffect, useState } from 'react';
import { BookOpen, FileText, History, LoaderCircle, Minus, Plus, Save, Settings, Sparkles } from 'lucide-react';
import API_ENDPOINTS from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import '../../style/SystemSettings.scss';

const defaults = {
  studentTopicProposalEnabled: false,
  auditRetentionLimit: 100,
  libraryTopicsVisible: true,
  templatesVisible: true,
};

export default function SystemSettingsPage() {
  const toast = useToast();
  const [settings, setSettings] = useState(defaults);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    ResourceService.getOne(API_ENDPOINTS.systemSettings.current)
      .then((result) => active && setSettings({ ...defaults, ...result }))
      .catch((requestError) => active && setError(getApiErrorMessage(requestError, 'Không thể tải cài đặt hệ thống.')))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, []);

  const setToggle = (name) => setSettings((current) => ({ ...current, [name]: !current[name] }));
  const setRetention = (value) => setSettings((current) => ({
    ...current,
    auditRetentionLimit: Math.max(1, Number.isFinite(Number(value)) ? Math.floor(Number(value)) : 1),
  }));

  const save = async () => {
    setSaving(true); setError('');
    try {
      const response = await ResourceService.update(API_ENDPOINTS.systemSettings.update, {
        studentTopicProposalEnabled: settings.studentTopicProposalEnabled,
        auditRetentionLimit: settings.auditRetentionLimit,
        libraryTopicsVisible: settings.libraryTopicsVisible,
        templatesVisible: settings.templatesVisible,
      });
      setSettings({ ...defaults, ...(response?.result ?? response) });
      toast.success('Đã lưu cài đặt hệ thống.');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể lưu cài đặt hệ thống.'));
    } finally { setSaving(false); }
  };

  if (loading) return <div className="system-settings-loading"><LoaderCircle size={24}/><span>Đang tải cài đặt...</span></div>;

  return <div className="system-settings-page">
    <div className="page-heading"><div><span>CẤU HÌNH HỆ THỐNG</span><h1>Cài đặt chung</h1><p>Quản lý các tính năng dùng chung và chính sách lưu trữ của hệ thống.</p></div><button className="settings-save" disabled={saving} onClick={save}>{saving ? <LoaderCircle className="spin" size={17}/> : <Save size={17}/>} {saving ? 'Đang lưu...' : 'Lưu cài đặt'}</button></div>
    {error && <div className="settings-error">{error}</div>}
    <div className="settings-grid">
      <SettingCard icon={Sparkles} tone="blue" title="Đề xuất đề tài của sinh viên" description="Cho phép sinh viên tự tạo và gửi đề xuất đề tài mới để xét duyệt.">
        <Toggle checked={settings.studentTopicProposalEnabled} onChange={() => setToggle('studentTopicProposalEnabled')} label={settings.studentTopicProposalEnabled ? 'Đang bật' : 'Đang tắt'}/>
      </SettingCard>
      <SettingCard icon={History} tone="orange" title="Lưu trữ nhật ký hoạt động" description="Hệ thống tự động xóa các bản ghi cũ nhất khi vượt quá giới hạn này.">
        <div className="retention-control"><button onClick={() => setRetention(settings.auditRetentionLimit - 1)} disabled={settings.auditRetentionLimit <= 1}><Minus size={16}/></button><input type="number" min="1" value={settings.auditRetentionLimit} onChange={(event) => setRetention(event.target.value)}/><button onClick={() => setRetention(settings.auditRetentionLimit + 1)}><Plus size={16}/></button><span>bản ghi</span></div>
      </SettingCard>
      <SettingCard icon={BookOpen} tone="green" title="Thư viện đề tài" description="Ẩn hoặc hiện các đề tài tham khảo đối với sinh viên và giảng viên.">
        <Toggle checked={settings.libraryTopicsVisible} onChange={() => setToggle('libraryTopicsVisible')} label={settings.libraryTopicsVisible ? 'Đang hiển thị' : 'Đang ẩn'}/>
      </SettingCard>
      <SettingCard icon={FileText} tone="purple" title="Thư viện biểu mẫu" description="Ẩn hoặc hiện các biểu mẫu đối với sinh viên và giảng viên.">
        <Toggle checked={settings.templatesVisible} onChange={() => setToggle('templatesVisible')} label={settings.templatesVisible ? 'Đang hiển thị' : 'Đang ẩn'}/>
      </SettingCard>
    </div>
    <div className="settings-note"><Settings size={18}/><div><strong>Dữ liệu luôn được giữ nguyên</strong><span>Tắt thư viện chỉ ẩn nội dung khỏi sinh viên và giảng viên. Quản trị viên vẫn xem và quản lý đầy đủ các bản ghi.</span></div></div>
  </div>;
}

function SettingCard({ icon: Icon, tone, title, description, children }) {
  return <section className="setting-card"><div className={`setting-icon ${tone}`}><Icon size={21}/></div><div className="setting-copy"><h2>{title}</h2><p>{description}</p></div><div className="setting-control">{children}</div></section>;
}

function Toggle({ checked, onChange, label }) {
  return <button type="button" className={`setting-toggle ${checked ? 'on' : ''}`} aria-pressed={checked} onClick={onChange}><span className="toggle-track"><i/></span><strong>{label}</strong></button>;
}
