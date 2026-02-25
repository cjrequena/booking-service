package com.cjrequena.sample.command.handler.persistence.mongodb.repository;

import com.cjrequena.sample.command.handler.persistence.mongodb.entity.BookingEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface BookingProjectionRepository extends MongoRepository<BookingEntity, UUID> {

}
