package com.example.invitevip.customer.dto;

import lombok.Getter;
import lombok.Setter;

// DTO 클래스 : 프론트엔드와 컨트롤러 사이에서 데이터를 주고 받는다.
// 엔티티를 외부에 노출하지 않고 필요한 데이터만 담아서 요청받거나 응답해주는 역할을 한다.

// CustomerSearchResponse는 고객 검색 결과를 프론트엔드로 응답할 떄 사용하는 DTO

// Getter. Setter 메서드를 자동으로 만들어주는 어노테이션이다.
@Getter
@Setter
public class CustomerSearchResponse {

    private Long id;
    private String name;
    private String grade;
    private String phone;
    private String code;
    private String note;
}
