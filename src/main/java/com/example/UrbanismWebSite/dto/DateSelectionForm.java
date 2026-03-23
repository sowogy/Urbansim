package com.example.UrbanismWebSite.dto;

import lombok.Data;

//이전 프로젝트 조회 시 선택한 날짜 데이터를 저장하는 폼
@Data
public class DateSelectionForm {
    private String selectedYear;
    private String selectedSemester;
}
