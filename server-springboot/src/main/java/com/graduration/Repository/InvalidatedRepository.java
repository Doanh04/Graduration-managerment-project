package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.InvalidatedToken;

public interface InvalidatedRepository extends JpaRepository<InvalidatedToken, String> {}
