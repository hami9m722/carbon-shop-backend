package uit.carbon_shop.repos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uit.carbon_shop.domain.Company;


public interface CompanyRepository extends JpaRepository<Company, UUID> {
}