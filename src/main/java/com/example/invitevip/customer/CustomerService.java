package com.example.invitevip.customer;

import com.example.invitevip.customer.entity.Customer;
import com.example.invitevip.customer.entity.InviteCode;
import com.example.invitevip.customer.database.CustomerRepository;
import com.example.invitevip.customer.database.CustomerSearchRepository;
import com.example.invitevip.customer.dto.CustomerRequest;
import com.example.invitevip.customer.dto.CustomerResponse;
import com.example.invitevip.customer.dto.CustomerSearchResponse;
import com.example.invitevip.customer.mapper.CustomerSearchMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
// 고객 관련 실제 비즈니스 로직을 처리하는 서비스 클래스
// 컨트롤러는 프론트 요청을 받기만 했으면 서비스 클래스에서 직접 그 요청을 처리한다.

// 이 클래스가 Service 계층의 클래스임을 나타낸다.
// Spring이 이 클래스를 Bean으로 등록해서 다른 클래스에서 주입받아 사용할 수 있게 해준다.
@Service
public class CustomerService {

    // 고객 데이터를 MySQL 같은 실제 DB에서 조회, 저장, 수정, 삭제하기 위한 Repository이다.
    private final CustomerRepository customerRepository;
    // 고객 검색 데이터를 Elasticsearch에 저장하거나 검색하기 위한 Repository이다.
    private final CustomerSearchRepository customerSearchRepository;
    // Customer Entity와 Elasticsearch용 검색 Entity, 검색 응답 DTO를 서로 변환해주는 Mapper이다.
    private final CustomerSearchMapper customerSearchMapper;

    // 생성자 주입 방식이다.
    // CustomerService가 동작하려면 필요하므로 생성자를 통해 주입받는다.
    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                           CustomerSearchRepository customerSearchRepository,
                           CustomerSearchMapper customerSearchMapper) {

        // 이 파일 안에 있는 메서드 등을 사용할 수 있게 된다.
        this.customerRepository = customerRepository;
        this.customerSearchRepository = customerSearchRepository;
        this.customerSearchMapper = customerSearchMapper;
    }


    // Controller의 GET /api/customers 요청에서 호출된다.
    // 전체 고객 목록을 조회하는 메서드이다.
    public List<CustomerResponse> findAllCustomers() {

        // customerRepository.findAll()로 DB에 저장된 모든 Customer Entity를 조회한다.
        // stream()을 사용해서 조회된 Customer 목록을 하나씩 처리한다.
        return customerRepository.findAll().stream()

                // 각 Customer Entity를 CustomerResponse DTO로 변환한다.
                // customer -> this.toResponse(customer)와 같다.
                .map(this::toResponse)

                // 변환된 CustomerResponse들을 다시 List로 모아서 반환한다.
                .toList();
    }


    // 고객 등록 작업은 DB 데이터를 변경하는 작업이므로 트랜잭션이 필요하다.
    // 중간에 오류가 발생하면 저장 작업 전체를 롤백할 수 있게 해준다.
    @Transactional
    // Controller의 POST /api/customers 요청에서 호출된다.
    // 고객을 새로 저장하는 메서드이다.
    // CustomerRequest에는 프론트에서 보낸 name, grade 등 값이 들어 있다.
    public CustomerResponse save(CustomerRequest request) {

        // request.getCode()로 프론트에서 보낸 초대코드를 꺼낸다.
        // InviteCode.of()를 통해 단순 문자열을 InviteCode 값 객체로 변환한다. ( 예: "1234" → InviteCode 객체 )
        InviteCode inviteCode = InviteCode.of(request.getCode());

        // 같은 초대코드가 이미 DB에 존재하는지 확인한다.
        // true이면 중복 코드가 있다는 뜻이다.
        if (customerRepository.existsByInviteCode(inviteCode)) {

            // 초대코드가 중복되면 DuplicateCodeException 예외를 발생시킨다.
            // 즉, 같은 초대코드로 고객을 새로 등록하지 못하게 막는다.
            throw new DuplicateCodeException("초대코드가 중복되었습니다.");
        }

        // Customer.create() 정적 메서드를 사용해서 새로운 Customer Entity를 생성한다.
        // 고객 추가는 DB에 없던 고객을 새로 등록하는 기능이기 때문에 새로 생성하는 것이다.
        // request에서 이름, 등급, 전화번호, 초대코드, 메모를 꺼내 Customer 객체에 담는다.
        Customer customer = Customer.create(
                request.getName(),
                request.getGrade(),
                request.getPhone(),
                inviteCode,
                request.getNote()
        );

        // 생성한 Customer Entity를 DB에 저장한다.
        // 이때 id가 자동 생성되어 saved 객체에 들어간다.
        Customer saved = customerRepository.save(customer);

        // 저장된 Customer 객체를 Elasticsearch 검색용 Entity로 변환한다.
        // 변환한 검색용 Entity를 customerSearchRepository에 저장한다.
        // 즉, MySQL DB에 저장한 고객을 Elasticsearch에도 함께 저장하는 작업이다.
        customerSearchRepository.save(customerSearchMapper.toSearchEntity(saved));

        // 저장된 Customer Entity를 CustomerResponse DTO로 변환해서 반환한다.
        // Controller는 이 응답을 프론트엔드에 JSON으로 보내준다.
        return toResponse(saved);
    }



    @Transactional
    // Controller의 PUT /api/customers/{id} 요청에서 호출된다.
    // 기존 고객 정보를 수정하는 메서드이다.
    // id는 수정할 고객의 고유 번호이고, request는 수정할 새 데이터이다.
    public CustomerResponse update(Long id, CustomerRequest request) {

        // customerRepository.findById(id)로 DB에서 해당 id의 고객을 찾는다.
        // Optional로 반환되기 때문에, 없으면 orElseThrow()가 실행된다.
        // Optional은 값이 있을 수도 있고, 없을 수도 있을 때 사용한다. -> id가 없을 걸 대비해서 optional로 작성했
        Customer customer = customerRepository.findById(id)
                // 해당 id의 고객이 없으면 CustomerNotFoundException 예외를 발생시킨다.
                .orElseThrow(() -> new CustomerNotFoundException(id));

        // 수정 요청으로 들어온 새로운 초대코드를 InviteCode 값 객체로 변환한다. ( 예: "1234" → InviteCode 객체 )
        InviteCode newInviteCode = InviteCode.of(request.getCode());

        // 새 초대코드가 이미 DB에 존재하는지 조회한다.
        // findByInviteCode(newInviteCode)는 같은 초대코드를 가진 고객을 Optional로 반환한다.
        customerRepository.findByInviteCode(newInviteCode).ifPresent(found -> {
            // 같은 초대코드를 가진 고객이 발견되었는데,
            // 그 고객의 id가 현재 수정 중인 고객 id와 다르면 중복으로 판단한다.
            if (!found.getId().equals(id)) {
                throw new DuplicateCodeException("초대코드가 중복되었습니다.");
            }
        });

        // 기존 Customer Entity의 값을 새로운 값으로 변경한다.
        // 이때 customer는 이미 JPA가 관리 중인 영속 상태의 Entity이다.
        // 트랜잭션 종료 시 DB에 반영된다.
        customer.update(
                request.getName(),
                request.getGrade(),
                request.getPhone(),
                newInviteCode,
                request.getNote()
        );

        // 수정된 Customer 정보를 Elasticsearch에도 다시 저장한다.
        customerSearchRepository.save(customerSearchMapper.toSearchEntity(customer));

        // 수정된 Customer Entity를 CustomerResponse DTO로 변환해서 반환한다.
        return toResponse(customer);
    }


    @Transactional
    // Controller의 DELETE /api/customers/{id} 요청에서 호출된다.
    // 특정 id의 고객을 삭제하는 메서드이다.
    public void delete(Long id) {

        // 삭제할 고객이 실제로 존재하는지 먼저 DB에서 조회한다.
        // 존재하지 않는 고객을 삭제하려 하면 예외를 발생시킨다.
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        // DB에서 해당 고객을 삭제한다.
        customerRepository.delete(customer);
        // Elasticsearch에서도 같은 id의 검색 데이터를 삭제한다.
        customerSearchRepository.deleteById(id);
    }


    @Transactional
    // Controller의 POST /api/customers/sync 요청에서 호출된다.
    // MySQL DB에 있는 모든 고객 데이터를 Elasticsearch에 저장하는 메서드이다.
    public void syncAllToElasticsearch() {

        // DB에 저장된 모든 Customer Entity를 조회한다.
        List<Customer> list = customerRepository.findAll();

        // 조회된 고객 목록을 하나씩 반복한다.
        for (Customer c : list) {

            // 각 Customer Entity를 Elasticsearch 검색용 Entity로 변환한다.
            // 변환한 데이터를 Elasticsearch에 저장한다.
            customerSearchRepository.save(customerSearchMapper.toSearchEntity(c));
        }
    }

    // Controller의 GET /api/customers/search?keyword=검색어 요청에서 호출된다.
    // 고객 검색 기능을 처리하는 메서드이다.
    public List<CustomerSearchResponse> searchCustomers(String keyword) {

        // 검색어를 정리한다. null이면 빈 문자열로 바꾸고, 앞뒤 공백을 제거한다.
        String normalizedKeyword = normalizeKeyword(keyword);

        // 정리된 검색어가 빈 문자열이면 검색하지 않고 빈 리스트를 반환한다.
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        // Elasticsearch에서 검색어로 고객 데이터를 검색한다.
        return customerSearchRepository.searchAll(normalizedKeyword).stream()
                // 검색 결과 Entity를 CustomerSearchResponse DTO로 변환한다.
                .map(customerSearchMapper::toResponse)
                // 변환된 검색 응답 DTO들을 List로 모아서 반환한다.
                .toList();
    }

    // 검색어를 정리하는 private 메서드이다.(내부에서만 사용)
    private String normalizeKeyword(String keyword) {

        // keyword가 null이면 빈 문자열 ""을 반환한다.
        // null이 아니면 trim()으로 앞뒤 공백을 제거한 문자열을 반환한다.
        return keyword == null ? "" : keyword.trim();
    }

    // Customer Entity를 CustomerResponse DTO로 변환하는 메서드이다.
    public CustomerResponse toResponse(Customer customer) {

        // 비어 있는 CustomerResponse 객체를 생성한다.
        CustomerResponse response = new CustomerResponse();

        // Customer Entity의 데이터를 CustomerResponse에 담는다.
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setGrade(customer.getGrade());
        response.setPhone(customer.getPhone());
        response.setCode(customer.getCode());
        response.setNote(customer.getNote());
        return response;
    }

}
