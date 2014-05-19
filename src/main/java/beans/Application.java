
package beans;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.FileUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;
import java.io.IOException;

@Configuration
@EnableNeo4jRepositories("beans.repositories")
@EnableAutoConfiguration
@EnableTransactionManagement
public class Application extends Neo4jConfiguration implements CommandLineRunner {

    public static final String PATH = "target/hello.db";

    public Application() {
        setBasePackage("beans.domain");
    }

    @org.springframework.context.annotation.Bean(destroyMethod = "shutdown")
    public GraphDatabaseService graphDatabaseService() {
//        return new GraphDatabaseFactory().newEmbeddedDatabase(PATH);
        return new SpringRestGraphDatabase("http://localhost:7474/db/data");
    }

    @org.springframework.context.annotation.Bean
    public DatabasePopulator databasePopulator() {
        return new DatabasePopulator();
    }

    public void run(String... args) throws Exception {
        databasePopulator().clean();
        databasePopulator().initialize();
        databasePopulator().list();
    }

    public static void main(String[] args) throws IOException {
        FileUtils.deleteRecursively(new File(PATH));

        SpringApplication.run(Application.class, args);
    }
}
