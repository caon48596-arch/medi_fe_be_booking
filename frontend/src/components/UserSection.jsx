import React, { useState, useEffect } from 'react';
import { userApi, appointmentApi, doctorApi, slotApi } from '../services/api';

const HOSPITAL_NAME = 'Bệnh Viện Đa Khoa MediSession';

const INTERNAL_DEPARTMENTS = [
  'Tất cả chuyên khoa nội bộ',
  'Khoa Tim Mạch',
  'Khoa Nhi & Tiêm Chủng',
  'Khoa Nội Tiết & Tiểu Đường',
  'Khoa Thận & Tiết Niệu',
  'Khoa Ngoại Tổng Quát',
  'Khoa Thần Kinh',
  'Khoa Tai Mũi Họng',
  'Khoa Mắt',
];

export default function UserSection({
  currentUser,
  currentRole,
  onLoginSuccess,
}) {
  // Appointment sub-tab: 'upcoming' | 'history' | 'cancelled'
  const [appointmentTab, setAppointmentTab] = useState('upcoming');

  // Real Doctors List from DB
  const [doctors, setDoctors] = useState([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState('');
  const [selectedDepartment, setSelectedDepartment] = useState('Tất cả chuyên khoa nội bộ');

  // Dates for Chip selector (Dynamic next 5 days)
  const getAvailableDates = () => {
    const days = [];
    const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    const today = new Date();
    for (let i = 0; i < 5; i++) {
      const d = new Date(today);
      d.setDate(today.getDate() + i);
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      days.push({
        label: i === 0 ? 'Hôm nay' : dayNames[d.getDay()],
        date: `${dd}/${mm}`,
        fullDate: `${yyyy}-${mm}-${dd}`,
      });
    }
    return days;
  };

  const [availableDates] = useState(getAvailableDates());
  const [selectedDateIndex, setSelectedDateIndex] = useState(0);

  // Time Slots from DB
  const [dbSlots, setDbSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState('');
  const [selectedSlotId, setSelectedSlotId] = useState(null);

  const [symptomReason, setSymptomReason] = useState('');

  // Appointments from DB
  const [upcomingList, setUpcomingList] = useState([]);
  const [historyList, setHistoryList] = useState([]);
  const [cancelledList, setCancelledList] = useState([]);

  // User Profile from DB
  const [userProfile, setUserProfile] = useState(null);

  // Attached files (Only real user-uploaded files, default empty)
  const [attachedFiles, setAttachedFiles] = useState([]);

  // Modals state
  const [modalQr, setModalQr] = useState(null);
  const [modalReschedule, setModalReschedule] = useState(null);
  const [rescheduleDate, setRescheduleDate] = useState('');
  const [rescheduleTime, setRescheduleTime] = useState('');
  const [rescheduleReason, setRescheduleReason] = useState('');
  const [modalCancel, setModalCancel] = useState(null);
  const [modalEditSymptom, setModalEditSymptom] = useState(null);
  const [modalAuth, setModalAuth] = useState(false);
  const [authMode, setAuthMode] = useState('login'); // 'login' | 'register' | 'otp'

  // Auth form
  const [authForm, setAuthForm] = useState({
    username: '',
    fullName: '',
    phone: '',
    email: '',
    password: '',
    confirePassword: '',
    otp: '',
  });

  // Toast
  const [toast, setToast] = useState(null);
  const showToast = (message, icon = 'check_circle') => {
    setToast({ message, icon });
    setTimeout(() => setToast(null), 4000);
  };

  // Initial load: Doctors, Profile, Appointments, Slots
  useEffect(() => {
    loadDoctors();
    loadUserProfile();
    loadBackendAppointments();
  }, [currentUser]);

  useEffect(() => {
    if (selectedDoctorId) {
      loadDoctorSlots(selectedDoctorId);
    }
  }, [selectedDoctorId, selectedDateIndex]);

  // 1. Load real doctors from DB
  const loadDoctors = async () => {
    try {
      const res = await doctorApi.getAll();
      if (res.data && res.data.length > 0) {
        setDoctors(res.data);
        if (!selectedDoctorId) {
          setSelectedDoctorId(String(res.data[0].doctor_id));
        }
      }
    } catch (e) {
      console.warn('Lỗi tải danh sách bác sĩ:', e);
    }
  };

  // 2. Load user profile from DB (Chỉ tải khi người dùng đã đăng nhập)
  const loadUserProfile = async () => {
    if (!currentUser) {
      setUserProfile(null);
      return;
    }
    try {
      const res = await userApi.getProfile(currentUser);
      if (res.data) {
        setUserProfile(res.data);
      }
    } catch (e) {
      console.warn('Lỗi tải thông tin bệnh nhân:', e);
    }
  };

  // 3. Load slots from DB for doctor & selected date
  const loadDoctorSlots = async (docId) => {
    if (!docId) {
      setDbSlots([]);
      return;
    }
    try {
      const res = await slotApi.read(Number(docId));
      if (res.data && res.data.length > 0) {
        setDbSlots(res.data);
      } else {
        setDbSlots([]);
      }
    } catch (e) {
      console.warn('Lỗi tải slots khám:', e);
    }
  };

  // 4. Load real appointments from DB (Chỉ tải lịch hẹn thật của người dùng hiện tại)
  const loadBackendAppointments = async () => {
    if (!currentUser) {
      setUpcomingList([]);
      setCancelledList([]);
      setHistoryList([]);
      return;
    }
    try {
      const res = await userApi.getAppointments(currentUser);
      if (res.data) {
        const upcoming = [];
        const cancelled = [];
        const history = [];

        res.data.forEach((app) => {
          const card = {
            id: `MED-${app.appoinments_id}`,
            rawId: app.appoinments_id,
            badgeDay: app.date ? app.date.split('-').slice(1).reverse().join('/') : 'Hôm nay',
            badgeTime: app.startTime ? app.startTime.substring(0, 5) : '09:00',
            isPrimaryDate: true,
            status: app.status === 'CONFIRMED' ? 'Đã xác nhận & Giữ slot' : (app.status || 'Đang xử lý'),
            statusType: app.status === 'CONFIRMED' ? 'confirmed' : 'holding',
            room: app.doctor?.room_number || 'Phòng khám theo phân bổ',
            doctor: app.doctorUsername
              ? `BS. ${app.doctorUsername} • ${app.doctor?.specialty || 'Chuyên khoa'}`
              : (app.doctor?.username ? `BS. ${app.doctor.username} • ${app.doctor?.specialty || 'Chuyên khoa'}` : 'Bác sĩ điều trị'),
            location: `${HOSPITAL_NAME} • ${app.doctor?.room_number || 'Khu khám bệnh nội bộ'}`,
            slotTime: app.startTime ? `${app.startTime.substring(0, 5)}` : 'Giờ hẹn',
            slotDay: app.date || 'Hôm nay',
            reason: app.reason || 'Khám bệnh theo yêu cầu',
            extraTag: null,
            canEditReason: true,
            notice: 'Vui lòng có mặt trước 15 phút để làm thủ tục tiếp nhận nội bộ',
          };

          if (app.status === 'CANCELLED' || (app.status && app.status.includes('REJECT'))) {
            cancelled.push({
              id: `MED-${app.appoinments_id}`,
              rawId: app.appoinments_id,
              title: card.doctor,
              reason: `Hủy ngày ${app.date || 'gần đây'} • Lý do: ${app.reason || 'Không rõ'}`,
            });
          } else if (app.status === 'COMPLETED' || app.status === 'HISTORY') {
            history.push({
              id: `MED-${app.appoinments_id}`,
              rawId: app.appoinments_id,
              title: card.doctor,
              sub: `Ngày ${app.date || ''} • Chẩn đoán: ${app.reason || 'Khám hoàn tất'}`,
            });
          } else {
            upcoming.push(card);
          }
        });

        setUpcomingList(upcoming);
        setCancelledList(cancelled);
        setHistoryList(history);
      }
    } catch (err) {
      console.warn('Backend appointment fetch error:', err);
    }
  };

  // Lọc danh sách bác sĩ theo chuyên khoa được chọn (hoặc tất cả)
  const filteredDoctors = doctors.filter((doc) => {
    if (selectedDepartment === 'Tất cả chuyên khoa nội bộ') return true;
    const docSpec = (doc.specialty || '').toLowerCase();
    const target = selectedDepartment.toLowerCase().replace('khoa ', '').trim();
    return docSpec.includes(target);
  });

  // Build slot items từ dữ liệu thật trong DB
  const currentDateObj = availableDates[selectedDateIndex] || availableDates[0];
  const dateSlots = dbSlots.filter(
    (s) => (s.workDate === currentDateObj.fullDate || s.date === currentDateObj.fullDate)
  );

  const displaySlots = dateSlots.map((s) => ({
    id: s.DoctorSlot_Id || s.doctorSlot_Id || s.id,
    time: `${s.startTime ? s.startTime.substring(0, 5) : '08:00'} - ${s.endTime ? s.endTime.substring(0, 5) : '08:30'}`,
    disabled: s.book === true,
  }));

  // Handler: Create Appointment (Lưu trực tiếp dữ liệu thật vào DB nội bộ)
  const handleCreateAppointment = async (e) => {
    e.preventDefault();
    if (!currentUser) {
      showToast('Vui lòng đăng nhập tài khoản bệnh nhân để đặt lịch khám.', 'warning');
      setModalAuth(true);
      return;
    }

    if (!selectedDoctorId) {
      showToast('Hiện chưa có bác sĩ nào thuộc khoa này. Vui lòng chọn khoa hoặc bác sĩ khác.', 'warning');
      return;
    }

    if (displaySlots.length === 0) {
      showToast('Bác sĩ chưa mở slot khám nào cho ngày này. Vui lòng chọn ngày khác.', 'warning');
      return;
    }

    if (!selectedSlot) {
      showToast('Vui lòng chọn một slot khám khả dụng.', 'warning');
      return;
    }

    if (!symptomReason.trim()) {
      showToast('Vui lòng điền lý do khám hoặc triệu chứng sơ bộ.', 'warning');
      return;
    }

    const dateObj = availableDates[selectedDateIndex];
    const chosenDoctor = doctors.find((d) => String(d.doctor_id) === String(selectedDoctorId));

    try {
      const res = await userApi.bookAppointment({
        username: currentUser,
        doctorId: chosenDoctor ? chosenDoctor.doctor_id : null,
        specialty: chosenDoctor?.specialty || (selectedDepartment !== 'Tất cả chuyên khoa nội bộ' ? selectedDepartment : 'Khoa Khám Bệnh Nội Bộ'),
        date: dateObj.fullDate,
        startTime: selectedSlot.split(' - ')[0],
        slotId: selectedSlotId ? Number(selectedSlotId) : null,
        reason: symptomReason,
      });

      showToast(`Tạo phiên khám thành công! Mã số hẹn: #${res.data?.appoinments_id || ''}`, 'check_circle');
      setSymptomReason('');
      setSelectedSlot('');
      setSelectedSlotId(null);
      loadBackendAppointments();
      if (selectedDoctorId) {
        loadDoctorSlots(selectedDoctorId);
      }
    } catch (err) {
      showToast(err.message || 'Lỗi đặt lịch hẹn', 'error');
    }
  };

  // Handler: Reschedule Confirm
  const handleConfirmReschedule = async () => {
    if (!modalReschedule) return;
    try {
      const rawId = modalReschedule.rawId || Number(String(modalReschedule.id).replace(/\D/g, ''));
      let st = rescheduleTime.split(' - ')[0].trim();
      if (st.length === 5) st += ':00';

      await appointmentApi.update({
        appoinmentId: rawId,
        workDate: rescheduleDate || availableDates[0].fullDate,
        startTime: st,
        reason: rescheduleReason,
      });

      showToast('Đã dời phiên khám và cấp giờ hẹn mới trong hệ thống!', 'update');
      setModalReschedule(null);
      loadBackendAppointments();
      if (selectedDoctorId) {
        loadDoctorSlots(selectedDoctorId);
      }
    } catch (err) {
      showToast(err.message || 'Lỗi cập nhật lịch hẹn', 'error');
    }
  };

  // Handler: Cancel Confirm
  const handleConfirmCancel = async () => {
    if (!modalCancel) return;
    try {
      const rawId = modalCancel.rawId || Number(String(modalCancel.id).replace(/\D/g, ''));
      await appointmentApi.cancel(rawId);
      showToast('Đã hủy phiên khám thành công và giải phóng slot!', 'delete');
      setModalCancel(null);
      loadBackendAppointments();
      if (selectedDoctorId) {
        loadDoctorSlots(selectedDoctorId);
      }
    } catch (err) {
      showToast(err.message || 'Lỗi hủy lịch hẹn', 'error');
    }
  };

  // Handler: Save Symptom
  const handleSaveSymptom = async (newSymptom) => {
    if (!modalEditSymptom) return;
    try {
      const rawId = modalEditSymptom.rawId || Number(String(modalEditSymptom.id).replace(/\D/g, ''));
      await appointmentApi.update({
        appoinmentId: rawId,
        reason: newSymptom,
      });
      showToast('Cập nhật lý do khám thành công vào cơ sở dữ liệu!', 'edit');
      setModalEditSymptom(null);
      loadBackendAppointments();
    } catch (err) {
      showToast(err.message || 'Lỗi cập nhật lý do khám', 'error');
    }
  };

  // Handler: File Upload
  const handleFileUpload = (e) => {
    const file = e.target.files && e.target.files[0];
    if (file) {
      const isPdf = file.name.endsWith('.pdf');
      const newFile = {
        id: Date.now(),
        name: file.name,
        meta: `${(file.size / (1024 * 1024)).toFixed(1)} MB • Vừa tải lên`,
        icon: isPdf ? 'picture_as_pdf' : 'image',
        color: isPdf ? '#ba1a1a' : '#006398',
        bg: isPdf ? 'rgba(186, 26, 26, 0.1)' : 'rgba(0, 99, 152, 0.1)',
      };
      setAttachedFiles([newFile, ...attachedFiles]);
      showToast(`Đã tải lên tệp: ${file.name}`, 'cloud_done');
    }
  };

  // Handler: Delete File
  const handleDeleteFile = (id) => {
    if (confirm('Xác nhận xóa tài liệu y tế này khỏi phiên lưu trữ?')) {
      setAttachedFiles(attachedFiles.filter((f) => f.id !== id));
      showToast('Đã xóa tệp đính kèm.', 'delete');
    }
  };

  // ==================== AUTH HANDLERS ====================

  // 1. ĐĂNG KÝ BỆNH NHÂN: Tạo tài khoản trực tiếp, chuyển sang màn hình đăng nhập (không đòi OTP khi đăng ký)
  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await userApi.register({
        username: authForm.username,
        fullName: authForm.fullName || authForm.username,
        phone: authForm.phone,
        email: authForm.email,
        password: authForm.password,
        confirmPassword: authForm.confirePassword,
        confirePassword: authForm.confirePassword,
      });

      showToast(res.message || 'Đăng ký tài khoản bệnh nhân thành công! Vui lòng đăng nhập.', 'check_circle');
      // Chuyển sang bước đăng nhập
      setAuthMode('login');
      setAuthForm((prev) => ({ ...prev, password: '', confirePassword: '', otp: '' }));
    } catch (err) {
      showToast(err.message || 'Lỗi đăng ký tài khoản', 'error');
    }
  };

  // 2. ĐĂNG NHẬP BƯỚC 1: Nhập username + password -> Kiểm tra hợp lệ -> Nhận mã OTP xác nhận
  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await userApi.login({
        username: authForm.username,
        password: authForm.password,
      });

      // Nếu backend yêu cầu xác thực OTP (luồng chuẩn 2FA)
      if (res.data?.requireOtp) {
        if (res.data?.otp) {
          setAuthForm((prev) => ({ ...prev, otp: String(res.data.otp) }));
        }
        showToast(res.message || 'Mã OTP xác nhận đăng nhập đã được gửi tới email! Vui lòng nhập OTP.', 'mark_email_read');
        setAuthMode('otp');
        return;
      }

      // Đăng nhập thành công ngay nếu không yêu cầu OTP
      showToast('Đăng nhập thành công!', 'verified');
      if (res.data) {
        onLoginSuccess(res.data.username, res.data.roles ? res.data.roles[0] : 'ROLE_USER');
      }
      setModalAuth(false);
    } catch (err) {
      showToast(err.message || 'Lỗi đăng nhập', 'error');
    }
  };

  // 3. ĐĂNG NHẬP BƯỚC 2: Nhập OTP để hoàn tất ĐĂNG NHẬP
  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await userApi.login({
        username: authForm.username,
        otp: Number(authForm.otp),
      });

      showToast(res.message || 'Xác nhận OTP thành công! Đăng nhập thành công.', 'check_circle');
      if (res.data) {
        onLoginSuccess(res.data.username, res.data.roles ? res.data.roles[0] : 'ROLE_USER');
      }
      setModalAuth(false);
    } catch (err) {
      showToast(err.message || 'Mã OTP không chính xác hoặc đã hết hạn', 'error');
    }
  };

  const patientName = currentUser
    ? (userProfile?.full_name || userProfile?.username || currentUser)
    : 'Khách (Chưa đăng nhập)';
  const patientId = currentUser && userProfile?.user_id ? userProfile.user_id : '--';
  const patientPhone = currentUser && userProfile?.phone ? userProfile.phone : '--';
  const patientInsurance = currentUser ? (userProfile?.insuranceNumber || 'Đã liên kết BHYT') : 'Chưa đăng nhập';

  return (
    <div className="page-container">
      {/* Toast Notice */}
      {toast && (
        <div className="toast-notice">
          <span className="material-symbols-outlined" style={{ color: 'var(--primary-fixed)' }}>
            {toast.icon}
          </span>
          <span>{toast.message}</span>
        </div>
      )}

      {/* ==================== PATIENT IDENTITY BANNER ==================== */}
      <div className="patient-banner-card">
        <div className="patient-banner-glow"></div>

        <div className="patient-info-left">
          <div className="patient-avatar-box">
            <span className="material-symbols-outlined" style={{ fontSize: '36px' }}>
              badge
            </span>
            <div className="avatar-verified-badge">✓</div>
          </div>

          <div className="patient-meta-text">
            <div className="patient-header-row">
              <h1 className="patient-title">Cổng Đặt Lịch & Điều Hành Nội Bộ • {HOSPITAL_NAME}</h1>
              <span className={`pill-badge ${currentUser && currentRole === 'ROLE_USER' ? 'pill-primary' : 'pill-secondary'}`}>
                {currentUser && currentRole === 'ROLE_USER' ? 'BỆNH NHÂN CHÍNH CHỦ' : (currentUser ? 'TÀI KHOẢN: ' + currentUser : 'CHƯA ĐĂNG NHẬP')}
              </span>
            </div>

            <div className="patient-details-line">
              <span>
                Họ và tên:{' '}
                <strong style={{ color: 'var(--on-surface)' }}>
                  {patientName}
                </strong>
              </span>
              <span className="meta-dot">•</span>
              <span>
                Mã hồ sơ: <span className="mono-tag">PID: #BN-{patientId}</span>
              </span>
              <span className="meta-dot">•</span>
              <span>
                Số điện thoại: <strong style={{ color: 'var(--on-surface)' }}>{patientPhone}</strong>
              </span>
              <span className="meta-dot">•</span>
              <span>
                Mã BHYT: <strong style={{ color: 'var(--on-surface)' }}>{patientInsurance}</strong>
              </span>
            </div>
          </div>
        </div>

        <div className="patient-actions-right">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              if (upcomingList.length > 0) {
                setModalQr(upcomingList[0]);
              } else {
                showToast('Chưa có lịch hẹn nào để mở thẻ QR');
              }
            }}
          >
            <span className="material-symbols-outlined" style={{ color: 'var(--primary)' }}>
              qr_code_scanner
            </span>
            <span>Thẻ khám điện tử</span>
          </button>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              if (currentUser && currentRole === 'ROLE_USER') {
                showToast(`Tài khoản bệnh nhân ${currentUser} đã đăng nhập chính chủ.`);
              } else {
                setModalAuth(true);
              }
            }}
          >
            <span className="material-symbols-outlined">
              {currentUser && currentRole === 'ROLE_USER' ? 'verified' : 'account_circle'}
            </span>
            <span>{currentUser && currentRole === 'ROLE_USER' ? 'Bệnh nhân chính chủ' : 'Đăng nhập / Đăng ký'}</span>
          </button>
        </div>
      </div>

      {/* ==================== KHỐI 1: TẠO LỊCH KHÁM MỚI NHANH CHÓNG ==================== */}
      <section className="card-section">
        <div className="card-section-header">
          <div className="section-title-wrap">
            <div className="section-icon-badge">
              <span className="material-symbols-outlined">add_circle</span>
            </div>
            <div>
              <h2 className="section-title">Khối 1: Đặt Lịch Khám Nội Bộ Tại {HOSPITAL_NAME}</h2>
              <p className="section-desc">
                Hệ thống đặt lịch duy nhất cho 1 bệnh viện - Giữ slot và phân bổ bác sĩ theo chuyên khoa nội bộ
              </p>
            </div>
          </div>

          <div
            className="pill-badge pill-primary"
            style={{ fontSize: '12px', padding: '6px 14px' }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
              domain
            </span>
            <span>Hệ Thống Nội Bộ {HOSPITAL_NAME}</span>
          </div>
        </div>

        <form onSubmit={handleCreateAppointment}>
          {/* 3 Column Top Row */}
          <div className="form-grid-3">
            {/* Chuyên khoa nội bộ của bệnh viện */}
            <div className="form-group">
              <label className="form-label">
                <span className="material-symbols-outlined">medical_services</span>
                Chuyên khoa khám bệnh nội bộ *
              </label>
              <div className="select-wrapper">
                <select
                  className="form-select"
                  value={selectedDepartment}
                  onChange={(e) => {
                    const dept = e.target.value;
                    setSelectedDepartment(dept);
                    if (dept !== 'Tất cả chuyên khoa nội bộ') {
                      const matched = doctors.find((d) =>
                        (d.specialty || '').toLowerCase().includes(dept.toLowerCase().replace('khoa ', '').trim())
                      );
                      if (matched) {
                        setSelectedDoctorId(String(matched.doctor_id));
                      }
                    }
                  }}
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

            {/* Doctor from DB */}
            <div className="form-group">
              <label className="form-label">
                <span className="material-symbols-outlined">stethoscope</span>
                Bác sĩ trực khám * ({filteredDoctors.length} bác sĩ sẵn sàng)
              </label>
              <div className="select-wrapper">
                <select
                  className="form-select"
                  value={selectedDoctorId}
                  onChange={(e) => setSelectedDoctorId(e.target.value)}
                  required
                >
                  {filteredDoctors.length === 0 ? (
                    <option value="">-- Hiện chưa có bác sĩ nào thuộc chuyên khoa này --</option>
                  ) : (
                    filteredDoctors.map((doc) => (
                      <option key={doc.doctor_id} value={doc.doctor_id}>
                        BS. {doc.username} • {doc.specialty || 'Chuyên khoa'} • {doc.room_number || 'Phòng khám'} (Phí: {(doc.price || 250000).toLocaleString('vi-VN')}đ)
                      </option>
                    ))
                  )}
                </select>
                <span className="material-symbols-outlined select-arrow">arrow_drop_down</span>
              </div>
            </div>

            {/* Date Strip */}
            <div className="form-group">
              <label className="form-label">
                <span className="material-symbols-outlined">event</span>
                Ngày khám khả dụng *
              </label>
              <div className="date-chips-row">
                {availableDates.map((item, index) => (
                  <button
                    key={index}
                    type="button"
                    className={`date-chip ${selectedDateIndex === index ? 'active' : ''}`}
                    onClick={() => setSelectedDateIndex(index)}
                  >
                    <span className="date-chip-label">{item.label}</span>
                    <span className="date-chip-date">{item.date}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Slot Grid Selection */}
          <div className="form-group" style={{ marginBottom: '18px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label className="form-label">
                <span className="material-symbols-outlined">schedule</span>
                Chọn slot hẹn khả dụng trong ngày ({currentDateObj.date})
              </label>
              <span style={{ fontSize: '12px', color: 'var(--outline)' }}>
                {displaySlots.filter((s) => !s.disabled).length} slot khả dụng
              </span>
            </div>

            {displaySlots.length === 0 ? (
              <div style={{ padding: '16px', background: 'var(--surface-container-low)', borderRadius: '12px', textAlign: 'center', color: 'var(--outline)', fontSize: '13px' }}>
                <span className="material-symbols-outlined" style={{ verticalAlign: 'middle', marginRight: '6px' }}>info</span>
                Bác sĩ chưa mở slot khám khả dụng trong ngày này ({currentDateObj.date}). Vui lòng chọn ngày khác hoặc liên hệ phòng tiếp nhận.
              </div>
            ) : (
              <div className="slot-grid">
                {displaySlots.map((slot, idx) => (
                  <div
                    key={idx}
                    className={`slot-item ${slot.disabled ? 'disabled' : ''} ${
                      selectedSlot === slot.time ? 'selected' : ''
                    }`}
                    onClick={() => {
                      if (!slot.disabled) {
                        setSelectedSlot(slot.time);
                        setSelectedSlotId(slot.id);
                      }
                    }}
                  >
                    {selectedSlot === slot.time && !slot.disabled && (
                      <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                        check_circle
                      </span>
                    )}
                    <span>{slot.time}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Symptom Note & Submit */}
          <div className="form-submit-row">
            <div className="form-group">
              <label className="form-label" htmlFor="symptomInput">
                Lý do khám / Triệu chứng bệnh sơ bộ *
              </label>
              <input
                id="symptomInput"
                type="text"
                className="form-input"
                value={symptomReason}
                onChange={(e) => setSymptomReason(e.target.value)}
                placeholder="Ví dụ: Tức ngực trái âm ỉ khi vận động mạnh, tiền sử tăng huyết áp 3 năm..."
                required
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ height: '48px' }}>
              <span className="material-symbols-outlined">assignment_turned_in</span>
              <span>Xác nhận đặt lịch (Hệ thống nội bộ)</span>
            </button>
          </div>
        </form>
      </section>

      {/* ==================== MAIN TWO COLUMNS ==================== */}
      <div className="two-col-layout">
        {/* ================= LEFT COLUMN: LỊCH HẸN & PHIÊN KHÁM CỦA TÔI ================= */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {/* Section Header & Tab Switcher */}
          <div
            className="card-section"
            style={{
              padding: '16px 20px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '12px',
            }}
          >
            <div>
              <h2 className="section-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span className="material-symbols-outlined" style={{ color: 'var(--primary)' }}>
                  event_available
                </span>
                <span>Lịch Hẹn Khám Bệnh Của Tôi</span>
              </h2>
              <p className="section-desc">{HOSPITAL_NAME} - Dữ liệu thực tế từ Database</p>
            </div>

            <div className="tab-switcher">
              <button
                type="button"
                className={`tab-switcher-btn ${appointmentTab === 'upcoming' ? 'active' : ''}`}
                onClick={() => setAppointmentTab('upcoming')}
              >
                Phiên sắp tới ({upcomingList.length})
              </button>
              <button
                type="button"
                className={`tab-switcher-btn ${appointmentTab === 'history' ? 'active' : ''}`}
                onClick={() => setAppointmentTab('history')}
              >
                Lịch sử đã khám ({historyList.length})
              </button>
              <button
                type="button"
                className={`tab-switcher-btn ${appointmentTab === 'cancelled' ? 'active' : ''}`}
                onClick={() => setAppointmentTab('cancelled')}
              >
                Đã hủy ({cancelledList.length})
              </button>
            </div>
          </div>

          {/* TAB 1: PHIÊN SẮP TỚI */}
          {appointmentTab === 'upcoming' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {upcomingList.length === 0 ? (
                <div className="card-section" style={{ textAlign: 'center', padding: '40px 20px' }}>
                  <span className="material-symbols-outlined" style={{ fontSize: '48px', color: 'var(--outline)' }}>
                    calendar_today
                  </span>
                  <p style={{ marginTop: '12px', color: 'var(--outline)' }}>
                    Chưa có lịch hẹn sắp tới nào trong Database. Hãy đặt lịch ở Khối 1 bên trên!
                  </p>
                </div>
              ) : (
                upcomingList.map((app) => (
                  <div key={app.id} className="appointment-card">
                    {/* Top row */}
                    <div className="appointment-top-row">
                      <div className="appointment-doctor-group">
                        <div
                          className={`appointment-date-badge ${
                            app.isPrimaryDate ? 'primary' : 'neutral'
                          }`}
                        >
                          <span className="badge-month">{app.badgeDay}</span>
                          <span className="badge-time">{app.badgeTime}</span>
                        </div>

                        <div className="appointment-doctor-info">
                          <div className="appointment-pills-row">
                            <span
                              className={`pill-badge ${
                                app.statusType === 'confirmed' ? 'pill-primary' : 'pill-tertiary'
                              }`}
                            >
                              <span
                                style={{
                                  width: '6px',
                                  height: '6px',
                                  borderRadius: '50%',
                                  background: 'currentColor',
                                }}
                              ></span>
                              <span>{app.status}</span>
                            </span>

                            <span className="pill-badge pill-secondary">{app.room}</span>
                            <span style={{ fontSize: '12px', color: 'var(--outline)' }}>
                              Mã phiên: #{app.id}
                            </span>
                          </div>

                          <h3 className="appointment-doctor-name">{app.doctor}</h3>
                          <p className="appointment-location">
                            <span
                              className="material-symbols-outlined"
                              style={{ fontSize: '16px', color: 'var(--outline)' }}
                            >
                              location_on
                            </span>
                            <span>{app.location}</span>
                          </p>
                        </div>
                      </div>

                      <div className="appointment-time-right">
                        <span className="time-highlight">{app.slotTime}</span>
                        <span className="time-date-sub">{app.slotDay}</span>
                      </div>
                    </div>

                    {/* Medical reason bar */}
                    <div className="reason-pill-bar">
                      <div className="reason-text-wrap">
                        <span
                          className="material-symbols-outlined"
                          style={{
                            color: app.canEditReason ? 'var(--secondary)' : 'var(--primary)',
                            fontSize: '18px',
                          }}
                        >
                          {app.canEditReason ? 'edit_note' : 'clinical_notes'}
                        </span>
                        <p className="reason-text-truncate">
                          <strong>Lý do/Triệu chứng:</strong> {app.reason}
                        </p>
                      </div>

                      <button
                        type="button"
                        style={{
                          border: 'none',
                          background: 'transparent',
                          color: 'var(--primary)',
                          fontWeight: 700,
                          cursor: 'pointer',
                          fontSize: '12px',
                        }}
                        onClick={() =>
                          setModalEditSymptom({ id: app.id, rawId: app.rawId, reason: app.reason })
                        }
                      >
                        Chỉnh sửa triệu chứng (Update DB)
                      </button>
                    </div>

                    {/* Action Bar */}
                    <div className="appointment-action-bar">
                      <div className="action-notice">
                        <span
                          className="material-symbols-outlined"
                          style={{ fontSize: '16px', color: 'var(--primary)' }}
                        >
                          priority_high
                        </span>
                        <span>{app.notice}</span>
                      </div>

                      <div className="action-btns-group">
                        <button
                          type="button"
                          className="btn btn-sm btn-primary"
                          onClick={() =>
                            setModalQr({
                              id: app.id,
                              doctor: app.doctor,
                              time: `${app.slotTime} (${app.slotDay})`,
                              room: app.room,
                            })
                          }
                        >
                          <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                            qr_code_2
                          </span>
                          <span>Mã QR Pass (Read)</span>
                        </button>

                        <button
                          type="button"
                          className="btn btn-sm btn-outline"
                          onClick={() => {
                            setModalReschedule({
                              id: app.id,
                              rawId: app.rawId,
                              doctor: app.doctor,
                              currentSlot: `${app.slotTime} (${app.slotDay})`,
                            });
                            setRescheduleDate(app.slotDay);
                          }}
                        >
                          <span
                            className="material-symbols-outlined"
                            style={{ fontSize: '16px', color: 'var(--secondary)' }}
                          >
                            update
                          </span>
                          <span>Dời phiên / Đổi giờ (Update DB)</span>
                        </button>

                        <button
                          type="button"
                          className="btn btn-sm btn-danger-light"
                          onClick={() =>
                            setModalCancel({
                              id: app.id,
                              rawId: app.rawId,
                              doctor: app.doctor,
                            })
                          }
                        >
                          <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                            cancel
                          </span>
                          <span>Hủy phiên (Giải phóng slot)</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* TAB 2: LỊCH SỬ ĐÃ KHÁM */}
          {appointmentTab === 'history' && (
            <div className="card-section" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 700, fontSize: '15px' }}>
                  Lượt khám hoàn tất trong hệ thống
                </span>
                <span style={{ fontSize: '12px', color: 'var(--outline)' }}>
                  Lưu trữ từ cơ sở dữ liệu {HOSPITAL_NAME}
                </span>
              </div>

              {historyList.length === 0 ? (
                <p style={{ color: 'var(--outline)', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>
                  Chưa có lịch sử khám hoàn tất.
                </p>
              ) : (
                historyList.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 16px',
                      borderRadius: '12px',
                      background: 'var(--surface)',
                      gap: '12px',
                      flexWrap: 'wrap',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div
                        style={{
                          width: '36px',
                          height: '36px',
                          borderRadius: '50%',
                          background: 'rgba(0, 104, 95, 0.1)',
                          color: 'var(--primary)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
                          task_alt
                        </span>
                      </div>
                      <div>
                        <h4 style={{ fontSize: '13.5px', fontWeight: 600 }}>{item.title}</h4>
                        <p style={{ fontSize: '12px', color: 'var(--on-surface-variant)' }}>
                          {item.sub}
                        </p>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        type="button"
                        className="btn btn-sm btn-secondary"
                        onClick={() => showToast('Mở toa thuốc điện tử')}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                          receipt_long
                        </span>
                        <span>Xem toa thuốc</span>
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* TAB 3: ĐÃ HỦY */}
          {appointmentTab === 'cancelled' && (
            <div className="card-section">
              {cancelledList.length === 0 ? (
                <p style={{ color: 'var(--outline)', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>
                  Không có lịch hẹn bị hủy trong hệ thống.
                </p>
              ) : (
                cancelledList.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '16px',
                      borderRadius: '12px',
                      background: 'rgba(255, 218, 214, 0.3)',
                      gap: '12px',
                      flexWrap: 'wrap',
                      marginBottom: '10px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div
                        style={{
                          width: '38px',
                          height: '38px',
                          borderRadius: '50%',
                          background: 'var(--error-container)',
                          color: 'var(--on-error-container)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <span className="material-symbols-outlined">event_busy</span>
                      </div>
                      <div>
                        <h4 style={{ fontSize: '14px', fontWeight: 700 }}>{item.title}</h4>
                        <p style={{ fontSize: '12px', color: 'var(--error)' }}>{item.reason}</p>
                      </div>
                    </div>

                    <button
                      type="button"
                      className="btn btn-sm btn-secondary"
                      onClick={() => {
                        setAppointmentTab('upcoming');
                        showToast('Vui lòng chọn ngày và giờ mới ở Khối 1');
                      }}
                    >
                      Đặt lại phiên này
                    </button>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* ================= RIGHT COLUMN: HỒ SƠ & KẾT QUẢ ================= */}
        <aside className="side-column">
          <div className="side-card">
            <div>
              <h3 className="side-card-title">
                <span className="material-symbols-outlined" style={{ color: 'var(--secondary)' }}>
                  folder_shared
                </span>
                <span>Hồ Sơ & Kết Quả Nội Bộ</span>
              </h3>
              <p style={{ fontSize: '12px', color: 'var(--outline)', marginTop: '2px' }}>
                Lưu trữ hồ sơ cận lâm sàng tại {HOSPITAL_NAME}
              </p>
            </div>

            {/* Upload Dropzone */}
            <div
              className="upload-dropzone"
              onClick={() => document.getElementById('fileUploadInput').click()}
            >
              <input
                id="fileUploadInput"
                type="file"
                style={{ display: 'none' }}
                onChange={handleFileUpload}
              />
              <div className="upload-icon-circle">
                <span className="material-symbols-outlined" style={{ fontSize: '24px' }}>
                  cloud_upload
                </span>
              </div>
              <span className="upload-main-text">Tải lên kết quả cận lâm sàng mới</span>
              <span className="upload-sub-text">PDF, JPG, PNG tối đa 15MB (Bảo mật y tế)</span>
            </div>

            {/* Attached Files List */}
            {attachedFiles.length === 0 ? (
              <div style={{ padding: '16px 12px', textAlign: 'center', color: 'var(--outline)', fontSize: '12.5px', background: 'var(--surface-container-low)', borderRadius: '12px' }}>
                <span className="material-symbols-outlined" style={{ display: 'block', fontSize: '24px', margin: '0 auto 6px', color: 'var(--outline)' }}>
                  description
                </span>
                Chưa có tài liệu đính kèm. Nhấp vào khung phía trên để tải lên kết quả xét nghiệm hoặc đơn thuốc thực tế của bạn.
              </div>
            ) : (
              <div className="attached-files-list">
                {attachedFiles.map((f) => (
                  <div key={f.id} className="attached-file-item">
                    <div className="file-info-left">
                      <div
                        className="file-icon-square"
                        style={{ background: f.bg, color: f.color }}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
                          {f.icon}
                        </span>
                      </div>
                      <div className="file-name-meta">
                        <span className="file-title">{f.name}</span>
                        <span className="file-meta-sub">{f.meta}</span>
                      </div>
                    </div>

                    <div className="file-actions">
                      <button
                        type="button"
                        className="file-icon-btn"
                        title="Tải xuống tài liệu"
                        onClick={() => showToast(`Đang tải xuống: ${f.name}`)}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                          download
                        </span>
                      </button>
                      <button
                        type="button"
                        className="file-icon-btn delete"
                        title="Xóa tài liệu"
                        onClick={() => handleDeleteFile(f.id)}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                          delete
                        </span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Privacy Metric Box */}
            <div className="privacy-metric-box">
              <span
                style={{
                  fontSize: '10.5px',
                  fontWeight: 700,
                  color: 'var(--outline)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.6px',
                }}
              >
                Bảo mật dữ liệu nội bộ
              </span>
              <div className="privacy-title-row">
                <span>Mã hóa y tế nội bộ AES-256</span>
                <span className="privacy-dot"></span>
              </div>
              <p style={{ fontSize: '11.5px', color: 'var(--on-surface-variant)', lineHeight: '1.4' }}>
                Dữ liệu chỉ được lưu trữ và truy cập bởi đội ngũ y bác sĩ trực thuộc {HOSPITAL_NAME}.
              </p>
            </div>
          </div>

          {/* Hotline Card */}
          <div
            className="hotline-card"
            onClick={() => alert(`Tổng đài hỗ trợ nội bộ ${HOSPITAL_NAME}: 1900 2115 (Khoa Cấp Cứu 24/7)`)}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <span
                className="material-symbols-outlined"
                style={{ color: 'var(--primary)', fontSize: '26px' }}
              >
                support_agent
              </span>
              <div>
                <p style={{ fontWeight: 700, fontSize: '13px' }}>Đường dây nóng cấp cứu nội bộ</p>
                <p style={{ fontSize: '12px', color: 'var(--on-surface-variant)' }}>
                  Tổng đài {HOSPITAL_NAME}: <strong style={{ color: 'var(--primary)' }}>1900 2115</strong>
                </p>
              </div>
            </div>
            <span className="material-symbols-outlined" style={{ color: 'var(--outline)' }}>
              chevron_right
            </span>
          </div>
        </aside>
      </div>

      {/* ==================== MODAL 1: QR PASS (READ) ==================== */}
      {modalQr && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '420px' }}>
            <div className="modal-header">
              <div className="modal-title">
                <span className="material-symbols-outlined" style={{ color: 'var(--primary)' }}>
                  qr_code_2
                </span>
                <span>Thẻ Phiên Khám QR Pass</span>
              </div>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setModalQr(null)}
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="qr-display-box">
              <div className="qr-code-img">
                <svg viewBox="0 0 100 100" style={{ width: '100%', height: '100%' }}>
                  <path
                    d="M10 10h30v30h-30zM50 10h10v10h-10zM70 10h20v20h-20zM15 15v20h20v-20zM75 15v10h10v-10zM20 20h10v10h-10zM50 30h10v10h-10zM70 35h10v10h-10zM85 35h5v20h-5zM10 50h10v10h-10zM30 50h10v10h-10zM50 50h20v20h-20zM10 70h30v20h-30zM15 75v10h20v-10zM20 80h10v0h-10zM50 75h10v15h-10zM70 70h20v20h-20zM75 75v10h10v-10z"
                    fill="var(--on-surface)"
                  />
                </svg>
              </div>
              <strong style={{ fontFamily: 'monospace', fontSize: '18px', color: 'var(--primary)' }}>
                #{modalQr.id}
              </strong>
              <span style={{ fontSize: '11px', color: 'var(--outline)', textTransform: 'uppercase' }}>
                Quét tại Kiosk tiếp nhận {HOSPITAL_NAME}
              </span>
            </div>

            <div
              style={{
                background: 'var(--surface-container-low)',
                padding: '14px',
                borderRadius: '12px',
                fontSize: '13px',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--on-surface-variant)' }}>Bệnh nhân:</span>
                <strong>{patientName}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--on-surface-variant)' }}>Bác sĩ:</span>
                <strong>{modalQr.doctor}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--on-surface-variant)' }}>Khung giờ:</span>
                <strong style={{ color: 'var(--primary)' }}>{modalQr.time}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--on-surface-variant)' }}>Vị trí:</span>
                <strong>{modalQr.room}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--on-surface-variant)' }}>Bệnh viện:</span>
                <strong style={{ color: 'var(--secondary)' }}>{HOSPITAL_NAME}</strong>
              </div>
            </div>

            <button
              type="button"
              className="btn btn-primary"
              style={{ width: '100%' }}
              onClick={() => {
                setModalQr(null);
                showToast('Đã lưu thẻ phiên khám vào thiết bị!');
              }}
            >
              Đã lưu thẻ lên thiết bị
            </button>
          </div>
        </div>
      )}

      {/* ==================== MODAL 2: RESCHEDULE ==================== */}
      {modalReschedule && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">
                <span className="material-symbols-outlined" style={{ color: 'var(--secondary)' }}>
                  calendar_month
                </span>
                <span>Dời Phiên & Đổi Giờ Khám ({HOSPITAL_NAME})</span>
              </div>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setModalReschedule(null)}
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div
              style={{
                background: 'var(--surface-container-low)',
                padding: '14px',
                borderRadius: '12px',
              }}
            >
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  color: 'var(--outline)',
                  textTransform: 'uppercase',
                }}
              >
                Lịch hẹn đang chọn:
              </span>
              <p style={{ fontWeight: 700, fontSize: '15px', marginTop: '2px' }}>
                {modalReschedule.doctor}
              </p>
              <p style={{ fontSize: '13px', color: 'var(--secondary)', marginTop: '2px' }}>
                Thời gian hiện tại: {modalReschedule.currentSlot}
              </p>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleConfirmReschedule();
              }}
              style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}
            >
              <div className="form-group">
                <label className="form-label">Chọn ngày khám thay thế:</label>
                <div className="select-wrapper">
                  <select
                    className="form-select"
                    value={rescheduleDate}
                    onChange={(e) => setRescheduleDate(e.target.value)}
                  >
                    {availableDates.map((item, idx) => (
                      <option key={idx} value={item.fullDate}>
                        {item.label} - {item.fullDate}
                      </option>
                    ))}
                  </select>
                  <span className="material-symbols-outlined select-arrow">arrow_drop_down</span>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Nhập khung giờ hẹn mới:</label>
                <input
                  type="text"
                  className="form-input"
                  value={rescheduleTime}
                  onChange={(e) => setRescheduleTime(e.target.value)}
                  placeholder="Ví dụ: 08:30 - 09:00 hoặc 14:00 - 14:30"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Lý do điều chỉnh:</label>
                <input
                  type="text"
                  className="form-input"
                  value={rescheduleReason}
                  onChange={(e) => setRescheduleReason(e.target.value)}
                  placeholder="Nhập lý do dời lịch khám..."
                  required
                />
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setModalReschedule(null)}
                >
                  Hủy thao tác
                </button>
                <button type="submit" className="btn btn-primary">
                  Lưu & Cập Nhật Lịch Hẹn
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==================== MODAL 3: CANCEL ==================== */}
      {modalCancel && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '440px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: '50%',
                  background: 'var(--error-container)',
                  color: 'var(--on-error-container)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                <span className="material-symbols-outlined">warning</span>
              </div>
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 700 }}>Xác nhận Hủy Phiên Khám</h3>
                <p style={{ fontSize: '12px', color: 'var(--outline)' }}>
                  Hệ thống sẽ cập nhật Database và giải phóng slot hẹn
                </p>
              </div>
            </div>

            <p style={{ fontSize: '13.5px', color: 'var(--on-surface-variant)', lineHeight: '1.5' }}>
              Bạn có chắc chắn muốn hủy lịch khám với <strong>{modalCancel.doctor}</strong>? Phiên
              khám này sẽ được chuyển sang trạng thái "CANCELLED" trong Database.
            </p>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleConfirmCancel();
              }}
              style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}
            >
              <div className="form-group">
                <label className="form-label">Lý do hủy hẹn *</label>
                <div className="select-wrapper">
                  <select className="form-select" required>
                    <option value="1">Triệu chứng đã đỡ, không cần khám lại</option>
                    <option value="2">Bận việc gia đình / công tác đột xuất</option>
                    <option value="3">Muốn đổi sang bác sĩ khác cùng chuyên khoa</option>
                    <option value="4">Lý do cá nhân khác</option>
                  </select>
                  <span className="material-symbols-outlined select-arrow">arrow_drop_down</span>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setModalCancel(null)}
                >
                  Quay lại giữ phiên
                </button>
                <button type="submit" className="btn btn-danger-light">
                  Xác Nhận Hủy (Cập nhật DB)
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==================== MODAL 4: EDIT SYMPTOM ==================== */}
      {modalEditSymptom && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '440px' }}>
            <div className="modal-header">
              <div className="modal-title">
                <span className="material-symbols-outlined" style={{ color: 'var(--primary)' }}>
                  edit_note
                </span>
                <span>Cập Nhật Ghi Chú Bệnh (Database)</span>
              </div>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setModalEditSymptom(null)}
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                const inputVal = e.target.elements.symptomText.value;
                handleSaveSymptom(inputVal);
              }}
              style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}
            >
              <div className="form-group">
                <label className="form-label">Triệu chứng & Lý do khám mới:</label>
                <textarea
                  name="symptomText"
                  rows="3"
                  className="form-textarea"
                  defaultValue={modalEditSymptom.reason}
                  required
                />
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setModalEditSymptom(null)}
                >
                  Đóng
                </button>
                <button type="submit" className="btn btn-primary">
                  Lưu Vào Database
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==================== MODAL 5: AUTHENTICATION (LOGIN VỚI OTP / ĐĂNG KÝ) ==================== */}
      {modalAuth && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '460px' }}>
            <div className="modal-header">
              <div className="modal-title">
                <span className="material-symbols-outlined" style={{ color: 'var(--primary)' }}>
                  account_circle
                </span>
                <span>Cổng Bệnh Nhân • {HOSPITAL_NAME}</span>
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

            {/* Switch tabs (Chỉ có 2 tab chính: Đăng nhập và Đăng ký) */}
            {authMode !== 'otp' ? (
              <div className="tab-switcher" style={{ width: '100%', marginBottom: '16px' }}>
                <button
                  type="button"
                  style={{ flex: 1 }}
                  className={`tab-switcher-btn ${authMode === 'login' ? 'active' : ''}`}
                  onClick={() => setAuthMode('login')}
                >
                  Đăng nhập
                </button>
                <button
                  type="button"
                  style={{ flex: 1 }}
                  className={`tab-switcher-btn ${authMode === 'register' ? 'active' : ''}`}
                  onClick={() => setAuthMode('register')}
                >
                  Đăng ký tài khoản
                </button>
              </div>
            ) : (
              <div style={{
                background: 'var(--surface-container-low)',
                padding: '10px 14px',
                borderRadius: '10px',
                marginBottom: '16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span className="material-symbols-outlined" style={{ color: 'var(--primary)' }}>security</span>
                  <span style={{ fontSize: '13px', fontWeight: 600 }}>Bước 2: Nhập mã OTP xác nhận đăng nhập</span>
                </div>
                <button
                  type="button"
                  style={{ background: 'transparent', border: 'none', color: 'var(--primary)', cursor: 'pointer', fontSize: '12px', fontWeight: 700 }}
                  onClick={() => setAuthMode('login')}
                >
                  Đổi tài khoản
                </button>
              </div>
            )}

            {/* BƯỚC 1: FORM ĐĂNG NHẬP (Username + Password) */}
            {authMode === 'login' && (
              <form onSubmit={handleLoginSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div className="form-group">
                  <label className="form-label">Tên đăng nhập (Username):</label>
                  <input
                    type="text"
                    className="form-input"
                    value={authForm.username}
                    onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                    placeholder="Ví dụ: tranminhquan"
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

                <div style={{ padding: '8px 12px', background: 'var(--surface-container-low)', borderRadius: '8px', fontSize: '12px', color: 'var(--outline)' }}>
                  <span className="material-symbols-outlined" style={{ fontSize: '15px', verticalAlign: 'middle', marginRight: '4px', color: 'var(--primary)' }}>info</span>
                  Sau khi nhập tài khoản và mật khẩu, hệ thống sẽ gửi mã OTP xác nhận tới email của bạn để đăng nhập.
                </div>

                <button type="submit" className="btn btn-primary" style={{ height: '46px', marginTop: '6px' }}>
                  <span className="material-symbols-outlined">send_to_mobile</span>
                  <span>Đăng nhập & Gửi mã OTP xác nhận</span>
                </button>
              </form>
            )}

            {/* FORM ĐĂNG KÝ BỆNH NHÂN (Không đòi hỏi OTP khi đăng ký) */}
            {authMode === 'register' && (
              <form onSubmit={handleRegisterSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div className="form-group">
                  <label className="form-label">Tên đăng nhập (Username) *:</label>
                  <input
                    type="text"
                    className="form-input"
                    value={authForm.username}
                    onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                    placeholder="tranminhquan"
                    required
                  />
                </div>

                <div className="form-grid-2" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div className="form-group">
                    <label className="form-label">Họ và tên *:</label>
                    <input
                      type="text"
                      className="form-input"
                      value={authForm.fullName}
                      onChange={(e) => setAuthForm({ ...authForm, fullName: e.target.value })}
                      placeholder="Trần Minh Quân"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Số điện thoại:</label>
                    <input
                      type="tel"
                      className="form-input"
                      value={authForm.phone}
                      onChange={(e) => setAuthForm({ ...authForm, phone: e.target.value })}
                      placeholder="0912345678"
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Email nhận OTP đăng nhập *:</label>
                  <input
                    type="email"
                    className="form-input"
                    value={authForm.email}
                    onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                    placeholder="quan.tran@example.com"
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

                <button type="submit" className="btn btn-primary" style={{ height: '46px', marginTop: '6px' }}>
                  <span className="material-symbols-outlined">person_add</span>
                  <span>Hoàn tất đăng ký tài khoản</span>
                </button>
              </form>
            )}

            {/* BƯỚC 2: FORM XÁC NHẬN MÃ OTP ĐĂNG NHẬP */}
            {authMode === 'otp' && (
              <form onSubmit={handleOtpSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ textAlign: 'center', padding: '10px 0' }}>
                  <div style={{
                    width: '56px',
                    height: '56px',
                    borderRadius: '50%',
                    background: 'rgba(0, 104, 95, 0.1)',
                    color: 'var(--primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 10px',
                  }}>
                    <span className="material-symbols-outlined" style={{ fontSize: '32px' }}>
                      verified_user
                    </span>
                  </div>
                  <h3 style={{ fontSize: '16px', fontWeight: 700 }}>Xác Nhận OTP Đăng Nhập</h3>
                  <p style={{ fontSize: '12.5px', color: 'var(--on-surface-variant)', marginTop: '4px' }}>
                    Tài khoản: <strong>{authForm.username}</strong>. Hãy nhập mã OTP 6 chữ số vừa nhận để hoàn tất đăng nhập.
                  </p>
                </div>

                <div className="form-group">
                  <label className="form-label" style={{ textAlign: 'center' }}>Mã OTP xác nhận (6 chữ số):</label>
                  <input
                    type="number"
                    className="form-input"
                    style={{ textAlign: 'center', fontSize: '22px', letterSpacing: '6px', fontWeight: 700, height: '52px' }}
                    value={authForm.otp}
                    onChange={(e) => setAuthForm({ ...authForm, otp: e.target.value })}
                    placeholder="123456"
                    required
                    autoFocus
                  />
                </div>

                <button type="submit" className="btn btn-primary" style={{ height: '48px', marginTop: '4px' }}>
                  <span className="material-symbols-outlined">check_circle</span>
                  <span>Xác nhận OTP & Đăng nhập</span>
                </button>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '6px' }}>
                  <button
                    type="button"
                    style={{ background: 'transparent', border: 'none', color: 'var(--outline)', cursor: 'pointer', fontSize: '12px' }}
                    onClick={() => setAuthMode('login')}
                  >
                    ← Quay lại đăng nhập
                  </button>
                  <button
                    type="button"
                    style={{ background: 'transparent', border: 'none', color: 'var(--primary)', cursor: 'pointer', fontSize: '12px', fontWeight: 600 }}
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
