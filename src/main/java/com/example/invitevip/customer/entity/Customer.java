package com.example.invitevip.customer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Customer는 실제로 DB의 customer 테이블과 연결되는 클래스

// 이 클래스가 JPA Entity임
// Entity는 DB 테이블과 연결되는 Java 클래스이다.
@Entity
// 이 Entity가 매핑될 테이블 정보를 설정한다.
// name = "customer"는 DB의 customer 테이블과 연결한다는 뜻이다.
// uniqueConstraints는 특정 컬럼에 중복 방지 조건을 거는 설정이다.
// 여기서는 code 컬럼에 uk_customer_code라는 이름의 unique 제약 조건을 건다. => 초대코드 중복 불가
@Table(name = "customer", uniqueConstraints = @UniqueConstraint(name = "uk_customer_code", columnNames = "code"))
// getter 메서드를 자동으로 만들어준다.
@Getter
// 기본 생성자를 자동으로 만들어준다.
// JPA는 Entity를 만들 때 기본 생성자가 필요하다.
// 기본 생성자를 protected로 만든다.
// 외부에서 new Customer()로 마음대로 생성하지 못하게 막고 JPA 내부에서는 사용할 수 있게 허용하는 방식이다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    // 이 필드가 Entity의 기본키, Primary Key임을 나타낸다.
    // DB에서 각 고객을 구분하는 고유 번호이다.
    @Id
    // id 값을 DB가 자동으로 생성하도록 설정한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column은 객체 필드를 테이블의 컬럼에 매핑시켜주는 어노테이션입니다.
    // nullable = false는 DB에 null 값을 허용하지 않는다는 뜻이다.
    // length는 DB 컬럼 길이를 최대로 얼만큼 제한하는지 설정한다.
    @Column(nullable = false, length = 5)
    private String name;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(nullable = false, length = 20)
    private String phone;

    // InviteCode는 별도의 Entity가 아니라 값 타입이다.
    // @Embedded는 InviteCode 안의 값을 Customer 테이블 안에 포함시켜 저장하겠다는 뜻이다.
    // entity 폴더 안에 InviteCode 클래스가 있음.
    @Embedded
    // InviteCode 내부 필드의 컬럼 설정을 Customer Entity에서 재정의한다.
    // value 필드를 DB에서는 code라는 컬럼명으로 저장하겠다는 의미이다.
    // nullable = false는 code 값이 반드시 있어야 하며 length = 4는 4자리로 제한한다는 뜻이다.
    @AttributeOverride(name = "value", column = @Column(name = "code", nullable = false, length = 4))
    // 고객 초대코드를 저장하는 필드이다.
    // String code로 바로 두지 않고 InviteCode 값 객체로 감싸서 사용한다.
    // 이렇게 하면 초대코드 관련 규칙을 InviteCode 클래스 안에 모을 수 있다.
    private InviteCode inviteCode;

    @Column(length = 255)
    private String note;

    // Customer 객체를 생성하기 위한 정적 팩토리 메서드이다.
    public static Customer create(String name, String grade, String phone, InviteCode inviteCode, String note) {

        // Customer 객체를 생성한다.
        Customer customer = new Customer();
        // update 메서드를 호출해서 고객이름, 등급.. 등을 채운대.
        // // 생성할 때와 수정할 때 같은 로직을 재사용하는 구조이다.
        customer.update(name, grade, phone, inviteCode, note);
        // 값이 채워진 Customer 객체를 반환한다.
        return customer;
    }

    // 기존 Customer 객체의 정보를 수정하는 메서드이다.
    // 이렇게 하면 고객 정보 수정 로직을 한 곳에서 관리할 수 있다.
    public void update(String name, String grade, String phone, InviteCode inviteCode, String note) {

        // 고객 정보를 새 값으로 변경한다.
        this.name = name;
        this.grade = grade;
        this.phone = phone;
        this.inviteCode = inviteCode;
        this.note = note;
    }

    // 초대코드 값을 문자열로 꺼내는 메서드이다.
    // Customer 내부에서는 InviteCode 객체로 보관하지만,
    // 응답 DTO나 화면에서는 "1234" 같은 문자열 코드가 필요할 수 있다.
    public String getCode() {

        // inviteCode 객체 안에 들어 있는 실제 문자열 값을 꺼내서 반환한다.
        // 예: InviteCode(value = "1234")라면 "1234"를 반환한다.
        return inviteCode.getValue();
    }
}
