package com.cjrequena.sample.query.handler.persistence.mongodb.repository;

import com.cjrequena.sample.query.handler.persistence.mongodb.entity.DeadLetterProjectionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface DeadLetterProjectionRepository extends MongoRepository<DeadLetterProjectionEntity, UUID> {
}
