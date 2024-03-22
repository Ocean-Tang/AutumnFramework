package com.autumn.test;

import lombok.ToString;

@ToString
public class User {

    public int id;
    public String name;
    public Integer theAge;

    public void setAge(Integer age) {
        this.theAge = age;
    }
}