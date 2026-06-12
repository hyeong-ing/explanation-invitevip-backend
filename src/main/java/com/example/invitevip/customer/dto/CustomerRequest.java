package com.example.invitevip.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// DTO 클래스 : 프론트엔드와 컨트롤러 사이에서 데이터를 주고 받는다.
// 엔티티를 외부에 노출하지 않고 필요한 데이터만 담아서 요청받거나 응답해주는 역할을 한다.

// CustomerRequest는 프론트엔드에서 백엔드로 고객 정보를 보낼 때 요청하는 DTO
/*
프론트엔드에서 아래와 같은 JSON 데이터가 도착한다.
예를 들어 고객 추가, 수정할 떄 이런 데이터를 보낸다.
{
  "name": "홍길동",
  "grade": "VIP",
  "phone": "01012345678",
  "code": "1234",
  "note": "중요 고객"
}
*/

// Getter. Setter 메서드를 자동으로 만들어주는 어노테이션이다.
@Getter
@Setter
public class CustomerRequest {

    // @NotBlank : null, "  ", "" 모두 허용하지 않는다.
    // @Size(max = n) : 최대 길이는 5까지 허용한다.

    @NotBlank
    @Size(max = 5)
    private String name;

    @NotBlank
    @Size(max = 10)
    private String grade;

    @NotBlank
    @Size(max = 20)
    private String phone;

    @NotBlank
    // regexp는 특정 문자열 규칙이며, //d[4]는 코드가 숫자 4까지만 가능하다는 의미이다.
    // 만약 이 규칙을 어기면 message가 날라간다.
    @Pattern(regexp = "\\d{4}", message = "초대코드는 숫자 4자리로 입력해야 합니다.")
    private String code;

    @Size(max = 255)
    private String note;
}
