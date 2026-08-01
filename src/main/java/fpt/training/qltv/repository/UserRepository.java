package fpt.training.qltv.repository;

import fpt.training.qltv.entity.Role;
import fpt.training.qltv.entity.User;
import fpt.training.qltv.repository.projection.UserSummaryProjection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    Page<UserSummaryProjection> findAllProjectedBy(Pageable pageable);
}
