package beans.domain;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Arrays;
import java.util.Set;

@NodeEntity
public class Bean {

    private String shortName;

    public enum Scope {singleton, prototype}

    @GraphId
    private Long id;

    @Indexed(unique = true)
    private String name;

    @Indexed
    private String type;
    private Scope scope;

    @RelatedTo(type = "DEPENDS_ON")
    private Set<Bean> dependencies;

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Scope getScope() {
        return scope;
    }

    public Set<Bean> getDependencies() {
        return dependencies;
    }

    public Long getId() {
        return id;
    }

    public Bean(String name, String type, Scope scope) {
        this.name = name;
        this.shortName = name.contains(".") ? name.substring(name.lastIndexOf('.')+1) : name;
        this.type = type;
        this.scope = scope;
    }

    public void setDependencies(Set<Bean> dependencies) {
        this.dependencies = dependencies;
    }

    public Bean() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" dependencies: ");
        for (Bean dependency : dependencies) {
            sb.append(dependency.getName());
            sb.append(", ");
        }
        return sb.toString();
    }
}
