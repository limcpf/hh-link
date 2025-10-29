package com.hkhr.link.db;

// USERS 테이블(EMP_ID, EMP_NM) 적재를 위한 간단한 DTO
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
