package com.example.Billing.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ShopRepository extends JpaRepository<Shop_entity, Long> {

    Optional<Shop_entity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Shop_entity> findByOwnerId(Long ownerId);
    List<Shop_entity> findAllByActiveTrue();

    Optional<Shop_entity> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    @Query("select min(s.id) from Shop_entity s where s.owner.id = :ownerId")
    Optional<Long> findFirstIdByOwnerId(@Param("ownerId") Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop_entity s where s.id = :id")
    Optional<Shop_entity> findByIdForUpdate(@Param("id") Long id);

}
