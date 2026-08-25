import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Check, ChevronDown, LoaderCircle, Save, Search, UserCog, X } from 'lucide-react';
import httpClient from '../../config/HttpClient.jsx';
import API_ENDPOINTS from '../../config/endpoints.js';
import { useToast } from '../../context/ToastContext.jsx';
import getApiErrorMessage from '../../utils/apiError.js';
import '../../style/RolePermissionPage.scss';

const roleLabels = {
  ADMIN: 'Quản trị viên', FACULTY: 'Ban quản lý khoa', SUPERVISOR: 'Giảng viên hướng dẫn',
  REVIEWER: 'Giảng viên phản biện', STUDENT: 'Sinh viên',
};
const roleContent = (response) => response?.result?.content || response?.result || [];

export default function RolePermissionPage() {
  const toast = useToast();
  const dropdownRef = useRef(null);
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(null);
  const [selected, setSelected] = useState([]);
  const [roleQuery, setRoleQuery] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [userResponse, roleResponse] = await Promise.all([
          httpClient.get(API_ENDPOINTS.userRoles.list),
          httpClient.get(API_ENDPOINTS.roles.list, { params: { size: 100 } }),
        ]);
        setUsers(userResponse?.result || []);
        setRoles(roleContent(roleResponse));
      } catch (error) {
        toast.error(getApiErrorMessage(error, 'Không thể tải danh sách người dùng và vai trò.'));
      } finally { setLoading(false); }
    };
    load();
  }, []);
  useEffect(() => {
    const close = (event) => { if (!dropdownRef.current?.contains(event.target)) setDropdownOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const roleById = useMemo(() => new Map(roles.map((role) => [role.role, role])), [roles]);
  const filteredUsers = users.filter((user) => `${user.userName} ${user.fullName} ${user.userCode || ''} ${(user.roles || []).join(' ')}`.toLowerCase().includes(query.toLowerCase()));
  const filteredRoles = roles.filter((role) => `${role.role} ${roleLabels[role.role] || role.roleName} ${role.description || ''}`.toLowerCase().includes(roleQuery.toLowerCase()));
  const openEditor = (user) => { setEditing(user); setSelected(Array.from(user.roles || [])); setRoleQuery(''); setDropdownOpen(false); };
  const toggleRole = (roleId) => setSelected((current) => current.includes(roleId) ? current.filter((id) => id !== roleId) : [...current, roleId]);
  const saveRoles = async () => {
    setSaving(true);
    try {
      const response = await httpClient.put(API_ENDPOINTS.userRoles.update(editing.userId), { roles: selected });
      const updated = response?.result;
      setUsers((current) => current.map((user) => user.userId === editing.userId ? updated : user));
      setEditing(null);
      toast.success('Cập nhật vai trò người dùng thành công.');
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Không thể cập nhật vai trò người dùng.'));
    } finally { setSaving(false); }
  };

  return <div className="page-stack role-permission-page">
    <section className="page-title-row"><div><span className="page-kicker">QUẢN LÝ NGƯỜI DÙNG</span><h2>Phân quyền người dùng</h2><p>Gán hoặc hủy nhiều vai trò cho từng tài khoản trong hệ thống.</p></div></section>
    <section className="panel data-panel"><div className="table-toolbar"><div className="table-search"><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên, tài khoản hoặc mã người dùng..." />{query && <button className="search-clear" onClick={() => setQuery('')}><X size={15} /></button>}</div></div>
      {loading ? <div className="permission-state"><LoaderCircle className="spin" />Đang tải danh sách người dùng...</div> : <div className="table-scroll"><table><thead><tr><th>Tài khoản</th><th>Thông tin người dùng</th><th>Danh sách vai trò</th><th>Số vai trò</th><th /></tr></thead><tbody>{filteredUsers.map((user) => <tr key={user.userId}><td><strong>{user.userName}</strong></td><td><div className="role-name"><UserCog size={18} /><span><strong>{user.fullName || user.userName}</strong><small>{user.userCode || (user.userType === 'SYSTEM' ? 'Tài khoản hệ thống' : user.userType)}</small></span></div></td><td><div className="permission-tags">{(user.roles || []).length ? user.roles.map((id) => <span key={id}>{roleLabels[id] || roleById.get(id)?.roleName || id}</span>) : <em>Chưa có vai trò</em>}</div></td><td>{user.roles?.length || 0}</td><td className="table-actions"><button className="permission-edit-button" onClick={() => openEditor(user)}>Cập nhật vai trò</button></td></tr>)}</tbody></table></div>}
    </section>
    {editing && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !saving && setEditing(null)}><section className="topic-modal permission-modal" role="dialog" aria-modal="true"><header><div><span>PHÂN QUYỀN NGƯỜI DÙNG</span><h3>{editing.fullName || editing.userName}</h3></div><button disabled={saving} onClick={() => setEditing(null)}><X size={20} /></button></header><div className="permission-modal-body"><label>Danh sách vai trò</label><div className="permission-multiselect" ref={dropdownRef}><button type="button" className="permission-select-trigger" onClick={() => setDropdownOpen((open) => !open)}><span>{selected.length ? `Đã chọn ${selected.length} vai trò` : 'Chọn vai trò cần gán'}</span><ChevronDown size={18} /></button>{dropdownOpen && <div className="permission-dropdown"><div className="permission-dropdown-search"><Search size={16} /><input autoFocus value={roleQuery} onChange={(event) => setRoleQuery(event.target.value)} placeholder="Tìm theo tên vai trò..." /></div><div className="permission-options">{filteredRoles.map((role) => { const checked = selected.includes(role.role); return <button type="button" className={checked ? 'selected' : ''} key={role.role} onClick={() => toggleRole(role.role)}><span><strong>{roleLabels[role.role] || role.roleName}</strong><small>{role.description || role.role}</small></span><span className="permission-check">{checked && <Check size={15} />}</span></button>; })}</div></div>}</div><div className="selected-permissions">{selected.map((id) => <span key={id}>{roleLabels[id] || roleById.get(id)?.roleName || id}<button type="button" title="Hủy vai trò" onClick={() => toggleRole(id)}><X size={14} /></button></span>)}</div>{selected.length === 0 && <p className="permission-empty-note">Lưu danh sách trống sẽ hủy toàn bộ vai trò của tài khoản này.</p>}</div><footer><button className="secondary-button" disabled={saving} onClick={() => setEditing(null)}>Hủy thao tác</button><button className="primary-button" disabled={saving} onClick={saveRoles}>{saving ? <LoaderCircle className="spin" size={17} /> : <Save size={17} />}{saving ? 'Đang lưu...' : 'Lưu danh sách vai trò'}</button></footer></section></div>}
  </div>;
}
