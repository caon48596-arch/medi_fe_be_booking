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

  // Countdown timer for session hold (09:38)
  const [secondsLeft, setSecondsLeft] = useState(578); // 9m 38s

  useEffect(() => {
    const interval = setInterval(() => {
      setSecondsLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  const formatTimer = (totalSeconds) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

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
    <div className="medisession-app">
      {/* Fixed Topbar & Left Sidebar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        currentUser={currentUser}
        currentRole={currentRole}
        onLogout={handleLogout}
      />

      {/* Main Content Area */}
      <div className="main-wrapper">
        {/* Session Top Alert Timer (Brand System Spec) */}
        <div className="session-timer-bar">
          <div className="session-timer-left">
            <span
              className="material-symbols-outlined"
              style={{ color: 'var(--tertiary)', fontSize: '20px' }}
            >
              timer
            </span>
            <div>
              <span className="timer-tag">Phiên đăng ký trực tuyến: </span>
              <span>Hệ thống đang giữ chỗ tạm thời trong </span>
              <strong className="timer-countdown">{formatTimer(secondsLeft)}</strong>
              <span> phút.</span>
            </div>
          </div>
          <div className="session-sync-right">
            <span className="sync-dot"></span>
            <span>Đồng bộ thời gian thực với phòng tiếp nhận</span>
          </div>
        </div>

        {/* Tab 1: Cổng Bệnh nhân (Exact design as screen.png) */}
        {activeTab === 'user' && (
          <UserSection
            currentUser={currentUser}
            currentRole={currentRole}
            onLoginSuccess={handleLoginSuccess}
          />
        )}

        {/* Tab 2: Cổng Bác sĩ (Doctor Portal) */}
        {activeTab === 'doctor' && (
          <DoctorSection
            currentUser={currentUser}
            currentRole={currentRole}
            onLoginSuccess={handleLoginSuccess}
          />
        )}

        {/* Tab 3: Cổng Hồ sơ bệnh án & Lịch khám */}
        {activeTab === 'records' && (
          <MedicalRecordsSection
            currentUser={currentUser}
          />
        )}
      </div>
    </div>
  );
}
