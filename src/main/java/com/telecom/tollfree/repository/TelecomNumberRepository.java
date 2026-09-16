package com.telecom.tollfree.repository;

import com.telecom.tollfree.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface TelecomNumberRepository extends JpaRepository<TelecomNumber, String> {
	Page<TelecomNumber> findByStatus(NumberStatus status, Pageable page);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select n from TelecomNumber n where n.number=:number")
	Optional<TelecomNumber> lockByNumber(String number);
}
