import React, { useState, useEffect } from 'react';
import {
  Stethoscope,
  LogIn,
  UserPlus,
  KeyRound,
  CheckCircle,
  AlertCircle,
  PlusCircle,
  Clock,
  Calendar,
  RefreshCw,
  Trash2,
  XCircle,
  ArrowRight,
} from 'lucide-react';
import { doctorApi, slotApi } from '../services/api';

export default function DoctorSection({
  currentUser,
  currentRole,
  onLoginSuccess,
}) {
  const [subTab, setSubTab] = useState(
    currentRole === 'ROLE_DOCTOR' ? 'slots' : 'auth'
  );
  const [authMode, setAuthMode] = useState('login'); // 'login' | 'register' | 'otp'

  // Auth Form State
  const [authForm, setAuthForm] = useState({
    username: '',
    email: '',
    password: '',
    confirePassword: '',
    otp: '',
  });

  // Doctor ID input for API calls requiring doctorId
  const [doctorId, setDoctorId] = useState('1');

  // Slot Generator Form
  const [slotForm, setSlotForm] = useState({
    doctorId: '1',
    workdate: new Date().toISOString().split('T')[0],
    startShift: '08:00',
    endShift: '12:00',
    durationMinutes: 30,
  });

  // Data lists
  const [slots, setSlots] = useState([]);
  const [doctorAppointments, setDoctorAppointments] = useState([]);

  // Status
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);

  const showMsg = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 6000);
  };

  useEffect(() => {
    if (currentUser && currentRole === 'ROLE_DOCTOR') {
      loadSlots();
      loadAppointments();
    }
  }, [currentUser, currentRole]);

  // ==================== AUTH HANDLERS ====================
  const handleRegister = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await doctorApi.register({
        username: authForm.username,
        email: authForm.email,
        password: authForm.password,
        confirePassword: authForm.confirePassword,
      });
      showMsg('success', res.message || 'Đăng ký bác sĩ thành công! Hãy xác thực OTP.');
      setAuthMode('otp');
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await doctorApi.verifyOtp({
        email: authForm.email,
        otp: authForm.otp,
      });
      showMsg('success', res.message || 'Xác thực OTP thành công! Giờ bạn có thể đăng nhập.');
      setAuthMode('login');
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await doctorApi.login({
        username: authForm.username,
        password: authForm.password,
      });
      showMsg('success', 'Bác sĩ đăng nhập thành công!');
      if (res.data) {
        onLoginSuccess(res.data.username, 'ROLE_DOCTOR');
        setSubTab('slots');
      }
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  // ==================== SLOTS HANDLERS ====================
  const handleCreateSlots = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      // Backend expects startShift and endShift with seconds e.g. "08:00:00"
      const formatTime = (time) => (time.length === 5 ? `${time}:00` : time);

      await slotApi.create({
        doctorId: Number(slotForm.doctorId),
        workdate: slotForm.workdate,
        startShift: formatTime(slotForm.startShift),
        endShift: formatTime(slotForm.endShift),
        durationMinutes: Number(slotForm.durationMinutes),
      });

      showMsg('success', 'Tạo danh sách ca khám thành công!');
      loadSlots();
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadSlots = async () => {
    setLoading(true);
    try {
      const username = currentUser || authForm.username;
      if (username) {
        const res = await doctorApi.getSlots(username);
        setSlots(res.data || []);
      } else if (doctorId) {
        const res = await slotApi.read(Number(doctorId));
        setSlots(res.data || []);
      }
    } catch (err) {
      showMsg('error', 'Không thể tải danh sách slots: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteSlot = async (slotId) => {
    if (!confirm('Bạn có chắc muốn xóa slot khám #' + slotId + '?')) return;
    setLoading(true);
    try {
      await slotApi.delete(slotId);
      showMsg('success', 'Đã xóa slot khám #' + slotId);
      loadSlots();
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  // ==================== APPOINTMENTS HANDLERS ====================
  const loadAppointments = async () => {
    if (!doctorId) return;
    setLoading(true);
    try {
      const res = await doctorApi.getAppointments(Number(doctorId));
      setDoctorAppointments(res.data || []);
    } catch (err) {
      showMsg('error', 'Không thể tải lịch hẹn của bác sĩ: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async (appointmentId) => {
    if (!confirm('Từ chối và chuyển giao lịch hẹn #' + appointmentId + ' sang bác sĩ khác?')) return;
    setLoading(true);
    try {
      await doctorApi.rejectAppointment(Number(doctorId), appointmentId);
      showMsg('success', 'Đã từ chối và tự động chuyển giao lịch hẹn #' + appointmentId);
      loadAppointments();
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Alert Messages */}
      {message && (
        <div className={`alert ${message.type === 'success' ? 'alert-success' : 'alert-error'}`}>
          {message.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
          <span>{message.text}</span>
        </div>
      )}

      {/* Sub Navigation */}
      <div className="sub-tabs">
        {currentRole !== 'ROLE_DOCTOR' && (
          <button
            className={`sub-tab-item ${subTab === 'auth' ? 'active' : ''}`}
            onClick={() => setSubTab('auth')}
          >
            Đăng nhập / Đăng ký Bác sĩ
          </button>
        )}
        <button
          className={`sub-tab-item ${subTab === 'slots' ? 'active' : ''}`}
          onClick={() => {
            setSubTab('slots');
            loadSlots();
          }}
        >
          Quản lý Slot & Ca khám
        </button>
        <button
          className={`sub-tab-item ${subTab === 'appointments' ? 'active' : ''}`}
          onClick={() => {
            setSubTab('appointments');
            loadAppointments();
          }}
        >
          Lịch hẹn bệnh nhân ({doctorAppointments.length})
        </button>
      </div>

      {/* ================= TAB 1: AUTH BÁC SĨ ================= */}
      {subTab === 'auth' && (
        <div className="card" style={{ maxWidth: '480px', margin: '0 auto 24px auto' }}>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
            <button
              className={`btn ${authMode === 'login' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
              style={{ flex: 1 }}
              onClick={() => setAuthMode('login')}
            >
              <LogIn size={16} /> Bác sĩ Đăng nhập
            </button>
            <button
              className={`btn ${authMode === 'register' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
              style={{ flex: 1 }}
              onClick={() => setAuthMode('register')}
            >
              <UserPlus size={16} /> Đăng ký
            </button>
            <button
              className={`btn ${authMode === 'otp' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
              style={{ flex: 1 }}
              onClick={() => setAuthMode('otp')}
            >
              <KeyRound size={16} /> Nhập OTP
            </button>
          </div>

          {authMode === 'login' && (
            <form onSubmit={handleLogin}>
              <h3 className="card-title"><LogIn size={20} /> Cổng đăng nhập Bác sĩ</h3>
              <div className="form-group" style={{ marginBottom: '14px' }}>
                <label>Tên đăng nhập (Username):</label>
                <input
                  type="text"
                  className="form-control"
                  value={authForm.username}
                  onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                  placeholder="Ví dụ: bacsi_an"
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '18px' }}>
                <label>Mật khẩu:</label>
                <input
                  type="password"
                  className="form-control"
                  value={authForm.password}
                  onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                  placeholder="••••••••"
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Đang xác thực...' : 'Đăng nhập Bác sĩ'}
              </button>
            </form>
          )}

          {authMode === 'register' && (
            <form onSubmit={handleRegister}>
              <h3 className="card-title"><UserPlus size={20} /> Đăng ký tài khoản Bác sĩ</h3>
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label>Tên đăng nhập (Username):</label>
                <input
                  type="text"
                  className="form-control"
                  value={authForm.username}
                  onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label>Email bác sĩ:</label>
                <input
                  type="email"
                  className="form-control"
                  value={authForm.email}
                  onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label>Mật khẩu:</label>
                <input
                  type="password"
                  className="form-control"
                  value={authForm.password}
                  onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '18px' }}>
                <label>Xác nhận mật khẩu:</label>
                <input
                  type="password"
                  className="form-control"
                  value={authForm.confirePassword}
                  onChange={(e) => setAuthForm({ ...authForm, confirePassword: e.target.value })}
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Đang gửi...' : 'Đăng ký Bác sĩ'}
              </button>
            </form>
          )}

          {authMode === 'otp' && (
            <form onSubmit={handleVerifyOtp}>
              <h3 className="card-title"><KeyRound size={20} /> Xác thực OTP Bác sĩ</h3>
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label>Email đã đăng ký:</label>
                <input
                  type="email"
                  className="form-control"
                  value={authForm.email}
                  onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '18px' }}>
                <label>Mã OTP (6 chữ số):</label>
                <input
                  type="number"
                  className="form-control"
                  value={authForm.otp}
                  onChange={(e) => setAuthForm({ ...authForm, otp: e.target.value })}
                  placeholder="123456"
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Đang xác thực...' : 'Xác thực OTP'}
              </button>
            </form>
          )}
        </div>
      )}

      {/* ================= TAB 2: QUẢN LÝ SLOT ================= */}
      {subTab === 'slots' && (
        <div>
          {/* Form tạo Slot tự động */}
          <div className="card">
            <h3 className="card-title">
              <PlusCircle size={20} /> Tạo khung giờ khám tự động (/doctor-slots/create)
            </h3>
            <form onSubmit={handleCreateSlots}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Mã Bác sĩ (Doctor ID):</label>
                  <input
                    type="number"
                    className="form-control"
                    value={slotForm.doctorId}
                    onChange={(e) => {
                      setSlotForm({ ...slotForm, doctorId: e.target.value });
                      setDoctorId(e.target.value);
                    }}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Ngày làm việc:</label>
                  <input
                    type="date"
                    className="form-control"
                    value={slotForm.workdate}
                    onChange={(e) => setSlotForm({ ...slotForm, workdate: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Giờ bắt đầu ca:</label>
                  <input
                    type="time"
                    className="form-control"
                    value={slotForm.startShift}
                    onChange={(e) => setSlotForm({ ...slotForm, startShift: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Giờ kết thúc ca:</label>
                  <input
                    type="time"
                    className="form-control"
                    value={slotForm.endShift}
                    onChange={(e) => setSlotForm({ ...slotForm, endShift: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Thời lượng mỗi slot (phút):</label>
                  <input
                    type="number"
                    min="10"
                    step="5"
                    className="form-control"
                    value={slotForm.durationMinutes}
                    onChange={(e) => setSlotForm({ ...slotForm, durationMinutes: e.target.value })}
                    required
                  />
                </div>
              </div>

              <button type="submit" className="btn btn-primary" disabled={loading}>
                <Clock size={16} /> Tạo các Slot khám
              </button>
            </form>
          </div>

          {/* Danh sách Slots hiện có */}
          <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 className="card-title" style={{ margin: 0, border: 'none', padding: 0 }}>
                <Clock size={20} /> Danh sách ca khám đã tạo
              </h3>
              <div style={{ display: 'flex', gap: '8px' }}>
                <input
                  type="number"
                  className="form-control"
                  style={{ width: '120px' }}
                  placeholder="ID Bác sĩ"
                  value={doctorId}
                  onChange={(e) => setDoctorId(e.target.value)}
                />
                <button className="btn btn-outline btn-sm" onClick={loadSlots} disabled={loading}>
                  <RefreshCw size={14} /> Tải lại
                </button>
              </div>
            </div>

            {slots.length === 0 ? (
              <div className="empty-box">
                <Clock size={48} />
                <p>Chưa có slot khám nào được tạo cho bác sĩ này.</p>
              </div>
            ) : (
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Slot ID</th>
                      <th>Ngày</th>
                      <th>Giờ bắt đầu</th>
                      <th>Giờ kết thúc</th>
                      <th>Trạng thái đặt</th>
                      <th>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {slots.map((s) => {
                      const id = s.DoctorSlot_Id || s.doctorSlot_Id || s.id;
                      return (
                        <tr key={id || Math.random()}>
                          <td><strong>#{id}</strong></td>
                          <td>{s.workDate || s.date || '---'}</td>
                          <td>{s.startTime || '---'}</td>
                          <td>{s.endTime || '---'}</td>
                          <td>
                            {s.book ? (
                              <span className="badge badge-warning">Đã được đặt</span>
                            ) : (
                              <span className="badge badge-success">Còn trống</span>
                            )}
                          </td>
                          <td>
                            <button
                              className="btn btn-danger btn-sm"
                              onClick={() => handleDeleteSlot(id)}
                              title="Xóa slot"
                            >
                              <Trash2 size={14} /> Xóa
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ================= TAB 3: LỊCH HẸN BỆNH NHÂN ================= */}
      {subTab === 'appointments' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="card-title" style={{ margin: 0, border: 'none', padding: 0 }}>
              <Calendar size={20} /> Lịch khám của bệnh nhân (/doctor/{'{doctorId}'}/appointments)
            </h3>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input
                type="number"
                className="form-control"
                style={{ width: '130px' }}
                placeholder="Doctor ID"
                value={doctorId}
                onChange={(e) => setDoctorId(e.target.value)}
              />
              <button className="btn btn-outline btn-sm" onClick={loadAppointments} disabled={loading}>
                <RefreshCw size={14} /> Làm mới
              </button>
            </div>
          </div>

          {doctorAppointments.length === 0 ? (
            <div className="empty-box">
              <Calendar size={48} />
              <p>Chưa có lịch hẹn nào của bệnh nhân cho bác sĩ #{doctorId}.</p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Lịch #</th>
                    <th>Bệnh nhân</th>
                    <th>Email / SĐT</th>
                    <th>Ngày hẹn</th>
                    <th>Giờ hẹn</th>
                    <th>Lý do khám</th>
                    <th>Trạng thái</th>
                    <th>Xử lý</th>
                  </tr>
                </thead>
                <tbody>
                  {doctorAppointments.map((app) => (
                    <tr key={app.appoinments_id || Math.random()}>
                      <td><strong>#{app.appoinments_id}</strong></td>
                      <td>{app.patientName || app.user?.username || '---'}</td>
                      <td>{app.email || app.phone || '---'}</td>
                      <td>{app.date || '---'}</td>
                      <td>{app.startTime || '---'}</td>
                      <td>{app.reason || '---'}</td>
                      <td>
                        <span
                          className={`badge ${
                            app.status === 'CONFIRMED'
                              ? 'badge-success'
                              : app.status?.includes('REJECT')
                              ? 'badge-danger'
                              : 'badge-info'
                          }`}
                        >
                          {app.status}
                        </span>
                      </td>
                      <td>
                        <button
                          className="btn btn-warning btn-sm"
                          onClick={() => handleReject(app.appoinments_id)}
                          title="Từ chối và tự động chuyển bác sĩ khác cùng chuyên khoa"
                        >
                          <XCircle size={14} /> Từ chối / Chuyển
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
