package com.vivance.hotel.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "api_call_event_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiCallEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "api_access_log_id")
    private Long apiAccessLogId;

    @Column(name = "service_channel")
    private String serviceChannel;

    @Column(name = "event_name")
    private String eventName;

    @Column(name = "event_type")
    private String eventType;

    @Lob
    @Column(name = "headers")
    private String headers;

    @Lob
    @Column(name = "parameters")
    private String parameters;

    @Column(name = "result_token")
    private String resultToken;

    @Column(name = "app_payment_refid")
    private String appPaymentRefId;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "created_datetime")
    private Date createdDatetime;
}
