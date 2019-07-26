package com.example.ifasanatmodebusproject;

public class MeterRegister {

    private int register;
    private int len;

    public MeterRegister(int register, int len){
        this.len = len;
        this.register = register;
    }

    public MeterRegister(){

    }

    public int getRegister() {
        return register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }
}
