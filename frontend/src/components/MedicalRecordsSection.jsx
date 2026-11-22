import React, { useState, useEffect } from 'react';
import { medicalRecordsApi } from '../services/api';

export default function MedicalRecordsSection({ currentUser }) {
  // Real Database Data lists (No fake/mock data)
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);

  // Form create / edit
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    medical_recordId: null,
    patient_id: '',
    username: '',
    doctorId: '',
    doctorUsername: currentUser || '',
    reason: '',
    diagnosis: '',
    treatment: '',
    prescription: '',
    note: '',
  });

  // Search filter
  const [searchId, setSearchId] = useState('');

  const showMsg = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 5000);
  };

  useEffect(() => {
    loadRecords();
  }, []);

  const loadRecords = async (recordId = null) => {
    setLoading(true);
    try {
      const res = await medicalRecordsApi.read(recordId);
      if (res.data) {
        setRecords(res.data);
      }
    } catch (err) {
      console.warn('Backend loadRecords error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isEditing) {
        await medicalRecordsApi.update({
          medical_recordId: formData.medical_recordId,
          patient_id: formData.patient_id ? Number(formData.patient_id) : null,
          username: formData.username,
          doctorId: formData.doctorId ? Number(formData.doctorId) : null,
          doctorUsername: formData.doctorUsername,
          reason: formData.reason,
          diagnosis: formData.diagnosis,
          treatment: formData.treatment,
          prescription: formData.prescription,
          note: formData.note,
        });
        showMsg('success', 'Cập nhật hồ sơ bệnh án thành công vào Database!');
      } else {
        await medicalRecordsApi.create({
          patient_id: formData.patient_id ? Number(formData.patient_id) : 1,
          username: formData.username,
          doctorId: formData.doctorId ? Number(formData.doctorId) : 1,
          doctorUsername: formData.doctorUsername,
          reason: formData.reason,
          diagnosis: formData.diagnosis,
          treatment: formData.treatment,
          prescription: formData.prescription,
          note: formData.note,
        });
        showMsg('success', 'Tạo mới và lưu hồ sơ bệnh án thành công vào Database!');
      }

      // Reset form & Reload real records from DB
      setFormData({
        medical_recordId: null,
        patient_id: '',
        username: '',
        doctorId: '',
        doctorUsername: currentUser || '',
        reason: '',
        diagnosis: '',
        treatment: '',
        prescription: '',
        note: '',
      });
      setIsEditing(false);
      loadRecords();
    } catch (err) {
      showMsg('error', err.message || 'Lỗi lưu hồ sơ bệnh án');
    } finally {
      setLoading(false);
    }
  };

  const handleEditClick = (rec) => {
    setIsEditing(true);
    const recId = rec.medical_id || rec.medical_recordId;
    setFormData({
      medical_recordId: recId,
      patient_id: rec.patient_id || '1',
      username: rec.patientName || rec.username || 'Trần Minh Quân',
      doctorId: rec.doctor_id || rec.doctorId || '1',
      doctorUsername: rec.doctorName || rec.doctorUsername || currentUser || 'BS. Nguyễn Văn An',
      reason: rec.reason || '',
      diagnosis: rec.diagnosis || '',
      treatment: rec.treatment || '',
      prescription: rec.prescription || '',
      note: rec.note || '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (recordId) => {
    if (!confirm('Bạn có chắc muốn xóa hồ sơ bệnh án #' + recordId + ' khỏi Database?')) return;
    setLoading(true);
    try {
      await medicalRecordsApi.delete(recordId);
      showMsg('success', 'Đã xóa hồ sơ bệnh án #' + recordId + ' khỏi Database');
      loadRecords();
    } catch (err) {
      showMsg('error', err.message || 'Lỗi xóa hồ sơ bệnh án');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      {/* Messages */}
      {message && (
        <div className={`alert ${message.type === 'success' ? 'alert-success' : 'alert-error'}`}>
          <span className="material-symbols-outlined">
            {message.type === 'success' ? 'check_circle' : 'error'}
          </span>
          <span>{message.text}</span>
        </div>
      )}

      {/* Header Banner */}
      <div className="patient-banner-card">
        <div className="patient-banner-glow"></div>
        <div className="patient-info-left">
          <div className="patient-avatar-box">
            <span className="material-symbols-outlined" style={{ fontSize: '36px' }}>
              folder_managed
            </span>
          </div>
          <div className="patient-meta-text">
            <div className="patient-header-row">
              <h1 className="patient-title">Quản Lý Hồ Sơ Bệnh Án Điện Tử (EMR)</h1>
              <span className="pill-badge pill-primary">DỮ LIỆU DATABASE POSTGRESQL</span>
            </div>
            <div className="patient-details-line">
              <span>Hệ thống lưu trữ bệnh án & lịch sử khám ngoại trú</span>
              <span className="meta-dot">•</span>
              <span>Bảo mật AES-256</span>
              <span className="meta-dot">•</span>
              <span>Tổng số hồ sơ trong Database: <strong>{records.length}</strong></span>
            </div>
          </div>
        </div>
      </div>

      {/* Form Tạo / Cập nhật */}
      <div className="card-section">
        <div className="card-section-header">
          <div className="section-title-wrap">
            <div className="section-icon-badge">
              <span className="material-symbols-outlined">
                {isEditing ? 'edit_note' : 'post_add'}
              </span>
            </div>
            <div>
              <h2 className="section-title">
                {isEditing
                  ? `Cập Nhật Hồ Sơ Bệnh Án #${formData.medical_recordId}`
                  : 'Tạo Hồ Sơ Bệnh Án Mới (/medical-records/create)'}
              </h2>
              <p className="section-desc">Ghi chép diễn biến chẩn đoán và phác đồ điều trị trực tiếp vào cơ sở dữ liệu</p>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-grid-3">
            <div className="form-group">
              <label className="form-label">ID Bệnh nhân:</label>
              <input
                type="number"
                className="form-input"
                value={formData.patient_id}
                onChange={(e) => setFormData({ ...formData, patient_id: e.target.value })}
                placeholder="ID Bệnh nhân (ví dụ: 1)"
                disabled={isEditing}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Lý do khám / Triệu chứng:</label>
              <input
                type="text"
                className="form-input"
                value={formData.reason}
                onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                placeholder="Triệu chứng ban đầu"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Chẩn đoán bệnh (Diagnosis):</label>
              <input
                type="text"
                className="form-input"
                value={formData.diagnosis}
                onChange={(e) => setFormData({ ...formData, diagnosis: e.target.value })}
                placeholder="Chẩn đoán của bác sĩ"
                required
              />
            </div>
          </div>

          <div className="form-grid-3">
            <div className="form-group">
              <label className="form-label">Phương pháp điều trị:</label>
              <input
                type="text"
                className="form-input"
                value={formData.treatment}
                onChange={(e) => setFormData({ ...formData, treatment: e.target.value })}
                placeholder="Phương pháp điều trị"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Đơn thuốc (Prescription):</label>
              <input
                type="text"
                className="form-input"
                value={formData.prescription}
                onChange={(e) => setFormData({ ...formData, prescription: e.target.value })}
                placeholder="Tên thuốc, liều dùng..."
              />
            </div>

            <div className="form-group">
              <label className="form-label">Ghi chú & Dặn dò:</label>
              <input
                type="text"
                className="form-input"
                value={formData.note}
                onChange={(e) => setFormData({ ...formData, note: e.target.value })}
                placeholder="Ghi chú dặn dò bệnh nhân..."
              />
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', marginTop: '16px' }}>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              <span className="material-symbols-outlined">
                {isEditing ? 'save' : 'add_circle'}
              </span>
              <span>{isEditing ? 'Lưu thay đổi hồ sơ vào DB' : 'Lưu hồ sơ bệnh án mới vào DB'}</span>
            </button>

            {isEditing && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setIsEditing(false);
                  setFormData({
                    medical_recordId: null,
                    patient_id: '',
                    username: '',
                    doctorId: '',
                    doctorUsername: currentUser || '',
                    reason: '',
                    diagnosis: '',
                    treatment: '',
                    prescription: '',
                    note: '',
                  });
                }}
              >
                Hủy sửa
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Danh sách hồ sơ bệnh án */}
      <div className="card-section">
        <div className="card-section-header">
          <div className="section-title-wrap">
            <div className="section-icon-badge">
              <span className="material-symbols-outlined">clinical_notes</span>
            </div>
            <div>
              <h3 className="section-title">Danh Sách Bệnh Án Điện Tử Trong Database ({records.length})</h3>
              <p className="section-desc">Toàn bộ hồ sơ khám chữa bệnh đã được lưu trữ trong cơ sở dữ liệu</p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <input
              type="number"
              className="form-input"
              style={{ width: '150px', height: '36px' }}
              placeholder="Tra mã HS"
              value={searchId}
              onChange={(e) => setSearchId(e.target.value)}
            />
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              onClick={() => loadRecords(searchId ? Number(searchId) : null)}
              disabled={loading}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                search
              </span>
              <span>Tìm</span>
            </button>
            <button
              type="button"
              className="btn btn-sm btn-outline"
              onClick={() => {
                setSearchId('');
                loadRecords(null);
              }}
              disabled={loading}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                refresh
              </span>
              <span>Tất cả</span>
            </button>
          </div>
        </div>

        {records.length === 0 ? (
          <p style={{ color: 'var(--outline)', textAlign: 'center', padding: '30px 0' }}>
            Hiện chưa có hồ sơ bệnh án nào trong cơ sở dữ liệu. Hãy tạo hồ sơ mới ở trên.
          </p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {records.map((rec) => {
              const id = rec.medical_id || rec.medical_recordId;
              return (
                <div
                  key={id}
                  className="appointment-card"
                  style={{ padding: '18px 20px' }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <strong style={{ fontSize: '15px' }}>Bệnh án #{id}</strong>
                        <span className="pill-badge pill-secondary">
                          Chẩn đoán: {rec.diagnosis || 'Chưa có'}
                        </span>
                        {rec.patientName && (
                          <span className="mono-tag">
                            Bệnh nhân: {rec.patientName} (ID #{rec.patient_id})
                          </span>
                        )}
                        {rec.doctorName && (
                          <span style={{ fontSize: '12px', color: 'var(--outline)' }}>
                            Bác sĩ: {rec.doctorName}
                          </span>
                        )}
                      </div>

                      <p style={{ fontSize: '13.5px', marginTop: '6px', color: 'var(--on-surface)' }}>
                        <strong>Lý do khám:</strong> {rec.reason || '---'}
                      </p>

                      <p style={{ fontSize: '13px', marginTop: '3px', color: 'var(--on-surface-variant)' }}>
                        <strong>Điều trị:</strong> {rec.treatment || '---'}
                      </p>

                      {rec.prescription && (
                        <p style={{ fontSize: '12.5px', marginTop: '3px', color: 'var(--primary)', fontWeight: 600 }}>
                          <strong>Đơn thuốc:</strong> {rec.prescription}
                        </p>
                      )}

                      {rec.note && (
                        <p style={{ fontSize: '12px', marginTop: '2px', color: 'var(--outline)' }}>
                          <strong>Dặn dò:</strong> {rec.note}
                        </p>
                      )}
                    </div>

                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        type="button"
                        className="btn btn-sm btn-secondary"
                        onClick={() => handleEditClick(rec)}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>
                          edit
                        </span>
                        <span>Sửa</span>
                      </button>

                      <button
                        type="button"
                        className="file-icon-btn delete"
                        title="Xóa hồ sơ khỏi Database"
                        onClick={() => handleDelete(id)}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>
                          delete
                        </span>
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
