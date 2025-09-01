// File location: src/main/java/annotations/Entity.java
package annotations;

import java.lang.annotation.*;

/**
 * Custom entity annotation to mark domain model classes
 * Provides metadata for entity identification and processing
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Entity {
    
    /**
     * The name of the entity table
     * @return table name, defaults to class name in lowercase
     */
    String table() default "";
    
    /**
     * The database schema name
     * @return schema name, defaults to empty
     */
    String schema() default "";
    
    /**
     * Primary key field name
     * @return primary key field name, defaults to "id"
     */
    String primaryKey() default "id";
    
    /**
     * Whether to generate audit fields automatically
     * @return true if audit fields should be generated
     */
    boolean auditable() default true;
    
    /**
     * Whether the entity supports soft delete
     * @return true if soft delete is enabled
     */
    boolean softDelete() default false;
    
    /**
     * Cache strategy for the entity
     * @return cache strategy name
     */
    String cacheStrategy() default "LRU";
    
    /**
     * Cache TTL in minutes
     * @return TTL in minutes, 0 means no expiration
     */
    int cacheTTL() default 60;
    
    /**
     * Entity description for documentation
     * @return entity description
     */
    String description() default "";
    
    /**
     * Version for schema migration tracking
     * @return version number
     */
    String version() default "1.0";
    
    /**
     * Whether the entity is read-only
     * @return true if read-only
     */
    boolean readOnly() default false;
}