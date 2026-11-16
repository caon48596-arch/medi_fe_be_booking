import React, { useState, useEffect } from 'react';
import {
  FileText,
  PlusCircle,
  RefreshCw,
  Edit,
  Trash2,
  CheckCircle,
  AlertCircle,
  Search,
} from 'lucide-react';
import { medicalRecordsApi } from '../services/api';

export default function MedicalRecordsSection({ currentUser }) {
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
    setTimeout(() => setMessage(null), 6000);
  };

  useEffect(() => {
    loadRecords();
  }, []);

  const loadRecords = async (recordId = null) => {
    setLoading(true);
    try {
      const res = await medicalRecordsApi.read(recordId);
      setRecords(res.data || []);
    } catch (err) {
      showMsg('error', 'Không thể tải hồ sơ: ' + err.message);
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
          reason: formData.reason,
          diagnosis: formData.diagnosis,
          treatment: formData.treatment,
          prescription: formData.prescription,
          note: formData.note,
        });
        showMsg('success', 'Cập nhật hồ sơ bệnh án thành công!');
      } else {
        await medicalRecordsApi.create({
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
        showMsg('success', 'Tạo mới hồ sơ bệnh án thành công!');
      }

      // Reset form
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
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEditClick = (rec) => {
    setIsEditing(true);
    setFormData({
      medical_recordId: rec.medical_id || rec.medical_recordId,
      patient_id: rec.patient_id || '',
      username: rec.username || '',
      doctorId: rec.doctorId || '',
      doctorUsername: rec.doctorUsername || '',
      reason: rec.reason || '',
      diagnosis: rec.diagnosis || '',
      treatment: rec.treatment || '',
      prescription: rec.prescription || '',
      note: rec.note || '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (recordId) => {
    if (!confirm('Bạn có chắc muốn xóa hồ sơ bệnh án #' + recordId + '?')) return;
    setLoading(true);
    try {
      await medicalRecordsApi.delete(recordId);
      showMsg('success', 'Đã xóa hồ sơ bệnh án #' + recordId);
      loadRecords();
    } catch (err) {
      showMsg('error', err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Messages */}
      {message && (
        <div className={`alert ${message.type === 'success' ? 'alert-success' : 'alert-error'}`}>
          {message.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
          <span>{message.text}</span>
        </div>
      )}

      {/* Form Tạo / Cập nhật */}
      <div className="card">
        <h3 className="card-title">
          {isEditing ? <Edit size={20} /> : <PlusCircle size={20} />}
          {isEditing
            ? `Cập nhật hồ sơ bệnh án #${formData.medical_recordId}`
            : 'Tạo hồ sơ bệnh án mới (/medical-records/create)'}
        </h3>

        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group">
              <label>ID Bệnh nhân:</label>
              <input
                type="number"
                className="form-control"
                value={formData.patient_id}
                onChange={(e) => setFormData({ ...formData, patient_id: e.target.value })}
                placeholder="ID Bệnh nhân (ví dụ: 1)"
                disabled={isEditing}
              />
            </div>

            <div className="form-group">
              <label>Lý do khám / Triệu chứng:</label>
              <input
                type="text"
                className="form-control"
                value={formData.reason}
                onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                placeholder="Triệu chứng ban đầu"
                required
              />
            </div>

            <div className="form-group">
              <label>Chẩn đoán bệnh:</label>
              <input
                type="text"
                className="form-control"
                value={formData.diagnosis}
                onChange={(e) => setFormData({ ...formData, diagnosis: e.target.value })}
                placeholder="Chẩn đoán của bác sĩ"
                required
              />
            </div>

            <div className="form-group">
              <label>Phương pháp điều trị:</label>
              <input
                type="text"
                className="form-control"
                value={formData.treatment}
                onChange={(e) => setFormData({ ...formData, treatment: e.target.value })}
                placeholder="Phương pháp điều trị"
              />
            </div>
          </div>

          <div className="form-grid">
            <div className="form-group">
              <label>Đơn thuốc (Prescription):</label>
              <textarea
                className="form-control"
                rows="2"
                value={formData.prescription}
                onChange={(e) => setFormData({ ...formData, prescription: e.target.value })}
                placeholder="Tên thuốc, liều dùng..."
              ></textarea>
            </div>

            <div className="form-group">
              <label>Ghi chú thêm:</label>
              <textarea
                className="form-control"
                rows="2"
                value={formData.note}
                onChange={(e) => setFormData({ ...formData, note: e.target.value })}
                placeholder="Ghi chú dặn dò bệnh nhân..."
              ></textarea>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {isEditing ? <Edit size={16} /> : <PlusCircle size={16} />}
              <span>{isEditing ? 'Lưu thay đổi' : 'Lưu hồ sơ bệnh án'}</span>
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
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '8px' }}>
          <h3 className="card-title" style={{ margin: 0, border: 'none', padding: 0 }}>
            <FileText size={20} /> Danh sách hồ sơ bệnh án (/medical-records/read)
          </h3>
          <div style={{ display: 'flex', gap: '8px' }}>
            <input
              type="number"
              className="form-control"
              style={{ width: '150px' }}
              placeholder="Tra theo ID hồ sơ"
              value={searchId}
              onChange={(e) => setSearchId(e.target.value)}
            />
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => loadRecords(searchId ? Number(searchId) : null)}
              disabled={loading}
            >
              <Search size={14} /> Tìm
            </button>
            <button
              className="btn btn-outline btn-sm"
              onClick={() => {
                setSearchId('');
                loadRecords(null);
              }}
              disabled={loading}
            >
              <RefreshCw size={14} /> Tất cả
            </button>
          </div>
        </div>

        {records.length === 0 ? (
          <div className="empty-box">
            <FileText size={48} />
            <p>Chưa có hồ sơ bệnh án nào.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã HS</th>
                  <th>Lý do khám</th>
                  <th>Chẩn đoán</th>
                  <th>Điều trị</th>
                  <th>Đơn thuốc</th>
                  <th>Ghi chú</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {records.map((rec) => {
                  const id = rec.medical_id || rec.medical_recordId;
                  return (
                    <tr key={id || Math.random()}>
                      <td><strong>#{id}</strong></td>
                      <td>{rec.reason || '---'}</td>
                      <td><span className="badge badge-info">{rec.diagnosis || '---'}</span></td>
                      <td>{rec.treatment || '---'}</td>
                      <td>{rec.prescription || '---'}</td>
                      <td>{rec.note || '---'}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleEditClick(rec)}
                            title="Sửa hồ sơ"
                          >
                            <Edit size={14} /> Sửa
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDelete(id)}
                            title="Xóa hồ sơ"
                          >
                            <Trash2 size={14} /> Xóa
                          </button>
                        </div>
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
  );
}
