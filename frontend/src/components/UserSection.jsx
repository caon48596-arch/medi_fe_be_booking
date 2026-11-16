import React, { useState, useEffect } from 'react';
import {
  Calendar,
  Clock,
  UserPlus,
  LogIn,
  KeyRound,
  CheckCircle,
  AlertCircle,
  RefreshCw,
  PlusCircle,
  UserCheck,
  Edit,
} from 'lucide-react';
import { userApi, appointmentApi } from '../services/api';

const SPECIALTIES = [
  'Nội khoa',
  'Ngoại khoa',
  'Nhi khoa',
  'Tim mạch',
  'Da liễu',
  'Tai mũi họng',
  'Răng hàm mặt',
  'Mắt',
  'Sản phụ khoa',
  'Thần kinh',
];

export default function UserSection({
  currentUser,
  currentRole,
  onLoginSuccess,
}) {
  const [subTab, setSubTab] = useState(currentUser ? 'my-appointments' : 'auth');
  const [authMode, setAuthMode] = useState('login'); // 'login' | 'register' | 'otp'

  // Auth Form State
  const [authForm, setAuthForm] = useState({
    username: '',
    email: '',
    password: '',
    confirePassword: '',
    otp: '',
  });

  // Booking Form State
  const [bookingForm, setBookingForm] = useState({
    username: currentUser || '',
    specialty: 'Nội khoa',
    date: new Date().toISOString().split('T')[0],
    reason: '',
  });

  // Direct Slot Appointment Form State
  const [slotAppForm, setSlotAppForm] = useState({
    patienId: '',
    slotId: '',
    reason: '',
    workDate: new Date().toISOString().split('T')[0],
    starTime: '08:00',
  });

  // Profile Form State
  const [profileId, setProfileId] = useState('');
  const [profileForm, setProfileForm] = useState({
    username: '',
    email: '',
    phone: '',
    address: '',
  });

  // Data & Status State
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null); // { type: 'success' | 'error', text: '' }

  // Cập nhật username form khi currentUser thay đổi
  useEffect(() => {
    if (currentUser) {
      setBookingForm((prev) => ({ ...prev, username: currentUser }));
      loadAppointments();
    }
  }, [currentUser]);

  const showMsg = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 6000);
  };

  // ==================== AUTH HANDLERS ====================
  const handleRegister = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await userApi.register({
        username: authForm.username,
        email: authForm.email,
        password: authForm.password,
        confirePassword: authForm.confirePassword,
      });
      showMsg('success', res.message || 'Đăng ký thành công! Hãy nhập OTP được gửi về email/log.');
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
      const res = await userApi.verifyOtp({
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
      const res = await userApi.login({
        username: authForm.username,
        password: authForm.password,
      });
      showMsg('success', 'Đăng nhập thành công!');
      if (res.data) {
        onLoginSuccess(res.data.username, res.data.roles ? res.data.roles[0] : 'ROLE_USER');
        setSubTab('my-appointments');
      }
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  // ==================== BOOKING HANDLERS ====================
  const handleBooking = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await userApi.bookAppointment(bookingForm);
      showMsg('success', 'Đặt lịch khám thành công! Mã lịch hẹn: ' + (res.data?.appoinments_id || ''));
      loadAppointments();
      setSubTab('my-appointments');
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateAppointmentBySlot = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await appointmentApi.create({
        patienId: Number(slotAppForm.patienId),
        slotId: Number(slotAppForm.slotId),
        reason: slotAppForm.reason,
        workDate: slotAppForm.workDate,
        starTime: slotAppForm.starTime,
      });
      showMsg('success', 'Đặt lịch theo khung giờ bác sĩ thành công!');
      loadAppointments();
      setSubTab('my-appointments');
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadAppointments = async () => {
    if (!currentUser) return;
    setLoading(true);
    try {
      const res = await userApi.getAppointments(currentUser);
      setAppointments(res.data || []);
    } catch (err) {
      showMsg('error', 'Không thể tải lịch hẹn: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleFetchProfile = async (e) => {
    e.preventDefault();
    if (!profileId) return;
    setLoading(true);
    try {
      const res = await userApi.getUser(profileId);
      if (res.data) {
        setProfileForm({
          username: res.data.username || '',
          email: res.data.email || '',
          phone: res.data.phone || '',
          address: res.data.address || '',
        });
        showMsg('success', 'Đã tải thông tin người dùng #' + profileId);
      }
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!profileId) return;
    setLoading(true);
    try {
      await userApi.updateUser(profileId, {
        userId: Number(profileId),
        username: profileForm.username,
        email: profileForm.email,
        phoneNumber: String(profileForm.phone),
        address: profileForm.address,
      });
      showMsg('success', 'Cập nhật thông tin thành công!');
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
        {!currentUser && (
          <button
            className={`sub-tab-item ${subTab === 'auth' ? 'active' : ''}`}
            onClick={() => setSubTab('auth')}
          >
            Đăng nhập / Đăng ký
          </button>
        )}
        <button
          className={`sub-tab-item ${subTab === 'booking' ? 'active' : ''}`}
          onClick={() => setSubTab('booking')}
        >
          Đặt lịch khám nhanh
        </button>
        <button
          className={`sub-tab-item ${subTab === 'slot-app' ? 'active' : ''}`}
          onClick={() => setSubTab('slot-app')}
        >
          Đặt theo Slot Bác sĩ
        </button>
        <button
          className={`sub-tab-item ${subTab === 'my-appointments' ? 'active' : ''}`}
          onClick={() => {
            setSubTab('my-appointments');
            loadAppointments();
          }}
        >
          Lịch hẹn của tôi ({appointments.length})
        </button>
        <button
          className={`sub-tab-item ${subTab === 'profile' ? 'active' : ''}`}
          onClick={() => setSubTab('profile')}
        >
          Thông tin cá nhân
        </button>
      </div>

      {/* ================= TAB 1: AUTH ================= */}
      {subTab === 'auth' && (
        <div className="card" style={{ maxWidth: '480px', margin: '0 auto 24px auto' }}>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
            <button
              className={`btn ${authMode === 'login' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
              style={{ flex: 1 }}
              onClick={() => setAuthMode('login')}
            >
              <LogIn size={16} /> Đăng nhập
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
              <h3 className="card-title"><LogIn size={20} /> Bệnh nhân Đăng nhập</h3>
              <div className="form-group" style={{ marginBottom: '14px' }}>
                <label>Tên đăng nhập (Username):</label>
                <input
                  type="text"
                  className="form-control"
                  value={authForm.username}
                  onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                  placeholder="Ví dụ: nguyenvanan"
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '18px' }}>
                <label>Mật khẩu (Password):</label>
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
                {loading ? 'Đang xác thực...' : 'Đăng nhập ngay'}
              </button>
            </form>
          )}

          {authMode === 'register' && (
            <form onSubmit={handleRegister}>
              <h3 className="card-title"><UserPlus size={20} /> Tạo tài khoản bệnh nhân</h3>
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label>Tên tài khoản (Username):</label>
                <input
                  type="text"
                  className="form-control"
                  value={authForm.username}
                  onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })}
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label>Email liên hệ:</label>
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
                <label>Xác nhận lại mật khẩu:</label>
                <input
                  type="password"
                  className="form-control"
                  value={authForm.confirePassword}
                  onChange={(e) => setAuthForm({ ...authForm, confirePassword: e.target.value })}
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Đang tạo...' : 'Đăng ký tài khoản'}
              </button>
            </form>
          )}

          {authMode === 'otp' && (
            <form onSubmit={handleVerifyOtp}>
              <h3 className="card-title"><KeyRound size={20} /> Xác thực mã OTP</h3>
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
                <small style={{ color: 'var(--gray-500)', fontSize: '0.8rem' }}>
                  Kiểm tra hộp thư email hoặc xem console/log backend nếu SMTP chưa cấu hình.
                </small>
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                {loading ? 'Đang xác thực...' : 'Xác thực OTP'}
              </button>
            </form>
          )}
        </div>
      )}

      {/* ================= TAB 2: ĐẶT LỊCH NHANH ================= */}
      {subTab === 'booking' && (
        <div className="card">
          <h3 className="card-title">
            <Calendar size={20} /> Đặt lịch khám nhanh theo chuyên khoa (/user/booking)
          </h3>
          <form onSubmit={handleBooking}>
            <div className="form-grid">
              <div className="form-group">
                <label>Tên tài khoản bệnh nhân:</label>
                <input
                  type="text"
                  className="form-control"
                  value={bookingForm.username}
                  onChange={(e) => setBookingForm({ ...bookingForm, username: e.target.value })}
                  placeholder="Tên tài khoản của bạn"
                  required
                />
              </div>

              <div className="form-group">
                <label>Chuyên khoa cần khám:</label>
                <select
                  className="form-control"
                  value={bookingForm.specialty}
                  onChange={(e) => setBookingForm({ ...bookingForm, specialty: e.target.value })}
                  required
                >
                  {SPECIALTIES.map((spec) => (
                    <option key={spec} value={spec}>
                      {spec}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Ngày khám dự kiến:</label>
                <input
                  type="date"
                  className="form-control"
                  value={bookingForm.date}
                  onChange={(e) => setBookingForm({ ...bookingForm, date: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label>Lý do khám / Triệu chứng bệnh:</label>
              <textarea
                className="form-control"
                rows="3"
                value={bookingForm.reason}
                onChange={(e) => setBookingForm({ ...bookingForm, reason: e.target.value })}
                placeholder="Mô tả ngắn gọn triệu chứng (ví dụ: đau đầu, sốt nhẹ 2 ngày nay...)"
                required
              ></textarea>
            </div>

            <button type="submit" className="btn btn-primary" disabled={loading}>
              <PlusCircle size={18} />
              <span>{loading ? 'Đang gửi...' : 'Gửi yêu cầu đặt lịch khám'}</span>
            </button>
          </form>
        </div>
      )}

      {/* ================= TAB 3: ĐẶT THEO KHUNG GIỜ BÁC SĨ ================= */}
      {subTab === 'slot-app' && (
        <div className="card">
          <h3 className="card-title">
            <Clock size={20} /> Đặt lịch theo Slot Bác sĩ (/appointment/create)
          </h3>
          <form onSubmit={handleCreateAppointmentBySlot}>
            <div className="form-grid">
              <div className="form-group">
                <label>ID Bệnh nhân (User ID):</label>
                <input
                  type="number"
                  className="form-control"
                  value={slotAppForm.patienId}
                  onChange={(e) => setSlotAppForm({ ...slotAppForm, patienId: e.target.value })}
                  placeholder="Ví dụ: 1"
                  required
                />
              </div>

              <div className="form-group">
                <label>Mã Slot khám (Slot ID):</label>
                <input
                  type="number"
                  className="form-control"
                  value={slotAppForm.slotId}
                  onChange={(e) => setSlotAppForm({ ...slotAppForm, slotId: e.target.value })}
                  placeholder="ID slot từ Bác sĩ"
                  required
                />
              </div>

              <div className="form-group">
                <label>Ngày khám:</label>
                <input
                  type="date"
                  className="form-control"
                  value={slotAppForm.workDate}
                  onChange={(e) => setSlotAppForm({ ...slotAppForm, workDate: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label>Giờ bắt đầu:</label>
                <input
                  type="time"
                  className="form-control"
                  value={slotAppForm.starTime}
                  onChange={(e) => setSlotAppForm({ ...slotAppForm, starTime: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label>Lý do khám:</label>
              <input
                type="text"
                className="form-control"
                value={slotAppForm.reason}
                onChange={(e) => setSlotAppForm({ ...slotAppForm, reason: e.target.value })}
                placeholder="Lý do khám bệnh"
                required
              />
            </div>

            <button type="submit" className="btn btn-primary" disabled={loading}>
              <PlusCircle size={18} />
              <span>{loading ? 'Đang tạo...' : 'Xác nhận đặt lịch slot'}</span>
            </button>
          </form>
        </div>
      )}

      {/* ================= TAB 4: LỊCH HẸN CỦA TÔI ================= */}
      {subTab === 'my-appointments' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 className="card-title" style={{ margin: 0, border: 'none', padding: 0 }}>
              <Calendar size={20} /> Danh sách lịch khám của bạn
            </h3>
            <button className="btn btn-outline btn-sm" onClick={loadAppointments} disabled={loading}>
              <RefreshCw size={14} /> Làm mới
            </button>
          </div>

          {appointments.length === 0 ? (
            <div className="empty-box">
              <Calendar size={48} />
              <p>Bạn chưa có lịch hẹn nào hoặc chưa tải dữ liệu.</p>
              <button
                className="btn btn-primary btn-sm"
                style={{ marginTop: '12px' }}
                onClick={() => setSubTab('booking')}
              >
                Đặt lịch khám ngay
              </button>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Ngày khám</th>
                    <th>Giờ khám</th>
                    <th>Bác sĩ</th>
                    <th>Lý do khám</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {appointments.map((app) => (
                    <tr key={app.appoinments_id || Math.random()}>
                      <td><strong>#{app.appoinments_id}</strong></td>
                      <td>{app.date || '---'}</td>
                      <td>{app.startTime || '---'}</td>
                      <td>
                        {app.doctor?.username || app.doctorUsername || (
                          <span style={{ color: 'var(--gray-400)' }}>Đang phân công</span>
                        )}
                      </td>
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
                          {app.status || 'PENDING'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ================= TAB 5: PROFILE ================= */}
      {subTab === 'profile' && (
        <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
          <h3 className="card-title">
            <UserCheck size={20} /> Tra cứu & Cập nhật thông tin bệnh nhân
          </h3>

          <form onSubmit={handleFetchProfile} style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
            <input
              type="number"
              className="form-control"
              placeholder="Nhập ID Người dùng (ví dụ: 1)"
              value={profileId}
              onChange={(e) => setProfileId(e.target.value)}
              required
            />
            <button type="submit" className="btn btn-secondary btn-sm" disabled={loading}>
              Tra cứu
            </button>
          </form>

          <form onSubmit={handleUpdateProfile}>
            <div className="form-group" style={{ marginBottom: '12px' }}>
              <label>Tên đăng nhập:</label>
              <input
                type="text"
                className="form-control"
                value={profileForm.username}
                onChange={(e) => setProfileForm({ ...profileForm, username: e.target.value })}
                required
              />
            </div>

            <div className="form-group" style={{ marginBottom: '12px' }}>
              <label>Email:</label>
              <input
                type="email"
                className="form-control"
                value={profileForm.email}
                onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                required
              />
            </div>

            <div className="form-group" style={{ marginBottom: '12px' }}>
              <label>Số điện thoại:</label>
              <input
                type="text"
                className="form-control"
                value={profileForm.phone}
                onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                placeholder="0987654321"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label>Địa chỉ:</label>
              <input
                type="text"
                className="form-control"
                value={profileForm.address}
                onChange={(e) => setProfileForm({ ...profileForm, address: e.target.value })}
                placeholder="Địa chỉ cư trú"
              />
            </div>

            <button type="submit" className="btn btn-primary" disabled={loading || !profileId}>
              <Edit size={16} /> Lưu cập nhật
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
