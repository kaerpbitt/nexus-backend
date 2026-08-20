package com.financial.engine.repository;

import com.financial.engine.entity.Order;
import com.financial.engine.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByAccountIdAndStatus(String accountId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.accountId = :accountId AND o.status IN ('OPEN', 'PENDING_RISK_CHECK', 'MARGIN_LOCKED')")
    long countActiveOrdersByAccountId(@Param("accountId") String accountId);
}
