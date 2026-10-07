package com.santsa.mediaassistant.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "specialty")
    private String specialty;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "phone")
    private String phone;

    @Column(name = "office")
    private String office;
}
