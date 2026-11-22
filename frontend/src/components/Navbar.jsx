import React, { useState } from 'react';
import { getBaseUrl, setBaseUrl } from '../services/api';

const HOSPITAL_NAME = 'Bệnh Viện Đa Khoa MediSession';

export default function Navbar({
  activeTab,
  setActiveTab,
  currentUser,
  currentRole,
  onLogout,
}) {
  const [showConfig, setShowConfig] = useState(false);
  const [apiUrl, setApiUrl] = useState(getBaseUrl());
  const [savedSuccess, setSavedSuccess] = useState(false);
  const [showUserDropdown, setShowUserDropdown] = useState(false);

  const handleSaveConfig = (e) => {
    e.preventDefault();
    setBaseUrl(apiUrl);
    setSavedSuccess(true);
    setTimeout(() => {
      setSavedSuccess(false);
      setShowConfig(false);
      window.location.reload();
    }, 800);
  };

  return (
    <>
      {/* ==================== FIXED TOPBAR ==================== */}
      <header className="topbar">
        <div className="topbar-content">
          {/* Brand Logo & Top Navigation */}
          <div className="topbar-left">
            <div className="brand-logo" onClick={() => setActiveTab('user')}>
              <div className="brand-icon-box">
                <span className="material-symbols-outlined" style={{ fontSize: '22px' }}>
                  local_hospital
                </span>
              </div>
              <div className="brand-text">
                <span className="brand-title">MediSession</span>
                <span className="brand-subtitle">{HOSPITAL_NAME} • Cổng Nội Bộ</span>
              </div>
            </div>

            <div className="topbar-divider"></div>

            {/* Horizontal Nav Links */}
            <nav className="topbar-nav">
              <button
                type="button"
                className={`topbar-nav-link ${activeTab === 'doctor' ? 'active' : ''}`}
                onClick={() => setActiveTab('doctor')}
              >
                Cổng Bác Sĩ (Doctor Portal)
              </button>
              <button
                type="button"
                className={`topbar-nav-link ${activeTab === 'user' ? 'active' : ''}`}
                onClick={() => setActiveTab('user')}
              >
                Cổng Bệnh Nhân (Patient Portal)
              </button>
              <button
                type="button"
                className={`topbar-nav-link ${activeTab === 'records' ? 'active' : ''}`}
                onClick={() => setActiveTab('records')}
              >
                Lịch Khám & Bệnh Án
              </button>
            </nav>
          </div>

          {/* Right Controls & User Info */}
          <div className="topbar-right">
            {/* Hospital Selector Pill */}
            <div className="hospital-pill" title={`Hệ thống nội bộ ${HOSPITAL_NAME}`}>
              <span className="material-symbols-outlined" style={{ fontSize: '18px', color: 'var(--primary)' }}>
                domain
              </span>
              <span className="hospital-pill-text">{HOSPITAL_NAME} (Nội Bộ)</span>
            </div>

            {/* Notification & Settings Buttons */}
            <div className="topbar-actions">
              <button
                type="button"
                className="icon-btn"
                title="Thông báo hệ thống"
                onClick={() => alert(`Thông báo: Cổng tiếp nhận nội bộ ${HOSPITAL_NAME} đang hoạt động bình thường.`)}
              >
                <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
                  notifications
                </span>
                <span className="icon-dot"></span>
              </button>

              <button
                type="button"
                className="icon-btn"
                onClick={() => setShowConfig(true)}
                title="Cấu hình Backend API"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
                  settings
                </span>
              </button>
            </div>

            <div className="topbar-divider"></div>

            {/* User Profile Info */}
            <div
              className="user-profile-badge"
              onClick={() => setShowUserDropdown(!showUserDropdown)}
              title={currentUser ? `Đang đăng nhập: ${currentUser}` : 'Chưa đăng nhập'}
            >
              <div className="user-profile-info">
                <span className="user-profile-name">
                  {currentUser
                    ? currentRole === 'ROLE_DOCTOR'
                      ? `BS. ${currentUser}`
                      : currentUser
                    : 'Chưa đăng nhập'}
                </span>
                <span className="user-profile-status">
                  <span className={currentUser ? 'status-dot-active' : ''} style={!currentUser ? { width: '6px', height: '6px', borderRadius: '50%', background: 'var(--outline)', display: 'inline-block' } : {}}></span>
                  {currentUser ? (currentRole === 'ROLE_DOCTOR' ? 'Bác sĩ nội bộ' : 'Bệnh nhân') : 'Khách vãng lai'}
                </span>
              </div>
              <div className="user-avatar-circle">
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                  person
                </span>
              </div>
            </div>

            {/* Quick Logout button if logged in */}
            {currentUser && (
              <button
                type="button"
                className="btn btn-sm btn-secondary"
                style={{ padding: '0 10px', height: '32px' }}
                onClick={onLogout}
                title="Đăng xuất khỏi hệ thống"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                  logout
                </span>
                <span>Thoát</span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* ==================== FIXED LEFT ASIDE (SIDEBAR) ==================== */}
      <aside className="sidebar">
        <div className="sidebar-menu">
          <div className="sidebar-heading">Điều hành phiên khám nội bộ</div>

          <button
            type="button"
            className={`sidebar-nav-item ${activeTab === 'doctor' ? 'active' : ''}`}
            onClick={() => setActiveTab('doctor')}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
              stethoscope
            </span>
            <span>Bác sĩ điều trị</span>
          </button>

          <button
            type="button"
            className={`sidebar-nav-item ${activeTab === 'user' ? 'active' : ''}`}
            onClick={() => setActiveTab('user')}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
              personal_injury
            </span>
            <span>Hồ sơ bệnh nhân</span>
          </button>

          <button
            type="button"
            className={`sidebar-nav-item ${activeTab === 'records' ? 'active' : ''}`}
            onClick={() => setActiveTab('records')}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
              calendar_month
            </span>
            <span>Ca trực & Slot hẹn</span>
          </button>

          <button
            type="button"
            className="sidebar-nav-item"
            onClick={() => {
              setActiveTab('records');
            }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>
              hourglass_top
            </span>
            <span>Điều phối hàng đợi</span>
          </button>
        </div>

        {/* Sidebar Bottom Capacity Progress */}
        <div className="sidebar-bottom-card">
          <div className="sidebar-progress-header">
            <span>Tải phòng khám hôm nay</span>
            <strong style={{ color: 'var(--primary)' }}>78%</strong>
          </div>
          <div className="sidebar-progress-track">
            <div className="sidebar-progress-fill" style={{ width: '78%' }}></div>
          </div>
        </div>
      </aside>

      {/* ==================== BACKEND CONFIG MODAL ==================== */}
      {showConfig && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <div className="modal-title">
                <span className="material-symbols-outlined" style={{ color: 'var(--primary)' }}>
                  settings
                </span>
                <span>Cấu hình Kết Nối Backend Spring Boot</span>
              </div>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setShowConfig(false)}
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form onSubmit={handleSaveConfig} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label">Backend API Base URL:</label>
                <input
                  type="text"
                  className="form-input"
                  value={apiUrl}
                  onChange={(e) => setApiUrl(e.target.value)}
                  placeholder="http://localhost:8080"
                  required
                />
                <span style={{ fontSize: '11.5px', color: 'var(--outline)', marginTop: '4px' }}>
                  Hỗ trợ cả môi trường Local (http://localhost:8080) và Production Cloud Deploy.
                </span>
              </div>

              {savedSuccess && (
                <div className="alert alert-success">
                  <span className="material-symbols-outlined">check_circle</span>
                  <span>Đã lưu! Ứng dụng đang tải lại...</span>
                </div>
              )}

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowConfig(false)}
                >
                  Đóng
                </button>
                <button type="submit" className="btn btn-primary">
                  Lưu & Áp dụng
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
