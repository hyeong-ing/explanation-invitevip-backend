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
    // 고객을 새로 저장하는 메서드이다.
    // Controller의 POST /api/customers 요청에서 호출된다.
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

        Customer customer = Customer.create(
                request.getName(),
                request.getGrade(),
                request.getPhone(),
                inviteCode,
                request.getNote()
        );

        Customer saved = customerRepository.save(customer);
        customerSearchRepository.save(customerSearchMapper.toSearchEntity(saved));

        return toResponse(saved);
    }


    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        InviteCode newInviteCode = InviteCode.of(request.getCode());

        customerRepository.findByInviteCode(newInviteCode).ifPresent(found -> {
            if (!found.getId().equals(id)) {
                throw new DuplicateCodeException("초대코드가 중복되었습니다.");
            }
        });

        customer.update(
                request.getName(),
                request.getGrade(),
                request.getPhone(),
                newInviteCode,
                request.getNote()
        );

        customerSearchRepository.save(customerSearchMapper.toSearchEntity(customer));

        return toResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customerRepository.delete(customer);
        customerSearchRepository.deleteById(id);
    }

    @Transactional
    public void syncAllToElasticsearch() {
        List<Customer> list = customerRepository.findAll();

        for (Customer c : list) {
            customerSearchRepository.save(customerSearchMapper.toSearchEntity(c));
        }
    }

    public List<CustomerSearchResponse> searchCustomers(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        return customerSearchRepository.searchAll(normalizedKeyword).stream()
                .map(customerSearchMapper::toResponse)
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    public CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setGrade(customer.getGrade());
        response.setPhone(customer.getPhone());
        response.setCode(customer.getCode());
        response.setNote(customer.getNote());
        return response;
    }

}
