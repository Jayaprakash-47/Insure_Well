package com.healthshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileRequest {

    @Pattern(regexp = "^$|^.{1,100}$", message = "Name must be between 1 and 100 characters")
    private String name;

    @Pattern(regexp = "^$|^[\\d\\s\\+\\-]{10,15}$", message = "Phone number is invalid")
    private String phone;

    private String address;

    private String city;

    private String state;

    @Pattern(regexp = "^$|^[A-Za-z0-9\\s\\-]{4,10}$", message = "Pincode is invalid")
    private String pincode;

    private String accountNumber;
    private String ifscCode;
    private String accountHolderName;
    private String bankName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
}