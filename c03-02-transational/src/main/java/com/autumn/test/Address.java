package com.autumn.test;

import lombok.ToString;

@ToString
public class Address {

    public int id;
    public int userId;
    public String address;
    public int zipcode;

    public void setZip(Integer zip) {
        this.zipcode = zip == null ? 0 : zip.intValue();
    }
}