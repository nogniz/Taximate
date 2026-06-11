package com.taximate.backend.repository;

import com.taximate.backend.model.TaxiRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // 💡 CRUD 기능을 무료로 상속받는 JPA 인터페이스
public interface TaxiRoomRepository extends JpaRepository<TaxiRoom, String> {
    // 별도의 SQL 작성 없이 findById, save, findAll 등 자동 지원됨
}



































