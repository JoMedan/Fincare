package Fincare.FincareAppProject.Repository;

import Fincare.FincareAppProject.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.user.username = :username AND t.date = :date")
    List<Transaction> findByUserAndDate(@Param("username") String username, @Param("date") LocalDate date);

    @Query("SELECT t FROM Transaction t WHERE t.user.username = :username AND t.date BETWEEN :startDate AND :endDate")
    List<Transaction> findByUserAndDateRange(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.user.username = :username")
    List<Transaction> findByUser(@Param("username") String username);

}
