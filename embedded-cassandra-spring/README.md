# Embedded Cassandra Spring
[![Maven Central](https://img.shields.io/maven-central/v/com.github.nosan/embedded-cassandra.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.nosan/embedded-cassandra-spring)
 

For running `Embedded Cassandra` within `Spring Context`, `@EmbeddedCassandra` annotation has to be used.

```java
@RunWith(SpringRunner.class)
@ContextConfiguration
@EmbeddedCassandra("init.cql")
public class CassandraTests {

	@Autowired
	private Cluster cluster;

	@Test
	public void test() {
	}

}
```

You can declare your own `ExecutableConfig`, `IRuntimeConfig` and `ClusterFactory` 
beans to take control of the Cassandra instance's


## Maven

```xml
<dependencies>
    <dependency>
        <groupId>com.github.nosan</groupId>
        <artifactId>embedded-cassandra-spring</artifactId>
        <version>${embedded-cassandra-spring.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>${snakeyaml.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.datastax.cassandra</groupId>
        <artifactId>cassandra-driver-core</artifactId>
        <version>${cassandra-driver-core.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-test</artifactId>
        <version>${spring.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>${spring.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```




