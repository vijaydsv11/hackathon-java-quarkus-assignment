# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
Both approaches are valid, but they optimize for different goals.

OpenAPI / Design-First Approach

In this approach, the API contract is defined first using an OpenAPI YAML specification, and the implementation is developed based on that specification.

Pros

Frontend and mobile teams can start development in parallel using the API specification before backend implementation is completed.

Provides clear API details, request/response models, and DTO structures early in development.

The YAML file can generate controller interfaces, DTO classes with validation rules, and Swagger documentation automatically.

Cons

Initial setup takes more time because the API specification must be designed first.

The YAML file must be updated and maintained whenever requirements change.

Code-First Approach

In this approach, developers implement APIs directly in code without creating a separate API specification. This is often used for internal services where development speed is important.

Pros

Faster development since APIs are implemented directly in code.

Frameworks like Spring Boot automatically generate Swagger documentation from controllers and DTO classes.

Cons

Documentation depends on code annotations and may become inconsistent if not maintained properly.

Swagger documentation is generated only after the API implementation is completed.

My choice here: hybrid.
- Public/external or business-critical APIs that support the main functionality of the system (like Warehouse) should follow the OpenAPI-first approach, since it provides a clear API contract, better documentation, and allows teams to work in parallel.

-Internal or simple endpoints can start with a code-first approach for faster development, and migrate to OpenAPI-first once stable for better documentation and standardization.


```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would first prioritize unit tests for the warehouse use cases because they contain the main business logic. I would test operations like create, replace, and archive to ensure validations such as location validation, capacity limits, and stock limits work correctly. I would also use parameterized tests to verify multiple input scenarios, such as different invalid capacity or stock values, using the same test logic.

After that, I would focus on integration tests to verify that the repository layer and database operations work correctly within transactions.

Finally, I would include concurrency tests to simulate multiple updates on the same warehouse and ensure that optimistic locking prevents data inconsistencies.

Test coverage helps ensure that most scenarios in the application are tested. Tools like JaCoCo can measure coverage, and maintaining around 80% coverage helps reduce the chances of bugs reaching production.

The tests should run automatically when code is pushed to Git through the CI pipeline, and the coverage report can be generated and shared with the team. This helps ensure that new changes do not reduce test coverage and keeps the code quality consistent.

```
