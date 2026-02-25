package com.cjrequena.sample.query.handler.persistence.mongodb.repository;

import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import java.util.UUID;

public interface BookingProjectionRepository extends ReactiveMongoRepository<BookingEntity, UUID> {

}
