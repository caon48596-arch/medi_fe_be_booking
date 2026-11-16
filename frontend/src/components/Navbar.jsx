import React, { useState } from 'react';
import {
  Activity,
  User,
  Stethoscope,
  FileText,
  Settings,
  LogOut,
  CheckCircle,
} from 'lucide-react';
import { getBaseUrl, setBaseUrl } from '../services/api';

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
      <header className="navbar">
        <div className="navbar-container">
          {/* Logo */}
          <div className="logo">
            <Activity size={28} />
            <span>EMedical</span>
            <span className="logo-badge">v1.0</span>
          </div>

          {/* Navigation Tabs */}
          <nav className="nav-tabs">
            <button
              className={`nav-tab-btn ${activeTab === 'user' ? 'active' : ''}`}
              onClick={() => setActiveTab('user')}
            >
              <User size={18} />
              <span>Bệnh nhân</span>
            </button>

            <button
              className={`nav-tab-btn ${activeTab === 'doctor' ? 'active' : ''}`}
              onClick={() => setActiveTab('doctor')}
            >
              <Stethoscope size={18} />
              <span>Bác sĩ</span>
            </button>

            <button
              className={`nav-tab-btn ${activeTab === 'records' ? 'active' : ''}`}
              onClick={() => setActiveTab('records')}
            >
              <FileText size={18} />
              <span>Hồ sơ bệnh án</span>
            </button>
          </nav>

          {/* User state & Settings */}
          <div className="nav-user-info">
            {currentUser ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span className="badge badge-info">
                  {currentRole === 'ROLE_DOCTOR' ? 'Bác sĩ' : 'Bệnh nhân'}: {currentUser}
                </span>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={onLogout}
                  title="Đăng xuất"
                >
                  <LogOut size={16} />
                  <span>Đăng xuất</span>
                </button>
              </div>
            ) : (
              <span className="badge badge-warning">Chưa đăng nhập</span>
            )}

            <button
              className="btn btn-outline btn-sm"
              onClick={() => setShowConfig(true)}
              title="Cấu hình Backend API"
            >
              <Settings size={16} />
            </button>
          </div>
        </div>
      </header>

      {/* Backend API Configuration Modal */}
      {showConfig && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <span>Cấu hình kết nối Backend</span>
              <button
                className="btn btn-sm btn-outline"
                onClick={() => setShowConfig(false)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleSaveConfig}>
              <div className="form-group" style={{ marginBottom: '16px' }}>
                <label>Backend API Base URL:</label>
                <input
                  type="text"
                  className="form-control"
                  value={apiUrl}
                  onChange={(e) => setApiUrl(e.target.value)}
                  placeholder="http://localhost:8080"
                  required
                />
                <small style={{ color: 'var(--gray-500)', fontSize: '0.8rem', marginTop: '4px' }}>
                  Hỗ trợ cả môi trường Local (http://localhost:8080) và Production Deploy.
                </small>
              </div>

              {savedSuccess && (
                <div className="alert alert-success">
                  <CheckCircle size={16} />
                  <span>Đã lưu! Trang đang tải lại...</span>
                </div>
              )}

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowConfig(false)}
                >
                  Đóng
                </button>
                <button type="submit" className="btn btn-primary btn-sm">
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
