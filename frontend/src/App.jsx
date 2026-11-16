import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import UserSection from './components/UserSection';
import DoctorSection from './components/DoctorSection';
import MedicalRecordsSection from './components/MedicalRecordsSection';
import { userApi, doctorApi } from './services/api';

export default function App() {
  const [activeTab, setActiveTab] = useState('user'); // 'user' | 'doctor' | 'records'
  const [currentUser, setCurrentUser] = useState(() => localStorage.getItem('EMEDICAL_USER') || null);
  const [currentRole, setCurrentRole] = useState(() => localStorage.getItem('EMEDICAL_ROLE') || null);

  const handleLoginSuccess = (username, role) => {
    setCurrentUser(username);
    setCurrentRole(role);
    localStorage.setItem('EMEDICAL_USER', username);
    localStorage.setItem('EMEDICAL_ROLE', role);
  };

  const handleLogout = async () => {
    try {
      if (currentRole === 'ROLE_DOCTOR') {
        await doctorApi.logout(currentUser);
      } else {
        await userApi.logout(currentUser);
      }
    } catch (e) {
      console.warn('Logout server error (ignored for client logout):', e);
    } finally {
      setCurrentUser(null);
      setCurrentRole(null);
      localStorage.removeItem('EMEDICAL_USER');
      localStorage.removeItem('EMEDICAL_ROLE');
    }
  };

  return (
    <div className="app-container">
      {/* Navigation Bar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        currentUser={currentUser}
        currentRole={currentRole}
        onLogout={handleLogout}
      />

      {/* Main Content Area */}
      <main className="main-content">
        {/* Banner giới thiệu ngắn gọn */}
        <div style={{ marginBottom: '24px' }}>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--gray-900)' }}>
            Hệ thống Đặt lịch Khám bệnh Y tế (E-Medical Booking)
          </h1>
          <p style={{ color: 'var(--gray-600)', fontSize: '0.95rem', marginTop: '4px' }}>
            Giao diện kết nối trực tiếp với Spring Boot Backend (PostgreSQL, Redis Session, Spring Security).
          </p>
        </div>

        {/* Tab 1: Cổng Bệnh nhân */}
        {activeTab === 'user' && (
          <UserSection
            currentUser={currentUser}
            currentRole={currentRole}
            onLoginSuccess={handleLoginSuccess}
          />
        )}

        {/* Tab 2: Cổng Bác sĩ */}
        {activeTab === 'doctor' && (
          <DoctorSection
            currentUser={currentUser}
            currentRole={currentRole}
            onLoginSuccess={handleLoginSuccess}
          />
        )}

        {/* Tab 3: Cổng Hồ sơ bệnh án */}
        {activeTab === 'records' && (
          <MedicalRecordsSection
            currentUser={currentUser}
          />
        )}
      </main>

      {/* Footer */}
      <footer style={{ borderTop: '1px solid var(--gray-200)', background: '#ffffff', padding: '16px', textAlign: 'center', fontSize: '0.85rem', color: 'var(--gray-500)' }}>
        <p>E-Medical Booking Frontend • Sẵn sàng triển khai độc lập (Vite / Vercel / Nginx) hoặc nhúng cùng Spring Boot.</p>
      </footer>
    </div>
  );
}
