package com.example.invitevip.customer.dto;

import lombok.Getter;
import lombok.Setter;

// DTO 클래스 : 프론트엔드와 컨트롤러 사이에서 데이터를 주고 받는다.
// 엔티티를 외부에 노출하지 않고 필요한 데이터만 담아서 요청받거나 응답해주는 역할을 한다.

// CustomerResponse는 백엔드에서 프론트엔드로 고객 정보를 돌려줄 때 사용하는 응답 DTO

// Getter. Setter 메서드를 자동으로 만들어주는 어노테이션이다.
@Getter
@Setter
public class CustomerResponse {

    // CustomerRequest와 비교시 가장 큰 차이점은
    // DB에서 저장되면서 자동으로 생기는 고객 고유 번호가 추가되었다는 점이다.
    private Long id;
    private String name;
    private String grade;
    private String phone;
    private String code;
    private String note;
}
