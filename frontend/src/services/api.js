import axios from 'axios';

// Lấy URL cấu hình từ localStorage hoặc biến môi trường hoặc mặc định localhost:8080
export const getBaseUrl = () => {
  return localStorage.getItem('EMEDICAL_API_URL') || import.meta.env.VITE_API_URL || 'http://localhost:8080';
};

export const setBaseUrl = (url) => {
  localStorage.setItem('EMEDICAL_API_URL', url.replace(/\/$/, ''));
};

const createClient = () => {
  const instance = axios.create({
    baseURL: getBaseUrl(),
    withCredentials: true, // Quan trọng: Cho phép gửi và lưu Cookie Session (JSESSIONID) với Redis Session
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
        error.message ||
        'Lỗi kết nối tới máy chủ';
      return Promise.reject(new Error(message));
    }
  );

  return instance;
};

// ==================== USER APIs ====================
export const userApi = {
  register: (data) => createClient().post('/user/register', data),
  verifyOtp: (data) => createClient().post('/user/verify-otp', {
    email: data.email,
    otp: Number(data.otp),
  }),
  login: (data) => createClient().post('/user/login', data),
  logout: (username) => createClient().post('/user/logout', { username }),
  getAppointments: (username, id) => {
    const params = {};
    if (username) params.username = username;
    if (id) params.id = id;
    return createClient().get('/user/get', { params });
  },
  bookAppointment: (data) => createClient().post('/user/booking', data),
  getUser: (id) => createClient().get(`/user/getuser/${id}`),
  updateUser: (id, data) => createClient().put(`/user/updateUser/${id}`, data),
  deleteUser: (id) => createClient().delete(`/user/deleteUser/${id}`),
};

// ==================== DOCTOR APIs ====================
export const doctorApi = {
  register: (data) => createClient().post('/doctor/register', data),
  verifyOtp: (data) => createClient().post('/doctor/verify-otp', {
    email: data.email,
    otp: Number(data.otp),
  }),
  login: (data) => createClient().post('/doctor/login', data),
  logout: (username) => createClient().post('/doctor/logout', { username }),
  getSlots: (username) => createClient().get(`/doctor/${encodeURIComponent(username)}/slots`),
  getAppointments: (doctorId) => createClient().get(`/doctor/${doctorId}/appointments`),
  rejectAppointment: (doctorId, appointmentId) =>
    createClient().post(`/doctor/${doctorId}/appointments/${appointmentId}/reject`),
};

// ==================== DOCTOR SLOTS APIs ====================
export const slotApi = {
  create: (data) => createClient().post('/doctor-slots/create', data),
  read: (doctorId) => createClient().post('/doctor-slots/read', { doctorId }),
  update: (data) => createClient().put('/doctor-slots/update', data),
  delete: (doctorSlotId) => createClient().delete('/doctor-slots/delete', { data: { doctorSlotId } }),
};

// ==================== APPOINTMENT APIs ====================
export const appointmentApi = {
  create: (data) => createClient().post('/appointment/create', data),
  read: (patienId) => createClient().post('/appointment/read', { patienId }),
  update: (data) => createClient().put('/appointment/update', data),
};

// ==================== MEDICAL RECORDS APIs ====================
export const medicalRecordsApi = {
  create: (data) => createClient().post('/medical-records/create', data),
  read: (medicalRecordId = null) =>
    createClient().post('/medical-records/read', medicalRecordId ? { medical_recordId: medicalRecordId } : {}),
  update: (data) => createClient().put('/medical-records/update', data),
  delete: (medicalRecordId) =>
    createClient().delete('/medical-records/delete', { data: { medical_recordId: medicalRecordId } }),
};
