package com.ezwallet.module.bill.entity;

import com.ezwallet.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bill_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillProvider extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "service_type", nullable = false, length = 30)
    private String serviceType;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
