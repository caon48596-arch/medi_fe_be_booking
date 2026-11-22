import axios from 'axios';

// Lấy URL cấu hình từ localStorage hoặc biến môi trường hoặc mặc định relative URL '' để đi qua Vite dev proxy
export const getBaseUrl = () => {
  const custom = localStorage.getItem('EMEDICAL_API_URL');
  if (custom && custom.trim()) return custom.trim();
  if (import.meta.env.VITE_API_URL) return import.meta.env.VITE_API_URL;
  // Mặc định '' giúp frontend gọi cùng origin (Vite dev proxy tự forward sang port 8080)
  // Loại bỏ hoàn toàn lỗi Network Error do CORS hoặc sai khác port/IP
  return '';
};

export const setBaseUrl = (url) => {
  if (!url || !url.trim()) {
    localStorage.removeItem('EMEDICAL_API_URL');
  } else {
    localStorage.setItem('EMEDICAL_API_URL', url.trim().replace(/\/$/, ''));
  }
};

const createClient = () => {
  const instance = axios.create({
    baseURL: getBaseUrl(),
    withCredentials: true, // Cho phép gửi và lưu Cookie Session (JSESSIONID) với Redis Session
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // Interceptor tự động bắt lỗi trả về thông điệp rõ ràng
  instance.interceptors.response.use(
    (response) => response.data,
    (error) => {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        (error.message === 'Network Error'
          ? 'Không thể kết nối đến máy chủ Backend (Port 8080). Vui lòng đảm bảo Backend Spring Boot đã được khởi động.'
          : error.message) ||
        'Lỗi kết nối tới máy chủ';
      return Promise.reject(new Error(message));
    }
  );

  return instance;
};

// ==================== USER APIs ====================
export const userApi = {
  // Đăng ký bệnh nhân nội bộ (không yêu cầu OTP khi đăng ký)
  register: (data) => createClient().post('/user/register', data),
  // Đăng nhập bệnh nhân (bước 1: gửi user/pass để nhận OTP; bước 2: gửi kèm otp để hoàn tất)
  login: (data) => createClient().post('/user/login', data),
  // Xác nhận OTP đăng nhập bệnh nhân
  verifyLoginOtp: (data) => createClient().post('/user/verify-login-otp', {
    username: data.username,
    email: data.email,
    otp: Number(data.otp),
  }),
  verifyOtp: (data) => createClient().post('/user/verify-otp', {
    email: data.email,
    username: data.username,
    otp: Number(data.otp),
  }),
  logout: (username) => createClient().post('/user/logout', { username }),
  getAppointments: (username, id) => {
    const params = {};
    if (username) params.username = username;
    if (id) params.id = id;
    return createClient().get('/user/get', { params });
  },
  bookAppointment: (data) => createClient().post('/user/booking', data),
  getUser: (id) => createClient().get(`/user/getuser/${id}`),
  getProfile: (username) => createClient().get('/user/profile', { params: username ? { username } : {} }),
  getAllUsers: () => createClient().get('/user/all'),
  updateUser: (id, data) => createClient().put(`/user/updateUser/${id}`, data),
  deleteUser: (id) => createClient().delete(`/user/deleteUser/${id}`),
};

// ==================== DOCTOR APIs ====================
export const doctorApi = {
  // Đăng ký bác sĩ nội bộ (không yêu cầu OTP khi đăng ký)
  register: (data) => createClient().post('/doctor/register', data),
  // Đăng nhập bác sĩ (bước 1: gửi user/pass để nhận OTP; bước 2: gửi kèm otp để hoàn tất)
  login: (data) => createClient().post('/doctor/login', data),
  // Xác nhận OTP đăng nhập bác sĩ
  verifyLoginOtp: (data) => createClient().post('/doctor/verify-login-otp', {
    username: data.username,
    email: data.email,
    otp: Number(data.otp),
  }),
  verifyOtp: (data) => createClient().post('/doctor/verify-otp', {
    email: data.email,
    username: data.username,
    otp: Number(data.otp),
  }),
  logout: (username) => createClient().post('/doctor/logout', { username }),
  getAll: () => createClient().get('/doctor/list'),
  getSlots: (username) => createClient().get(`/doctor/${encodeURIComponent(username)}/slots`),
  getAppointments: (doctorId) => createClient().get(`/doctor/${doctorId}/appointments`),
  rejectAppointment: (doctorId, appointmentId) =>
    createClient().post(`/doctor/${doctorId}/appointments/${appointmentId}/reject`),
};

// ==================== DOCTOR SLOTS APIs ====================
export const slotApi = {
  create: (data) => createClient().post('/doctor-slots/create', data),
  read: (doctorId = null) => createClient().get('/doctor-slots/read', { params: doctorId ? { doctorId } : {} }),
  update: (data) => createClient().put('/doctor-slots/update', data),
  delete: (doctorSlotId) => createClient().delete(`/doctor-slots/delete/${doctorSlotId}`),
};

// ==================== APPOINTMENT APIs ====================
export const appointmentApi = {
  create: (data) => createClient().post('/appointment/create', data),
  read: (patienId = null) => createClient().get('/appointment/read', { params: patienId ? { patientId: patienId } : {} }),
  update: (data) => createClient().put('/appointment/update', data),
  cancel: (id) => createClient().post(`/appointment/cancel/${id}`),
  delete: (id) => createClient().delete(`/appointment/delete/${id}`),
};

// ==================== MEDICAL RECORDS APIs ====================
export const medicalRecordsApi = {
  create: (data) => createClient().post('/medical-records/create', data),
  read: (medicalRecordId = null) =>
    createClient().get('/medical-records/read', { params: medicalRecordId ? { medicalRecordId } : {} }),
  update: (data) => createClient().put('/medical-records/update', data),
  delete: (medicalRecordId) =>
    createClient().delete(`/medical-records/delete/${medicalRecordId}`),
};
