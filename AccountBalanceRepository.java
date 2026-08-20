package com.financial.engine.repository;

import com.financial.engine.entity.AccountBalance;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT b FROM AccountBalance b WHERE b.accountId = :accountId")
    Optional<AccountBalance> findByAccountIdWithPessimisticLock(@Param("accountId") String accountId);

    Optional<AccountBalance> findByUserId(String userId);
}
