package com.cjrequena.sample.command.handler;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.shared.common.util.ApplicationContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

/**
 * Base test class that sets up mocked dependencies for domain tests.
 * This allows domain tests to run without Spring context.
 */
public abstract class TestBase {

  protected static ObjectMapper objectMapper;
  protected static ApplicationContext mockApplicationContext;
  protected static EventMapper mockEventMapper;
  private static MockedStatic<ApplicationContextProvider> mockedProvider;

  @BeforeAll
  static void setUpBase() {
    // Only set up if not already initialized
    if (objectMapper == null) {
      // Create real ObjectMapper for JSON operations
      objectMapper = new ObjectMapper();
      objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

      // Mock ApplicationContext
      mockApplicationContext = Mockito.mock(ApplicationContext.class);
      
      // Mock EventMapper
      mockEventMapper = Mockito.mock(EventMapper.class);

      // Configure ApplicationContext to return our mocks
      Mockito.when(mockApplicationContext.getBean("objectMapper", ObjectMapper.class))
        .thenReturn(objectMapper);
      Mockito.when(mockApplicationContext.getBean(EventMapper.class))
        .thenReturn(mockEventMapper);

      // Mock the static ApplicationContextProvider
      mockedProvider = Mockito.mockStatic(ApplicationContextProvider.class);
      mockedProvider.when(ApplicationContextProvider::getContext)
        .thenReturn(mockApplicationContext);
    }
  }

  @AfterAll
  static void tearDownBase() {
    // Close the static mock after all tests
    if (mockedProvider != null) {
      mockedProvider.close();
      mockedProvider = null;
      objectMapper = null;
      mockApplicationContext = null;
      mockEventMapper = null;
    }
  }
}
