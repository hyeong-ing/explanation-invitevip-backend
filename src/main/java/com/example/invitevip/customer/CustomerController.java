package com.example.invitevip.customer;

import com.example.invitevip.customer.dto.CustomerRequest;
import com.example.invitevip.customer.dto.CustomerResponse;
import com.example.invitevip.customer.dto.CustomerSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CustomerController는 고객 관련 API 요청을 받는 컨틀롤러 클래스
// 프론트엔드에서 고객목록조회+고객등록+수정+삭제+검색+Elasticsearch 동기화 요청을 보내면 여기서 먼저 받는다.
// 실제 비즈니스 로직은 CustomerService로 넘겨준다.

// 이 클래스가 RestAPI 컨트롤러임을 나타낸다.
// @Controller + @ResponseBody => JSON 형태로 HTTP 응답에 담긴다.
@RestController
// 기본 URL 경로를 설정한다.
// 아래 메서드들은 "/api/customers"로 시작하는 주소를 가진다.
@RequestMapping("/api/customers")
// final 필드를 사용하는 생성자를 자동으로 만들어준다.
// customerService를 생성자 주입 방식으로 주입받게 해준다.
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // HTTP GET 요청 처리_ 실제 요청 주소: GET /api/customers
    @GetMapping
    // 조회는 SUPER_AMDIN과 ADMIN 역할을 가진 사용자만 접근 가능하다.
    // hasAnyRole은 내부적으로 ROLE_ 접두사를 붙여서 검사한다. -> ROLE_SUPER_ADMIN, ROLE_ADMIN
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    // 전체 고객 목록을 조회하는 메서드이다.
    // 반환 타입은 CustomerResponse 객체들의 리스트이다.
    public List<CustomerResponse> effectCustomers() {
        // customerService의 findAllCustomers()를 호출해서 전체 고객 목록을 가져온다.
        // 가져온 결과를 그대로 프론트엔드에 JSON 형태로 반환한다.
        return customerService.findAllCustomers();
    }

    // HTTP POST 요청을 처리한다.
    // 고객을 새로 등록할 떄 사용한다.
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'CUSTOMER_ADD')")
    // 고객 등록 요청을 처리하는 메서드이다.
    // CustomerRequest에는 프론트에서 보낸 데이터(name, grade, phone...)가 담긴다.
    public CustomerResponse saveCustomer(@Valid @RequestBody CustomerRequest request) {

        // @Vaild는 CustomerRequest 안의 검증 어노테이션을 실행한다 => @Size, @Pattern 등
        // @RequestBody는 HTTP 요청 body의 JSON 데이터를 CustomerRequest 객체로 변환한다.

        // customerService의 save() 메서드에 요청 데이터를 넘겨 고객을 저장한다.
        // 저장된 고객 정보를 CustomerResponse 형태로 반환한다.
        return customerService.save(request);
    }

    // HTTP PUT 요청을 처리한다.
    // 실제 요청 주소는 PUT /api/customers/{id} 이다.
    // 특정 id를 가진 고객 정보를 수정할 때 사용한다.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'CUSTOMER_EDIT')")
    // 고객 수정 요청을 처리하는 메서드이다.
    // ResponseEntity<CustomerResponse>는 HTTP 상태 코드와 응답 데이터를 함께 반환할 수 있게 해준다.
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {

        // @PathVariable : URL 경로에 들어온 id 값을 메서드 파라미터로 가져온다.
        // 예: /api/customers/3으로 요청하면 id에는 3이 들어간다.

        // customerService의 update() 메서드에 고객 id와 수정할 데이터를 넘긴다.
        // 수정된 고객 정보가 CustomerResponse로 반환된다.
        CustomerResponse updated = customerService.update(id, request);
        // 수정된 고객 정보를 HTTP 200 OK 상태와 함께 반환한다.
        return ResponseEntity.ok(updated);
    }

    // HTTP DELETE 요청을 처리한다.
    // 실제 요청 주소는 DELETE /api/customers/{id} 이다.
    // 특정 id를 가진 고객을 삭제할 때 사용한다.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'CUSTOMER_DELETE')")
    // 고객 삭제 요청을 처리하는 메서드이다.
    // 삭제 후에는 별도의 응답 데이터가 없기 때문에 ResponseEntity<Void>를 사용한다.
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {

        // customerService의 delete() 메서드를 호출해서 해당 id의 고객을 삭제한다.
        customerService.delete(id);
        // 삭제가 성공했다는 의미로 HTTP 200 OK를 반환한다.
        // build()는 body 없이 응답을 만들 때 사용한다.
        return ResponseEntity.ok().build();
    }

    // HTTP GET 요청 중 /search 경로를 처리한다.
    // 실제 요청 주소는 GET /api/customers/search?keyword=검색어 이다.
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'CUSTOMER_SEARCH')")
    // 고객 검색 요청을 처리하는 메서드이다.
    // 검색 결과는 CustomerSearchResponse 리스트로 반환된다.
    public List<CustomerSearchResponse> searchCustomer(@RequestParam String keyword) {
        // URL 쿼리 파라미터에서 keyword 값을 가져온다.

        // customerService의 searchCustomers() 메서드에 검색어를 넘긴다.
        // 검색된 고객 목록을 프론트엔드에 반환한다.
        return customerService.searchCustomers(keyword);
    }

    // HTTP POST 요청 중 /sync 경로를 처리한다.
    // 실제 요청 주소는 POST /api/customers/sync 이다.
    // DB에 있는 고객 데이터를 Elasticsearch에 동기화할 때 사용한다.
    @PostMapping("/sync")
    // 이 API는 SUPER_ADMIN 역할을 가진 사용자만 접근할 수 있다.
    // hasRole은 내부적으로 ROLE_ 접두사를 붙여서 검사한다.
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    // 성공 메시지를 문자열로 반환하기 때문에 ResponseEntity<String>을 사용한다.
    public ResponseEntity<String> sync() {
        // customerService의 syncAllToElasticsearch()를 호출한다.
        // DB에 저장된 전체 고객 데이터를 Elasticsearch에 저장하거나 갱신하는 역할이다.
        // 즉, DB와 Elasticsearch 인덱스가 서로 안맞을 수 있는걸 대비해서 동기화를 시킨다.
        customerService.syncAllToElasticsearch();

        // 동기화가 성공하면 HTTP 200 OK 상태와 함께 "sync ok" 문자열을 반환한다.
        return ResponseEntity.ok("sync ok");
    }
}
