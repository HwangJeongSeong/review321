package com.team.cafe.test;

import org.springframework.data.jpa.repository.JpaRepository;

public interface  TestRepository extends JpaRepository<TestData, Integer> {
}
