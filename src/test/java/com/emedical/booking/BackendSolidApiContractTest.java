package com.emedical.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.emedical.booking.repository.AppoinmentRepository;
import com.emedical.booking.repository.DoctorRepository;
import com.emedical.booking.repository.DoctorSlotRepository;
import com.emedical.booking.repository.Medical_recordsRepository;
import com.emedical.booking.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class BackendSolidApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppoinmentRepository appoinmentRepository;

    @Autowired
    private DoctorSlotRepository doctorSlotRepository;

    @Autowired
    private Medical_recordsRepository medicalRecordsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @BeforeEach
    void cleanDb() {
        medicalRecordsRepository.deleteAll();
        appoinmentRepository.deleteAll();
        doctorSlotRepository.deleteAll();
        userRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Test
    void fullFrontendApiContract_AuthSlotsBookingCancelReassignAndMedicalRecords() throws Exception {
        // 1. Register Patient (FE contract: /user/register with confirePassword)
        MvcResult userRegRes = mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "fe_patient",
                                  "fullName": "Bệnh Nhân FE",
                                  "phone": "0912345678",
                                  "email": "fe_patient@test.vn",
                                  "password": "secret123",
                                  "confirePassword": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("fe_patient"))
                .andReturn();
        long patientId = objectMapper.readTree(userRegRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Patient 2-step Login + Session Cookie
        MvcResult userLoginStep1 = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "fe_patient",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requireOtp").value(true))
                .andExpect(jsonPath("$.data.otp").isNumber())
                .andReturn();

        int userOtp = objectMapper.readTree(userLoginStep1.getResponse().getContentAsString())
                .path("data").path("otp").asInt();

        MockHttpSession userSession = new MockHttpSession();
        mockMvc.perform(post("/user/login")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "fe_patient",
                                  "otp": %d
                                }
                                """.formatted(userOtp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_USER"));

        // Verify /user/profile preserves field names (user_id, full_name, phone)
        mockMvc.perform(get("/user/profile").param("username", "fe_patient").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(patientId))
                .andExpect(jsonPath("$.data.full_name").value("Bệnh Nhân FE"));

        // 3. Register 2 Doctors in same specialty for slot & reassignment tests
        MvcResult doc1Res = mockMvc.perform(post("/doctor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "doc_primary",
                                  "email": "doc1@hospital.vn",
                                  "password": "docpass",
                                  "confirmPassword": "docpass",
                                  "specialty": "Khoa Tim Mạch",
                                  "room_number": "P.201",
                                  "price": 300000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.doctor_id").isNumber())
                .andExpect(jsonPath("$.data.room_number").value("P.201"))
                .andReturn();
        long doc1Id = objectMapper.readTree(doc1Res.getResponse().getContentAsString())
                .path("data").path("doctor_id").asLong();

        MvcResult doc2Res = mockMvc.perform(post("/doctor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "doc_backup",
                                  "email": "doc2@hospital.vn",
                                  "password": "docpass",
                                  "confirmPassword": "docpass",
                                  "specialty": "Khoa Tim Mạch",
                                  "room_number": "P.202",
                                  "price": 300000
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long doc2Id = objectMapper.readTree(doc2Res.getResponse().getContentAsString())
                .path("data").path("doctor_id").asLong();

        // Doctor 2FA login via /doctor/verify-login-otp using email
        MvcResult docLoginStep1 = mockMvc.perform(post("/doctor/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "doc_primary",
                                  "password": "docpass"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        int docOtp = objectMapper.readTree(docLoginStep1.getResponse().getContentAsString())
                .path("data").path("otp").asInt();

        MockHttpSession docSession = new MockHttpSession();
        mockMvc.perform(post("/doctor/verify-login-otp")
                        .session(docSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "doc1@hospital.vn",
                                  "otp": %d
                                }
                                """.formatted(docOtp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_DOCTOR"));

        // 4. Create Doctor Slots (/doctor-slots/create) for both doctors
        String today = LocalDate.now().toString();
        MvcResult slotsRes1 = mockMvc.perform(post("/doctor-slots/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doctorId": %d,
                                  "workdate": "%s",
                                  "startShift": "08:00:00",
                                  "endShift": "09:00:00",
                                  "durationMinutes": 30
                                }
                                """.formatted(doc1Id, today)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        JsonNode doc1SlotNode = objectMapper.readTree(slotsRes1.getResponse().getContentAsString())
                .path("data").get(0);
        long slot1Id = doc1SlotNode.hasNonNull("doctorSlot_Id")
                ? doc1SlotNode.get("doctorSlot_Id").asLong()
                : doc1SlotNode.get("DoctorSlot_Id").asLong();

        mockMvc.perform(post("/doctor-slots/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "doctorId": %d,
                                  "workdate": "%s",
                                  "startShift": "08:00:00",
                                  "endShift": "08:30:00",
                                  "durationMinutes": 30
                                }
                                """.formatted(doc2Id, today)))
                .andExpect(status().isOk());

        // 5. Patient books slot (/user/booking) & verify double-booking prevention
        MvcResult bookingRes = mockMvc.perform(post("/user/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "fe_patient",
                                  "doctorId": %d,
                                  "specialty": "Khoa Tim Mạch",
                                  "date": "%s",
                                  "startTime": "08:00",
                                  "slotId": %d,
                                  "reason": "Khám tim mạch định kỳ"
                                }
                                """.formatted(doc1Id, today, slot1Id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appoinments_id").isNumber())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andReturn();
        long appointmentId = objectMapper.readTree(bookingRes.getResponse().getContentAsString())
                .path("data").path("appoinments_id").asLong();

        // Double booking same slot must fail cleanly with 400 + ResponseDTO(false, ...)
        mockMvc.perform(post("/user/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "fe_patient",
                                  "doctorId": %d,
                                  "date": "%s",
                                  "slotId": %d,
                                  "reason": "Đặt trùng slot"
                                }
                                """.formatted(doc1Id, today, slot1Id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 6. Doctor rejects appointment -> reassigned to doc_backup
        mockMvc.perform(post("/doctor/{doctorId}/appointments/{appointmentId}/reject", doc1Id, appointmentId)
                        .session(docSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("REASSIGNED_PENDING"))
                .andExpect(jsonPath("$.data[0].doctorUsername").value("doc_backup"));

        // 7. Update & Cancel appointment (/appointment/update & /appointment/cancel/{id})
        mockMvc.perform(put("/appointment/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appoinmentId": %d,
                                  "reason": "Cập nhật triệu chứng mới"
                                }
                                """.formatted(appointmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reason").value("Cập nhật triệu chứng mới"));

        mockMvc.perform(post("/appointment/cancel/{id}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 8. Medical Records CRUD (/medical-records/*)
        MvcResult medCreateRes = mockMvc.perform(post("/medical-records/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patient_id": %d,
                                  "doctorId": %d,
                                  "doctorUsername": "doc_backup",
                                  "reason": "Đau ngực nhẹ",
                                  "diagnosis": "Theo dõi huyết áp",
                                  "treatment": "Nghỉ ngơi",
                                  "prescription": "Vitamin B",
                                  "note": "Tái khám sau 2 tuần"
                                }
                                """.formatted(patientId, doc2Id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medical_id").isNumber())
                .andExpect(jsonPath("$.data.patientName").value("Bệnh Nhân FE"))
                .andReturn();
        long medicalId = objectMapper.readTree(medCreateRes.getResponse().getContentAsString())
                .path("data").path("medical_id").asLong();

        mockMvc.perform(get("/medical-records/read").param("medicalRecordId", String.valueOf(medicalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(put("/medical-records/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medical_recordId": %d,
                                  "diagnosis": "Ổn định huyết áp"
                                }
                                """.formatted(medicalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diagnosis").value("Ổn định huyết áp"));

        mockMvc.perform(delete("/medical-records/delete/{id}", medicalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
