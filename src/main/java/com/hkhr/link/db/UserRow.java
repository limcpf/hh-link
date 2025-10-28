package com.hkhr.link.db;

public class UserRow {
    private String empId;
    private String empNm;

    public UserRow() {}

    public UserRow(String empId, String empNm) {
        this.empId = empId;
        this.empNm = empNm;
    }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }
    public String getEmpNm() { return empNm; }
    public void setEmpNm(String empNm) { this.empNm = empNm; }
}

