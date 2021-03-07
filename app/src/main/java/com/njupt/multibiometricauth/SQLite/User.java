package com.njupt.multibiometricauth.SQLite;

public class User {
    private String name;            //用户名
    private String password;        //密码
    private String phone;

    public User(String phone, String name, String password) {
        this.name = name;
        this.password = password;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override

    public String toString() {
        return "User{" + "phone='" + phone + '\'' + "," + "name='" + name + '\'' + ", password='" + password + '\'' + '}';
    }
}
