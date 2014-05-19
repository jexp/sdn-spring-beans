package beans.repositories;

import beans.domain.Bean;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

public interface BeanRepository extends GraphRepository<Bean> {

    Bean findByName(@Param("0") String name);
    Bean findByType(@Param("0") String type);
}
