import React, { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  Edit3,
  LoaderCircle,
  Plus,
  Search,
  Trash2,
  UserPlus,
  Users,
  X,
} from "lucide-react";
import API_ENDPOINTS from "../../config/endpoints.js";
import ResourceService from "../../services/ResourceService.jsx";
import getApiErrorMessage from "../../utils/apiError.js";
import { useToast } from "../../context/ToastContext.jsx";
import "../../style/DefenseCommittee.scss";
import "../../style/DefenseCommitteeLayoutFix.scss";

const STATUS = {
  DRAFT: "Bản nháp",
  ACTIVE: "Đang hoạt động",
  INACTIVE: "Ngừng hoạt động",
};
const ROLES = {
  CHAIRPERSON: "Chủ tịch",
  SECRETARY: "Thư ký",
  REVIEWER: "Phản biện",
  MEMBER: "Thành viên",
};
const emptyForm = { committeeName: "", description: "", defensePeriodId: "" };
export default function DefenseCommitteePage() {
  const toast = useToast();
  const [periods, setPeriods] = useState([]);
  const [periodId, setPeriodId] = useState("");
  const [items, setItems] = useState([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [members, setMembers] = useState([]);
  const [lecturers, setLecturers] = useState([]);
  const [memberForm, setMemberForm] = useState({
    lectureId: "",
    role: "MEMBER",
    note: "",
  });
  const [removeMember, setRemoveMember] = useState(null);
  const [reason, setReason] = useState("");
  const loadPeriods = async () => {
    try {
      const list = await ResourceService.getAll(
        API_ENDPOINTS.defensePeriods.list,
      );
      setPeriods(list);
      const firstOpen = list.find(
        (p) => String(p.status || "").toUpperCase() !== "FINISHED",
      );
      if (firstOpen) setPeriodId(String(firstOpen.defensePeriodId));
      else if (list.length) setPeriodId(String(list[0].defensePeriodId));
    } catch (e) {
      setError(getApiErrorMessage(e, "Không thể tải đợt bảo vệ."));
    }
  };
  const load = async () => {
    if (!periodId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    try {
      setItems(
        await ResourceService.getAll(
          API_ENDPOINTS.committees.byPeriod(periodId),
        ),
      );
    } catch (e) {
      setError(getApiErrorMessage(e, "Không thể tải danh sách hội đồng."));
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    loadPeriods();
  }, []);
  useEffect(() => {
    load();
  }, [periodId]); // eslint-disable-line react-hooks/exhaustive-deps
  const filtered = useMemo(
    () =>
      items.filter((i) =>
        `${i.committeeName} ${i.description || ""}`
          .toLowerCase()
          .includes(query.toLowerCase()),
      ),
    [items, query],
  );
  const openCreate = () => {
    const selectedPeriod =
      periods.find(
        (p) =>
          String(p.defensePeriodId) === String(periodId) &&
          String(p.status || "").toUpperCase() !== "FINISHED",
      ) ||
      periods.find((p) => String(p.status || "").toUpperCase() !== "FINISHED");
    setForm({
      ...emptyForm,
      defensePeriodId: String(selectedPeriod?.defensePeriodId || ""),
    });
    setError("");
    setModal({ type: "committee" });
  };
  const openEdit = (i) => {
    setForm({
      committeeName: i.committeeName,
      description: i.description || "",
    });
    setError("");
    setModal({ type: "committee", item: i });
  };
  const saveCommittee = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (modal.item) {
        await ResourceService.update(
          API_ENDPOINTS.committees.update(modal.item.committeeId),
          { committeeName: form.committeeName, description: form.description },
        );
      } else {
        const targetPeriodId = String(form.defensePeriodId || "");
        if (!targetPeriodId) throw new Error("Vui lòng chọn đợt bảo vệ.");
        await ResourceService.create(
          API_ENDPOINTS.committees.create(targetPeriodId),
          { committeeName: form.committeeName, description: form.description },
        );
        setPeriodId(targetPeriodId);
      }
      toast.success(modal.item ? "Đã cập nhật hội đồng." : "Đã tạo hội đồng.");
      setModal(null);
      await load();
    } catch (ex) {
      setError(getApiErrorMessage(ex, "Không thể lưu hội đồng."));
    } finally {
      setSaving(false);
    }
  };
  const deleteCommittee = async (i) => {
    setSaving(true);
    try {
      await ResourceService.remove(
        API_ENDPOINTS.committees.remove(i.committeeId),
      );
      toast.success("Đã xóa hội đồng.");
      await load();
    } catch (ex) {
      toast.error(getApiErrorMessage(ex, "Không thể xóa hội đồng."));
    } finally {
      setSaving(false);
    }
  };
  const openMembers = async (i) => {
    setSaving(true);
    setError("");
    try {
      const [memberList, lecturerList] = await Promise.all([
        ResourceService.getAll(
          API_ENDPOINTS.committeeMembers.byCommittee(i.committeeId),
        ),
        ResourceService.getAll(API_ENDPOINTS.lecturers.list),
      ]);
      setMembers(memberList);
      setLecturers(lecturerList);
      setMemberForm({ lectureId: "", role: "MEMBER", note: "" });
      setModal({ type: "members", item: i });
    } catch (ex) {
      toast.error(getApiErrorMessage(ex, "Không thể tải thành viên hội đồng."));
    } finally {
      setSaving(false);
    }
  };
  const reloadMembers = async () =>
    setMembers(
      await ResourceService.getAll(
        API_ENDPOINTS.committeeMembers.byCommittee(modal.item.committeeId),
      ),
    );
  const assign = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await ResourceService.create(
        API_ENDPOINTS.committeeMembers.assign(modal.item.committeeId),
        memberForm,
      );
      toast.success("Đã thêm thành viên hội đồng.");
      setMemberForm({ lectureId: "", role: "MEMBER", note: "" });
      await reloadMembers();
      await load();
    } catch (ex) {
      setError(getApiErrorMessage(ex, "Không thể thêm thành viên."));
    } finally {
      setSaving(false);
    }
  };
  const updateRole = async (m, role) => {
    setSaving(true);
    setError("");
    try {
      await ResourceService.update(
        API_ENDPOINTS.committeeMembers.update(m.memberId),
        { role, note: m.note || null },
        "patch",
      );
      toast.success("Đã cập nhật vai trò.");
      await reloadMembers();
    } catch (ex) {
      setError(getApiErrorMessage(ex, "Không thể cập nhật vai trò."));
    } finally {
      setSaving(false);
    }
  };
  const deactivate = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await ResourceService.update(
        API_ENDPOINTS.committeeMembers.deactivate(removeMember.memberId),
        { reason },
        "patch",
      );
      toast.success("Đã gỡ thành viên khỏi hội đồng.");
      setRemoveMember(null);
      setReason("");
      await reloadMembers();
      await load();
    } catch (ex) {
      setError(getApiErrorMessage(ex, "Không thể gỡ thành viên."));
    } finally {
      setSaving(false);
    }
  };
  const activate = async (i) => {
    setSaving(true);
    try {
      await ResourceService.update(
        API_ENDPOINTS.committees.activate(i.committeeId),
        null,
        "patch",
      );
      toast.success("Đã kích hoạt hội đồng.");
      await load();
    } catch (ex) {
      toast.error(
        getApiErrorMessage(ex, "Hội đồng chưa đủ điều kiện để kích hoạt."),
      );
    } finally {
      setSaving(false);
    }
  };
  return (
    <div className="committee-page">
      <section className="period-selector panel">
        <div>
          <Users />
          <span>
            <strong>Đợt bảo vệ</strong>
            <small>Chọn đợt để quản lý hội đồng</small>
          </span>
        </div>
        <select value={periodId} onChange={(e) => setPeriodId(e.target.value)}>
          <option value="">Chọn đợt bảo vệ</option>
          {periods.map((p) => (
            <option key={p.defensePeriodId} value={p.defensePeriodId}>
              {p.periodName} · {p.academicYear}
            </option>
          ))}
        </select>
      </section>
      <div className="page-heading">
        <div>
          <span>QUẢN LÝ DỮ LIỆU</span>
          <h1>Hội đồng bảo vệ</h1>
          <p>Tạo hội đồng, phân công thành viên và vai trò tương ứng.</p>
        </div>
        <button
          className="primary-button"
          disabled={!periodId}
          onClick={openCreate}
        >
          <Plus /> Tạo hội đồng
        </button>
      </div>
      {error && !modal && <Error text={error} />}
      <section className="committee-panel">
        <div className="committee-toolbar">
          <Search />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo tên hội đồng..."
          />
        </div>
        {loading ? (
          <State />
        ) : (
          <table>
            <thead>
              <tr>
                <th>Mã</th>
                <th>Hội đồng</th>
                <th>Thành viên</th>
                <th>Đợt bảo vệ</th>
                <th>Trạng thái</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {filtered.map((i) => (
                <tr key={i.committeeId}>
                  <td>{i.committeeId}</td>
                  <td>
                    <strong>{i.committeeName}</strong>
                    <small>{i.description || "Chưa có mô tả"}</small>
                  </td>
                  <td>
                    <button
                      className="member-count"
                      onClick={() => openMembers(i)}
                    >
                      <Users /> {i.activeMemberCount} thành viên
                    </button>
                  </td>
                  <td>{i.defensePeriodName}</td>
                  <td>
                    <span
                      className={`committee-status ${i.status.toLowerCase()}`}
                    >
                      {STATUS[i.status]}
                    </span>
                  </td>
                  <td>
                    <div className="committee-actions">
                      <button
                        title="Quản lý thành viên"
                        onClick={() => openMembers(i)}
                      >
                        <UserPlus />
                      </button>
                      {i.status === "DRAFT" && (
                        <>
                          <button title="Sửa" onClick={() => openEdit(i)}>
                            <Edit3 />
                          </button>
                          <button
                            title="Kích hoạt"
                            className="success"
                            onClick={() => activate(i)}
                          >
                            <CheckCircle2 />
                          </button>
                          <button
                            title="Xóa"
                            className="danger"
                            onClick={() => deleteCommittee(i)}
                          >
                            <Trash2 />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && !filtered.length && (
          <div className="committee-empty">
            <Users />
            <strong>Chưa có hội đồng trong đợt bảo vệ này.</strong>
          </div>
        )}
      </section>
      {modal?.type === "committee" && (
        <div className="modal-backdrop">
          <section className="committee-modal">
            <Header
              title={modal.item ? "Cập nhật hội đồng" : "Tạo hội đồng"}
              close={() => setModal(null)}
            />
            <form onSubmit={saveCommittee}>
              {!modal.item && (
                <label>
                  Đợt bảo vệ<b>*</b>
                  <select
                    required
                    value={form.defensePeriodId}
                    onChange={(e) =>
                      setForm({ ...form, defensePeriodId: e.target.value })
                    }
                  >
                    <option value="">Chọn đợt bảo vệ</option>
                    {periods
                      .filter(
                        (p) =>
                          String(p.status || "").toUpperCase() !== "FINISHED",
                      )
                      .map((p) => (
                        <option
                          key={p.defensePeriodId}
                          value={p.defensePeriodId}
                        >
                          {p.periodName} · {p.academicYear}
                        </option>
                      ))}
                  </select>
                </label>
              )}
              <label>
                Tên hội đồng<b>*</b>
                <input
                  required
                  value={form.committeeName}
                  onChange={(e) =>
                    setForm({ ...form, committeeName: e.target.value })
                  }
                />
              </label>
              <label>
                Mô tả
                <textarea
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                />
              </label>
              {error && <Error text={error} />}
              <footer>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setModal(null)}
                >
                  Hủy
                </button>
                <button className="primary-button" disabled={saving}>
                  Lưu dữ liệu
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
      {modal?.type === "members" && (
        <div className="modal-backdrop">
          <section className="member-modal">
            <Header
              title={modal.item.committeeName}
              eyebrow="THÀNH VIÊN HỘI ĐỒNG"
              close={() => setModal(null)}
            />
            <form className="member-add" onSubmit={assign}>
              <label>
                Giảng viên<b>*</b>
                <select
                  required
                  value={memberForm.lectureId}
                  onChange={(e) =>
                    setMemberForm({ ...memberForm, lectureId: e.target.value })
                  }
                >
                  <option value="">Chọn giảng viên</option>
                  {lecturers
                    .filter(
                      (l) =>
                        !members.some(
                          (m) =>
                            m.status === "ACTIVE" &&
                            m.lectureId === (l.lectureId || l.idLecture),
                        ),
                    )
                    .map((l) => (
                      <option
                        key={l.lectureId || l.idLecture}
                        value={l.lectureId || l.idLecture}
                      >
                        {l.fullName} · {l.lectureCode}
                      </option>
                    ))}
                </select>
              </label>
              <label>
                Vai trò<b>*</b>
                <select
                  value={memberForm.role}
                  onChange={(e) =>
                    setMemberForm({ ...memberForm, role: e.target.value })
                  }
                >
                  {Object.entries(ROLES).map(([v, l]) => (
                    <option key={v} value={v}>
                      {l}
                    </option>
                  ))}
                </select>
              </label>
              <label className="wide">
                Ghi chú
                <input
                  value={memberForm.note}
                  onChange={(e) =>
                    setMemberForm({ ...memberForm, note: e.target.value })
                  }
                />
              </label>
              <button className="primary-button" disabled={saving}>
                <UserPlus /> Thêm thành viên
              </button>
            </form>
            {error && <Error text={error} />}
            <div className="member-list">
              {members
                .filter((m) => m.status === "ACTIVE")
                .map((m) => (
                  <article key={m.memberId}>
                    <div>
                      <strong>{m.lectureName}</strong>
                      <small>{m.lectureCode}</small>
                    </div>
                    <select
                      value={m.role}
                      disabled={saving || modal.item.status !== "DRAFT"}
                      onChange={(e) => updateRole(m, e.target.value)}
                    >
                      {Object.entries(ROLES).map(([v, l]) => (
                        <option key={v} value={v}>
                          {l}
                        </option>
                      ))}
                    </select>
                    {modal.item.status === "DRAFT" && (
                      <button
                        className="danger"
                        title="Gỡ thành viên"
                        onClick={() => {
                          setRemoveMember(m);
                          setReason("");
                        }}
                      >
                        <Trash2 />
                      </button>
                    )}
                  </article>
                ))}
            </div>
            <footer>
              <span>
                Cần tối thiểu 3 thành viên, gồm 1 Chủ tịch và 1 Thư ký.
              </span>
              <button
                className="secondary-button"
                onClick={() => setModal(null)}
              >
                Đóng
              </button>
            </footer>
          </section>
        </div>
      )}
      {removeMember && (
        <div className="modal-backdrop nested">
          <section className="remove-member-modal">
            <Header
              title={`Gỡ ${removeMember.lectureName}`}
              close={() => setRemoveMember(null)}
            />
            <form onSubmit={deactivate}>
              <label>
                Lý do gỡ thành viên<b>*</b>
                <textarea
                  required
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              </label>
              <footer>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setRemoveMember(null)}
                >
                  Hủy
                </button>
                <button className="primary-button" disabled={saving}>
                  Xác nhận gỡ
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
function Header({ title, eyebrow = "HỘI ĐỒNG BẢO VỆ", close }) {
  return (
    <header>
      <div>
        <span>{eyebrow}</span>
        <h3>{title}</h3>
      </div>
      <button onClick={close}>
        <X />
      </button>
    </header>
  );
}
function Error({ text }) {
  return (
    <div className="workflow-error">
      <AlertCircle />
      {text}
    </div>
  );
}
function State() {
  return (
    <div className="committee-empty">
      <LoaderCircle className="spin" />
      <strong>Đang tải danh sách hội đồng...</strong>
    </div>
  );
}
