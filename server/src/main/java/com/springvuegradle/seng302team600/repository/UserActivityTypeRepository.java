package com.springvuegradle.seng302team600.repository;

import com.springvuegradle.seng302team600.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;


@RepositoryRestResource
public interface UserActivityTypeRepository extends JpaRepository<Activity, Long> {


    @Query(value="SELECT user_id FROM user_activity_type WHERE activity_type_id in ?1 " +
            "GROUP BY user_id HAVING COUNT(DISTINCT activity_type_id) = ?2", nativeQuery=true)
    List<Long> findByAllActivityTypeIds(@Param("activityTypeIds") List<Long> activityTypeIds,
                                     @Param("numOfActivityTypes") int numOfActivityTypes);

    @Query(value="SELECT user_id FROM user_activity_type WHERE activity_type_id in ?1 " +
            "GROUP BY user_id", nativeQuery=true)
    List<Long> findBySomeActivityTypeIds(@Param("activityTypeIds") List<Long> activityTypeIds);
}
