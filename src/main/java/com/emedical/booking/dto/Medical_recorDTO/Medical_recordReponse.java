package com.emedical.booking.dto.Medical_recorDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medical_recordReponse {

    private Long medical_recordId;
    private String reason;
    private String diagnosis;
    private String treatment;
    private String prescription;
    private String note;
    private Long patient_id;
    private String username;
    private Long doctorId;
    private String doctorUsername;

}
