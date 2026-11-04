package com.chocomall.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oms_shop_location")
public class ShopLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "detail_address", length = 300)
    private String detailAddress;

    @Column(name = "pickup_enabled", nullable = false)
    private boolean pickupEnabled = true;

    @Column(name = "return_enabled", nullable = false)
    private boolean returnEnabled = true;

    @Column(name = "opening_hours", length = 200)
    private String openingHours;
}
