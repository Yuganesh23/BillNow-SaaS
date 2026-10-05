package com.example.Billing.supplier;
import lombok.*;
import java.math.BigDecimal;
@Data
public class SupplierRequest_Dto {
    private String businessName;
    private String contactPerson;
    private String phone;
    private String whatsapp;
    private String email;
    private String gstin;
    private String address;
    private String notes;
}
