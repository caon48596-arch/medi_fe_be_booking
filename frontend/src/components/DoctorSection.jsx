import React, { useState, useEffect } from 'react';
import { doctorApi, slotApi, appointmentApi } from '../services/api';

const HOSPITAL_NAME = 'Bệnh Viện Đa Khoa MediSession';

const INTERNAL_DEPARTMENTS = [
  'Khoa Tim Mạch',
  'Khoa Nhi & Tiêm Chủng',
  'Khoa Nội Tiết & Tiểu Đường',
  'Khoa Thận & Tiết Niệu',
  'Khoa Ngoại Tổng Quát',
  'Khoa Thần Kinh',
  'Khoa Tai Mũi Họng',
  'Khoa Mắt',
];

export default function DoctorSection({
  currentUser,
  currentRole,
  onLoginSuccess,
}) {
  const isDoctorLoggedIn = Boolean(currentUser && currentRole === 'ROLE_DOCTOR');

  // Sub Tab: 'slots' | 'appointments'
  const [subTab, setSubTab] = useState('slots');

  // Modal Auth State (đồng nhất 100% với UserSection)
  const [modalAuth, setModalAuth] = useState(false);
  const [authMode, setAuthMode] = useState('login'); // 'login' | 'register' | 'otp'

  // Auth Form State
  const [authForm, setAuthForm] = useState({
    username: '',
    fullName: '',
    email: '',
    password: '',
    confirePassword: '',
    specialty: 'Khoa Tim Mạch',
    room_number: 'Phòng 201 - Tầng 2',
    price: 250000,
    otp: '',
  });

  // Doctor ID input for API calls
  const [doctorId, setDoctorId] = useState('');
  const [doctorInfo, setDoctorInfo] = useState(null);

  // Slot Generator Form
  const [slotForm, setSlotForm] = useState({
    doctorId: '',
    workdate: new Date().toISOString().split('T')[0],
    startShift: '08:00',
    endShift: '12:00',
    durationMinutes: 30,
  });

  // Real Database Data lists
  const [slots, setSlots] = useState([]);
  const [doctorAppointments, setDoctorAppointments] = useState([]);

  // Status & Toast (đồng nhất với UserSection)
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (message, icon = 'check_circle') => {
    setToast({ message, icon });
    setTimeout(() => setToast(null), 4000);
  };

  useEffect(() => {
    resolveDoctor();
  }, [currentUser, currentRole]);

  useEffect(() => {
    if (doctorId) {
      loadSlots();
      loadAppointments();
    }
  }, [doctorId]);

  // Resolve Doctor ID from backend by currentUser username
  const resolveDoctor = async () => {
    try {
      const res = await doctorApi.getAll();
      if (res.data && res.data.length > 0) {
        let matched = null;
        if (currentUser && currentRole === 'ROLE_DOCTOR') {
          matched = res.data.find(
            (d) => d.username?.toLowerCase() === currentUser.toLowerCase()
          );
        }
        if (matched) {
          setDoctorInfo(matched);
          const idStr = String(matched.doctor_id);
          setDoctorId(idStr);
          setSlotForm((prev) => ({ ...prev, doctorId: idStr }));
        } else if (!isDoctorLoggedIn) {
          setDoctorInfo(null);
          setDoctorId('');
        }
      } else {
        setDoctorInfo(null);
        setDoctorId('');
      }
    } catch (e) {
      console.warn('Lỗi phân giải thông tin bác sĩ:', e);
    }
  };

  // ==================== AUTH HANDLERS (ĐỒNG NHẤT 100% VỚI USERSECTION) ====================

  // 1. ĐĂNG KÝ BÁC SĨ NỘI BỘ
  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await doctorApi.register({
        username: authForm.username,
        email: authForm.email,
        password: authForm.password,
        confirmPassword: authForm.confirePassword,
        confirePassword: authForm.confirePassword,
        specialty: authForm.specialty,
        room_number: authForm.room_number,
        price: Number(authForm.price) || 250000,
      });

      showToast(res.message || 'Đăng ký tài khoản bác sĩ thành công! Vui lòng đăng nhập.', 'check_circle');
      setAuthMode('login');
      setAuthForm((prev) => ({ ...prev, password: '', confirePassword: '', otp: '' }));
    } catch (err) {
      showToast(err.message || 'Lỗi đăng ký bác sĩ', 'error');
    } finally {
      setLoading(false);
    }
  };

  // 2. ĐĂNG NHẬP BƯỚC 1: Nhập username + password -> nhận mã OTP xác nhận
  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await doctorApi.login({
        username: authForm.username,
        password: authForm.password,
      });

      // Nếu backend yêu cầu xác thực OTP (luồng chuẩn 2FA)
      if (res.data?.requireOtp) {
        if (res.data?.otp) {
          setAuthForm((prev) => ({ ...prev, otp: String(res.data.otp) }));
        }
        showToast(
          res.message || 'Mã OTP xác nhận đăng nhập Bác sĩ đã được gửi tới email! Vui lòng nhập OTP.',
          'mark_email_read'
        );
        setAuthMode('otp');
        return;
      }

      // Đăng nhập thành công ngay nếu không yêu cầu OTP
      showToast('Đăng nhập Bác sĩ thành công!', 'verified');
      if (res.data) {
        onLoginSuccess(res.data.username, 'ROLE_DOCTOR');
      }
      setModalAuth(false);
      resolveDoctor();
    } catch (err) {
      showToast(err.message || 'Lỗi đăng nhập', 'error');
    } finally {
      setLoading(false);
    }
  };

  // 3. ĐĂNG NHẬP BƯỚC 2: Nhập OTP để hoàn tất ĐĂNG NHẬP
  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await doctorApi.login({
        username: authForm.username,
        otp: Number(authForm.otp),
      });

      showToast(res.message || 'Xác nhận OTP thành công! Bác sĩ đăng nhập thành công.', 'check_circle');
      if (res.data) {
        onLoginSuccess(res.data.username, 'ROLE_DOCTOR');
      }
      setModalAuth(false);
      resolveDoctor();
    } catch (err) {
      showToast(err.message || 'Mã OTP không chính xác hoặc đã hết hạn', 'error');
    } finally {
      setLoading(false);
    }
  };

  // ==================== SLOTS HANDLERS ====================
  const handleCreateSlots = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const formatTime = (time) => (time.length === 5 ? `${time}:00` : time);

      await slotApi.create({
        doctorId: Number(slotForm.doctorId || doctorId),
        workdate: slotForm.workdate,
        startShift: formatTime(slotForm.startShift),
        endShift: formatTime(slotForm.endShift),
        durationMinutes: Number(slotForm.durationMinutes),
      });

      showToast('Tạo danh sách ca khám thành công và đã lưu vào Database!', 'check_circle');
      loadSlots();
    } catch (err) {
      showToast(err.message || 'Lỗi tạo slot khám', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadSlots = async () => {
    setLoading(true);
    try {
      const res = await slotApi.read(doctorId ? Number(doctorId) : null);
      if (res.data) {
        setSlots(res.data);
      }
    } catch (err) {
      console.warn('Load slots error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteSlot = async (slotId) => {
    if (!confirm('Bạn có chắc muốn xóa slot khám #' + slotId + '?')) return;
    setLoading(true);
    try {
      await slotApi.delete(slotId);
      showToast('Đã xóa slot khám #' + slotId + ' khỏi Database', 'delete');
      loadSlots();
    } catch (err) {
      showToast(err.message || 'Lỗi xóa slot', 'error');
    } finally {
      setLoading(false);
    }
  };

  // ==================== APPOINTMENTS HANDLERS ====================
  const loadAppointments = async () => {
    setLoading(true);
    try {
      const res = await doctorApi.getAppointments(doctorId ? Number(doctorId) : 1);
      if (res.data) {
        setDoctorAppointments(res.data);
      }
    } catch (err) {
      console.warn('Doctor appointments error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteAppointment = async (appointmentId) => {
    setLoading(true);
    try {
      await appointmentApi.update({
        appoinmentId: appointmentId,
        status: 'COMPLETED',
      });
      showToast('Đã hoàn tất ca khám cho bệnh nhân #' + appointmentId, 'check_circle');
      loadAppointments();
    } catch (err) {
      showToast(err.message || 'Lỗi cập nhật lịch hẹn', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async (appointmentId) => {
    if (!confirm('Từ chối và chuyển giao lịch hẹn #' + appointmentId + ' sang bác sĩ khác?')) return;
    setLoading(true);
    try {
      await doctorApi.rejectAppointment(Number(doctorId), appointmentId);
      showToast('Đã từ chối và tự động chuyển giao lịch hẹn #' + appointmentId, 'swap_horiz');
      loadAppointments();
    } catch (err) {
      showToast(err.message || 'Lỗi chuyển giao lịch hẹn', 'error');
    } finally {
      setLoading(false);
    }
  };

  const docName = isDoctorLoggedIn ? (doctorInfo?.username || currentUser) : 'Chưa đăng nhập Bác sĩ';
  const docSpecialty = doctorInfo?.specialty || (isDoctorLoggedIn ? 'Chuyên khoa' : '--');
  const docRoom = doctorInfo?.room_number || (isDoctorLoggedIn ? 'Phòng trực ban' : '--');

  return (
    <div className="page-container">
      {/* Toast Notice (Đồng nhất với UserSection) */}
      {toast && (
        <div className="toast-notice">
          <span className="material-symbols-outlined" style={{ color: 'var(--primary-fixed)' }}>
            {toast.icon}
          </span>
          <span>{toast.message}</span>
        </div>
      )}

      {/* ==================== DOCTOR IDENTITY BANNER ==================== */}
      <div className="patient-banner-card">
        <div className="patient-banner-glow"></div>

        <div className="patient-info-left">
          <div className="patient-avatar-box" style={{ color: 'var(--secondary)' }}>
            <span className="material-symbols-outlined" style={{ fontSize: '36px' }}>
              stethoscope
            </span>
            <div className="avatar-verified-badge" style={{ background: isDoctorLoggedIn ? 'var(--secondary)' : 'var(--outline)' }}>
              {isDoctorLoggedIn ? '✓' : '!'}
            </div>
          </div>

          <div className="patient-meta-text">
            <div className="patient-header-row">
              <h1 className="patient-title">Cổng Điều Hành Bác Sĩ Nội Bộ • {HOSPITAL_NAME}</h1>
              <span className={`pill-badge ${isDoctorLoggedIn ? 'pill-secondary' : 'pill-primary'}`}>
                {isDoctorLoggedIn ? 'BÁC SĨ CHÍNH CHỦ' : 'CHƯA ĐĂNG NHẬP BÁC SĨ'}
              </span>
            </div>

            <div className="patient-details-line">
              <span>
                Bác sĩ trực:{' '}
                <strong style={{ color: 'var(--on-surface)' }}>
                  {isDoctorLoggedIn ? `BS. ${docName}` : 'Chưa đăng nhập'}
                </strong>
              </span>
              <span className="meta-dot">•</span>
              <span>Chuyên khoa: <strong>{docSpecialty}</strong></span>
              <span className="meta-dot">•</span>
              <span>
                Phòng khám: <span className="mono-tag">{docRoom}</span>
              </span>
              <span className="meta-dot">•</span>
              <span>
                Mã Bác sĩ: <strong style={{ color: 'var(--primary)' }}>ID #{doctorId || '--'}</strong>
              </span>
            </div>
          </div>
        </div>

        {/* Banner Right Actions (Đồng nhất với UserSection) */}
        <div className="patient-actions-right">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              if (doctorId) {
                loadSlots();
                loadAppointments();
                showToast('Đã làm mới dữ liệu ca khám và danh sách lịch hẹn!', 'refresh');
              } else {
                setModalAuth(true);
              }
            }}
          >
            <span className="material-symbols-outlined" style={{ color: 'var(--secondary)' }}>
              refresh
            </span>
            <span>Làm mới dữ liệu</span>
          </button>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              if (isDoctorLoggedIn) {
                showToast(`Tài khoản Bác sĩ ${currentUser} đã đăng nhập chính chủ.`);
              } else {
                setModalAuth(true);
              }
            }}
          >
            <span className="material-symbols-outlined">
              {isDoctorLoggedIn ? 'verified' : 'account_circle'}
            </span>
            <span>{isDoctorLoggedIn ? 'Bác sĩ chính chủ' : 'Đăng nhập / Đăng ký Bác sĩ'}</span>
          </button>
        </div>
      </div>

      {/* Thông báo nếu chưa đăng nhập Bác sĩ */}
      {!isDoctorLoggedIn && (
        <div
          className="card-section"
          style={{
            background: 'linear-gradient(135deg, rgba(0, 99, 152, 0.08) 0%, rgba(0, 104, 95, 0.05) 100%)',
            border: '1px solid rgba(0, 99, 152, 0.2)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '18px 24px',
            marginBottom: '4px',
            borderRadius: '16px',
            flexWrap: 'wrap',
            gap: '14px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div
              style={{
                width: '44px',
                height: '44px',
                borderRadius: '12px',
                background: 'rgba(0, 99, 152, 0.15)',
                color: 'var(--secondary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '26px' }}>
                medical_information
              </span>
            </div>
            <div>
              <h3 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--on-surface)' }}>
                Yêu cầu tài khoản Bác sĩ nội bộ {HOSPITAL_NAME}
              </h3>
              <p style={{ fontSize: '13px', color: 'var(--on-surface-variant)', marginTop: '2px' }}>
                Đăng nhập hoặc đăng ký tài khoản Bác sĩ để thiết lập khung giờ khám (slots) và tiếp nhận bệnh nhân.
              </p>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-primary"
            onClick={() => setModalAuth(true)}
            style={{ height: '42px', padding: '0 20px' }}
          >
            <span className="material-symbols-outlined">how_to_reg</span>
            <span>Đăng nhập / Đăng ký Bác sĩ</span>
          </button>
        </div>
      )}

      {/* ================= TAB SWITCHER CHO BÁC SĨ ================= */}
      <div
        className="card-section"
        style={{
          padding: '14px 20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div>
          <h2 className="section-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="material-symbols-outlined" style={{ color: 'var(--secondary)' }}>
              clinical_notes
            </span>
            <span>Không Gian Làm Việc Bác Sĩ Nội Bộ</span>
          </h2>
          <p className="section-desc">{HOSPITAL_NAME} - Tự động đồng bộ và điều phối hàng đợi</p>
        </div>

        <div className="tab-switcher">
          <button
            type="button"
            className={`tab-switcher-btn ${subTab === 'slots' ? 'active' : ''}`}
            onClick={() => {
              setSubTab('slots');
              loadSlots();
            }}
          >
            Quản lý Ca & Slot ({slots.length})
          </button>
          <button
            type="button"
            className={`tab-switcher-btn ${subTab === 'appointments' ? 'active' : ''}`}
            onClick={() => {
              setSubTab('appointments');
              loadAppointments();
            }}
          >
            Bệnh nhân đặt khám ({doctorAppointments.length})
          </button>
        </div>
      </div>

      {/* ================= TAB 1: QUẢN LÝ SLOT ================= */}
      {subTab === 'slots' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Form Tạo Slots */}
          <div className="card-section">
            <div className="card-section-header">
              <div className="section-title-wrap">
                <div className="section-icon-badge" style={{ background: 'rgba(0, 99, 152, 0.1)', color: 'var(--secondary)' }}>
                  <span className="material-symbols-outlined">add_alarm</span>
                </div>
                <div>
                  <h2 className="section-title">Thiết Lập Khung Giờ Khám Nội Bộ (/doctor-slots/create)</h2>
                  <p className="section-desc">{HOSPITAL_NAME} - Tự động chia slot theo thời lượng khám và lưu vào Database</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleCreateSlots}>
              <div className="form-grid-3">
                <div className="form-group">
                  <label className="form-label">Mã Bác sĩ (Doctor ID):</label>
                  <input
                    type="number"
                    className="form-input"
                    value={slotForm.doctorId}
                    onChange={(e) => {
                      setSlotForm({ ...slotForm, doctorId: e.target.value });
                      setDoctorId(e.target.value);
                    }}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Ngày làm việc:</label>
                  <input
                    type="date"
                    className="form-input"
                    value={slotForm.workdate}
                    onChange={(e) => setSlotForm({ ...slotForm, workdate: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Thời lượng mỗi slot (phút):</label>
                  <input
                    type="number"
                    min="10"
                    step="5"
                    className="form-input"
                    value={slotForm.durationMinutes}
                    onChange={(e) => setSlotForm({ ...slotForm, durationMinutes: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="form-grid-3" style={{ alignItems: 'flex-end' }}>
                <div className="form-group">
                  <label className="form-label">Giờ bắt đầu ca:</label>
                  <input
                    type="time"
                    className="form-input"
                    value={slotForm.startShift}
                    onChange={(e) => setSlotForm({ ...slotForm, startShift: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Giờ kết thúc ca:</label>
                  <input
                    type="time"
                    className="form-input"
                    value={slotForm.endShift}
                    onChange={(e) => setSlotForm({ ...slotForm, endShift: e.target.value })}
                    required
                  />
                </div>

                <button type="submit" className="btn btn-primary" style={{ height: '48px' }} disabled={loading}>
                  <span className="material-symbols-outlined">schedule_send</span>
                  <span>{loading ? 'Đang tạo...' : 'Tạo các Slot khám (Lưu DB)'}</span>
                </button>
              </div>
            </form>
          </div>

          {/* Danh sách Slots */}
          <div className="card-section">
            <div className="card-section-header">
              <div className="section-title-wrap">
                <div className="section-icon-badge">
                  <span className="material-symbols-outlined">view_agenda</span>
                </div>
                <div>
                  <h3 className="section-title">Danh Sách Ca Khám Thực Tế Trong Database ({slots.length})</h3>
                  <p className="section-desc">Theo dõi các slot trống và slot đã được người bệnh đặt tại {HOSPITAL_NAME}</p>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <input
                  type="number"
                  className="form-input"
                  style={{ width: '120px', height: '36px' }}
                  placeholder="Doctor ID"
                  value={doctorId}
                  onChange={(e) => setDoctorId(e.target.value)}
                />
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={loadSlots}
                  disabled={loading}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                    refresh
                  </span>
                  <span>Tải lại</span>
                </button>
              </div>
            </div>

            {slots.length === 0 ? (
              <p style={{ color: 'var(--outline)', textAlign: 'center', padding: '30px 0' }}>
                Chưa có slot khám nào trong Database cho Bác sĩ ID #{doctorId || '--'}. Hãy thiết lập khung giờ ở trên.
              </p>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '12px' }}>
                {slots.map((s) => {
                  const id = s.DoctorSlot_Id || s.doctorSlot_Id || s.id;
                  return (
                    <div
                      key={id}
                      style={{
                        background: 'var(--surface)',
                        borderRadius: '12px',
                        padding: '14px 16px',
                        border: '1px solid var(--surface-container-high)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                      }}
                    >
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <strong style={{ fontSize: '14px' }}>#{id}</strong>
                          <span className={`pill-badge ${s.book ? 'pill-tertiary' : 'pill-primary'}`}>
                            {s.book ? 'Đã được đặt' : 'Còn trống'}
                          </span>
                        </div>
                        <p style={{ fontSize: '13px', fontWeight: 600, color: 'var(--primary)', marginTop: '4px' }}>
                          {s.startTime ? s.startTime.substring(0, 5) : '08:00'} - {s.endTime ? s.endTime.substring(0, 5) : '08:30'}
                        </p>
                        <p style={{ fontSize: '11px', color: 'var(--outline)' }}>
                          Ngày: {s.workDate || s.date}
                        </p>
                      </div>

                      <button
                        type="button"
                        className="file-icon-btn delete"
                        title="Xóa slot khám khỏi Database"
                        onClick={() => handleDeleteSlot(id)}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                          delete
                        </span>
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ================= TAB 2: LỊCH HẸN BỆNH NHÂN ================= */}
      {subTab === 'appointments' && (
        <div className="card-section">
          <div className="card-section-header">
            <div className="section-title-wrap">
              <div className="section-icon-badge">
                <span className="material-symbols-outlined">event_available</span>
              </div>
              <div>
                <h3 className="section-title">Danh Sách Bệnh Nhân Đặt Hẹn Trong Database ({doctorAppointments.length})</h3>
                <p className="section-desc">Bác sĩ tiếp nhận khám, hoàn tất hoặc chuyển giao trong nội bộ {HOSPITAL_NAME}</p>
              </div>
            </div>

            <button
              type="button"
              className="btn btn-sm btn-outline"
              onClick={loadAppointments}
              disabled={loading}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                refresh
              </span>
              <span>Làm mới</span>
            </button>
          </div>

          {doctorAppointments.length === 0 ? (
            <p style={{ color: 'var(--outline)', textAlign: 'center', padding: '30px 0' }}>
              Hiện chưa có lịch hẹn nào của bệnh nhân cho Bác sĩ ID #{doctorId || '--'}.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {doctorAppointments.map((app) => (
                <div
                  key={app.appoinments_id}
                  style={{
                    background: 'var(--surface)',
                    borderRadius: '14px',
                    padding: '16px 20px',
                    border: '1px solid var(--surface-container-high)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '16px',
                    flexWrap: 'wrap',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    <div
                      style={{
                        width: '44px',
                        height: '44px',
                        borderRadius: '50%',
                        background: 'rgba(0, 104, 95, 0.1)',
                        color: 'var(--primary)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 700,
                      }}
                    >
                      <span className="material-symbols-outlined">person</span>
                    </div>

                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <strong style={{ fontSize: '15px' }}>
                          {app.patientName || app.user?.username || 'Bệnh nhân'}
                        </strong>
                        <span className="mono-tag">#{app.appoinments_id}</span>
                        <span
                          className={`pill-badge ${
                            app.status === 'CONFIRMED'
                              ? 'pill-primary'
                              : app.status === 'COMPLETED'
                              ? 'pill-secondary'
                              : app.status?.includes('REJECT')
                              ? 'pill-error'
                              : 'pill-tertiary'
                          }`}
                        >
                          {app.status}
                        </span>
                      </div>

                      <p style={{ fontSize: '13px', color: 'var(--on-surface-variant)', marginTop: '4px' }}>
                        <strong>Lý do khám:</strong> {app.reason}
                      </p>
                      <p style={{ fontSize: '12px', color: 'var(--outline)', marginTop: '2px' }}>
                        Giờ hẹn: {app.startTime ? app.startTime.substring(0, 5) : '---'} • Ngày {app.date} • Liên hệ: {app.email || app.phone || 'Chính chủ'}
                      </p>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '8px' }}>
                    {app.status !== 'COMPLETED' && (
                      <button
                        type="button"
                        className="btn btn-sm btn-primary"
                        onClick={() => handleCompleteAppointment(app.appoinments_id)}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                          check_circle
                        </span>
                        <span>Hoàn tất khám</span>
                      </button>
                    )}

                    <button
                      type="button"
                      className="btn btn-sm btn-danger-light"
                      onClick={() => handleReject(app.appoinments_id)}
                      title="Từ chối và tự động chuyển giao lịch hẹn"
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                        swap_horiz
                      </span>
                      <span>Chuyển giao</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ==================== MODAL: AUTHENTICATION BÁC SĨ (LOGIN VỚI OTP / ĐĂNG KÝ) ==================== */}
      {modalAuth && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <div className="modal-title">
                <span className="material-symbols-outlined" style={{ color: 'var(--secondary)' }}>
                  stethoscope
                </span>
                <span>Cổng Bác Sĩ • {HOSPITAL_NAME}</span>
              </div>
              <button
                type="button"
                className="icon-btn"
                onClick={() => {
                  setModalAuth(false);
                  setAuthMode('login');
                }}
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            {/* Switch tabs (Đồng nhất 100% với UserSection) */}
            {authMode !== 'otp' ? (
              <div className="tab-switcher" style={{ width: '100%', marginBottom: '16px' }}>
                <button
                  type="button"
                  style={{ flex: 1 }}
                  className={`tab-switcher-btn ${authMode === 'login' ? 'active' : ''}`}
                  onClick={() => setAuthMode('login')}
                >
                  Đăng nhập Bác sĩ
                </button>
                <button
                  type="button"
                  style={{ flex: 1 }}
                  className={`tab-switcher-btn ${authMode === 'register' ? 'active' : ''}`}
                  onClick={() => setAuthMode('register')}
                >
                  Đăng ký Bác sĩ nội bộ
                </button>
              </div>
            ) : (
              <div
                style={{
                  background: 'var(--surface-container-low)',
                  padding: '10px 14px',
                  borderRadius: '10px',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span className="material-symbols-outlined" style={{ color: 'var(--secondary)' }}>security</span>
                  <span style={{ fontSize: '13px', fontWeight: 600 }}>Bước 2: Nhập OTP xác nhận đăng nhập Bác sĩ</span>
                </div>
                <button
                  type="button"
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--primary)',
                    cursor: 'pointer',
                    fontSize: '12px',
                    fontWeight: 700,
                  }}
                  onClick={() => setAuthMode('login')}
                >
                  Đổi tài khoản
                </button>
              </div>
            )}

            {/* BƯỚC 1: FORM ĐĂNG NHẬP BÁC SĨ (Username + Password) */}
            {authMode === 'login' && (
              <form onSubmit={handleLoginSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div className="form-group">
                  <label className="form-label">Tên đăng nhập Bác sĩ (Username):</label>
                  <input
                    type="text"
                    className="form-input"
                    value={authForm.username}
                    onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                    placeholder="Ví dụ: bacsi_an"
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Mật khẩu:</label>
                  <input
                    type="password"
                    className="form-input"
                    value={authForm.password}
                    onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                    placeholder="••••••••"
                    required
                  />
                </div>

                <div
                  style={{
                    padding: '8px 12px',
                    background: 'var(--surface-container-low)',
                    borderRadius: '8px',
                    fontSize: '12px',
                    color: 'var(--outline)',
                  }}
                >
                  <span
                    className="material-symbols-outlined"
                    style={{ fontSize: '15px', verticalAlign: 'middle', marginRight: '4px', color: 'var(--secondary)' }}
                  >
                    info
                  </span>
                  Sau khi nhập tài khoản và mật khẩu, hệ thống sẽ gửi mã OTP xác nhận tới email của Bác sĩ để hoàn tất đăng nhập.
                </div>

                <button type="submit" className="btn btn-primary" style={{ height: '46px', marginTop: '6px' }} disabled={loading}>
                  <span className="material-symbols-outlined">send_to_mobile</span>
                  <span>{loading ? 'Đang gửi...' : 'Đăng nhập & Gửi mã OTP xác nhận'}</span>
                </button>
              </form>
            )}

            {/* FORM ĐĂNG KÝ BÁC SĨ NỘI BỘ (Không đòi hỏi OTP khi đăng ký) */}
            {authMode === 'register' && (
              <form onSubmit={handleRegisterSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div className="form-group">
                  <label className="form-label">Tên tài khoản Bác sĩ (Username) *:</label>
                  <input
                    type="text"
                    className="form-input"
                    value={authForm.username}
                    onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                    placeholder="bacsi_nguyenvanan"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Chuyên khoa khám nội bộ *:</label>
                  <div className="select-wrapper">
                    <select
                      className="form-select"
                      value={authForm.specialty}
                      onChange={(e) => setAuthForm({ ...authForm, specialty: e.target.value })}
                      required
                    >
                      {INTERNAL_DEPARTMENTS.map((dept, idx) => (
                        <option key={idx} value={dept}>
                          {dept}
                        </option>
                      ))}
                    </select>
                    <span className="material-symbols-outlined select-arrow">arrow_drop_down</span>
                  </div>
                </div>

                <div className="form-grid-2" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div className="form-group">
                    <label className="form-label">Phòng khám trực:</label>
                    <input
                      type="text"
                      className="form-input"
                      value={authForm.room_number}
                      onChange={(e) => setAuthForm({ ...authForm, room_number: e.target.value })}
                      placeholder="Phòng 201 - Tầng 2"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Phí khám (VND):</label>
                    <input
                      type="number"
                      className="form-input"
                      value={authForm.price}
                      onChange={(e) => setAuthForm({ ...authForm, price: e.target.value })}
                      placeholder="250000"
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Email cơ quan nhận OTP *:</label>
                  <input
                    type="email"
                    className="form-input"
                    value={authForm.email}
                    onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                    placeholder="doctor.an@hospital.vn"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Mật khẩu *:</label>
                  <input
                    type="password"
                    className="form-input"
                    value={authForm.password}
                    onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                    placeholder="••••••••"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Xác nhận mật khẩu *:</label>
                  <input
                    type="password"
                    className="form-input"
                    value={authForm.confirePassword}
                    onChange={(e) => setAuthForm({ ...authForm, confirePassword: e.target.value })}
                    placeholder="••••••••"
                    required
                  />
                </div>

                <button type="submit" className="btn btn-primary" style={{ height: '46px', marginTop: '6px' }} disabled={loading}>
                  <span className="material-symbols-outlined">how_to_reg</span>
                  <span>{loading ? 'Đang tạo...' : 'Hoàn tất đăng ký Bác sĩ nội bộ'}</span>
                </button>
              </form>
            )}

            {/* BƯỚC 2: FORM XÁC NHẬN MÃ OTP ĐĂNG NHẬP BÁC SĨ */}
            {authMode === 'otp' && (
              <form onSubmit={handleOtpSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ textAlign: 'center', padding: '10px 0' }}>
                  <div
                    style={{
                      width: '56px',
                      height: '56px',
                      borderRadius: '50%',
                      background: 'rgba(0, 99, 152, 0.1)',
                      color: 'var(--secondary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      margin: '0 auto 10px',
                    }}
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: '32px' }}>
                      lock_person
                    </span>
                  </div>
                  <h3 style={{ fontSize: '16px', fontWeight: 700 }}>Xác Nhận OTP Đăng Nhập Bác Sĩ</h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--on-surface-variant)', marginTop: '4px' }}>
                    Bác sĩ: <strong>{authForm.username}</strong>. Hãy nhập mã OTP 6 chữ số vừa nhận để hoàn tất đăng nhập.
                  </p>
                </div>

                <div className="form-group">
                  <label className="form-label" style={{ textAlign: 'center' }}>Mã OTP xác nhận (6 chữ số):</label>
                  <input
                    type="number"
                    className="form-input"
                    style={{
                      textAlign: 'center',
                      fontSize: '22px',
                      letterSpacing: '6px',
                      fontWeight: 700,
                      height: '52px',
                    }}
                    value={authForm.otp}
                    onChange={(e) => setAuthForm({ ...authForm, otp: e.target.value })}
                    placeholder="123456"
                    required
                    autoFocus
                  />
                </div>

                <button type="submit" className="btn btn-primary" style={{ height: '48px', marginTop: '4px' }} disabled={loading}>
                  <span className="material-symbols-outlined">check_circle</span>
                  <span>{loading ? 'Đang xác thực...' : 'Xác nhận OTP & Đăng nhập Bác sĩ'}</span>
                </button>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '6px' }}>
                  <button
                    type="button"
                    style={{
                      background: 'transparent',
                      border: 'none',
                      color: 'var(--outline)',
                      cursor: 'pointer',
                      fontSize: '12px',
                    }}
                    onClick={() => setAuthMode('login')}
                  >
                    ← Quay lại đăng nhập
                  </button>
                  <button
                    type="button"
                    style={{
                      background: 'transparent',
                      border: 'none',
                      color: 'var(--primary)',
                      cursor: 'pointer',
                      fontSize: '12px',
                      fontWeight: 600,
                    }}
                    onClick={handleLoginSubmit}
                  >
                    Gửi lại mã OTP
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
